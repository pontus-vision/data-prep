// ============================================================================
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.transformation.pipeline.runtime;

import static org.apache.beam.sdk.transforms.ParDo.of;
import static org.apache.beam.sdk.transforms.Sample.any;
import static org.talend.dataprep.api.dataset.row.AvroUtils.toDataSetRow;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.coders.KvCoder;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.beam.sdk.extensions.sorter.BufferedExternalSorter;
import org.apache.beam.sdk.extensions.sorter.SortValues;
import org.apache.beam.sdk.io.AvroIO;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.row.AvroUtils;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.actions.common.RunnableAction;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.builder.NodeBuilder;
import org.talend.dataprep.transformation.pipeline.link.BasicLink;
import org.talend.dataprep.transformation.pipeline.node.*;

public class BeamRuntime extends ExecutorVisitor<PCollection<KV<IndexedRecord, AvroUtils.Metadata>>>
        implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(BeamRuntime.class);

    private final transient Stack<Node> current = new Stack<>();

    private final Map<RunnableAction, CompileNode> compilations = new HashMap<>();

    private transient Pipeline root;

    private transient PCollection<KV<IndexedRecord, AvroUtils.Metadata>> pipeline;

    private transient NodeBuilder builder;

    private transient Schema schema;

    public BeamRuntime() {
    }

    public BeamRuntime(PCollection<IndexedRecord> pipeline, Schema schema) {
        this.schema = schema;
        this.pipeline = pipeline.apply(of(new DoFn<IndexedRecord, DataSetRow>() {

            @ProcessElement
            public void process(ProcessContext c) {
                c.output(AvroUtils.toDataSetRow(c.element(), new AvroUtils.Metadata(0L, Collections.emptyList())));
            }

        })).apply(of(new Converter())).setCoder(getCoder());
    }

    @Override
    public PCollection<KV<IndexedRecord, AvroUtils.Metadata>> getResult() {
        return pipeline;
    }

    @Override
    public ExecutorRunnable toRunnable() {
        return new BeamExecutorRunnable(root, getContext());
    }

    @Override
    public Node visitSource(SourceNode sourceNode) {
        PipelineOptions options = PipelineOptionsFactory.create();
        this.root = Pipeline.create(options);
        this.pipeline =
                root.apply(AvroIO.read(DataSetRow.class).from(sourceNode.getSourceUrl())).apply(of(new Converter()));

        builder = NodeBuilder.source(sourceNode.getSourceUrl());
        current.push(sourceNode.copyShallow());
        super.visitSource(sourceNode);
        return builder.build();
    }

    @Override
    public Node visitLocalSource(LocalSourceNode localSourceNode) {
        final List<DataSetRow> rows = localSourceNode.getSource().collect(Collectors.toList());

        final PipelineOptions options = PipelineOptionsFactory.create();
        this.root = Pipeline.create(options);
        if (rows.isEmpty()) {
            this.pipeline = root.apply(Create.empty(TypeDescriptor.of(DataSetRow.class))).apply(of(new Converter()));
        } else {
            schema = AvroUtils.toSchema(rows.get(0).getRowMetadata());
            this.pipeline = root
                    .apply(Create.of(rows).withType(TypeDescriptor.of(DataSetRow.class)))
                    .apply(of(new Converter()))
                    .setCoder(getCoder());
        }

        builder = NodeBuilder.source(localSourceNode.getSource());
        current.push(localSourceNode.copyShallow());
        super.visitLocalSource(localSourceNode);
        return builder.build();
    }

    @Override
    public Node visitStatistics(StatisticsNode statisticsNode) {
        pipeline = pipeline.apply(of(new Statistics(statisticsNode)));
        return process(statisticsNode, () -> super.visitStatistics(statisticsNode));
    }

    @Override
    public Node visitLimit(LimitNode limitNode) {
        pipeline = pipeline.apply(any(limitNode.getLimit()));
        return process(limitNode, () -> super.visitLimit(limitNode));
    }

    @Override
    public Node visitFilterNode(FilterNode filterNode) {
        pipeline = pipeline.apply(of(new Filter(this, filterNode)));
        return process(filterNode, () -> super.visitFilterNode(filterNode));
    }

    private Node process(Node node, Runnable runnable) {
        builder.to(node.copyShallow());
        runnable.run();
        return node;
    }

    @Override
    public Node visitNode(Node node) {
        pipeline = pipeline
                .apply(of(new DoFn<KV<IndexedRecord, AvroUtils.Metadata>, KV<IndexedRecord, AvroUtils.Metadata>>() {

                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        c.output(c.element());
                    }
                })) //
                .setCoder(getCoder());
        return process(node, () -> super.visitNode(node));
    }

    @Override
    public Node visitAction(ActionNode actionNode) {
        final CompileNode compilation = compilations.get(actionNode.getAction());
        final RowMetadata currentSchema = AvroUtils.toRowMetadata(schema);
        final DataSetRow compiledRow = toConsumer(compilation).apply(new DataSetRow(currentSchema));
        schema = AvroUtils.toSchema(compiledRow.getRowMetadata());

        final KvCoder<IndexedRecord, AvroUtils.Metadata> coder = getCoder();
        pipeline = pipeline.apply(of(new Apply(this, actionNode, compilation))).setCoder(coder); // Schema may change so
                                                                                                 // follow changes

        return process(actionNode, () -> super.visitAction(actionNode));
    }

    private KvCoder<IndexedRecord, AvroUtils.Metadata> getCoder() {
        return KvCoder.of( //
                AvroCoder.of(IndexedRecord.class, schema), //
                SerializableCoder.of(AvroUtils.Metadata.class) //
        );
    }

    @Override
    public Node visitCompile(CompileNode compileNode) {
        compilations.put(compileNode.getAction(), compileNode);
        return process(compileNode, () -> super.visitCompile(compileNode));
    }

    @Override
    public Node visitCollector(CollectorNode collectorNode) {
        try {
            final String tempFile = File.createTempFile("test", ".avro").getAbsolutePath();
            pipeline = pipeline.apply(of(new ToAvroCollector(tempFile)));
            AvroCollectorNode avroCollectorNode = new AvroCollectorNode(tempFile);
            current.push(avroCollectorNode);
            return collectorNode;
        } catch (IOException e) {
            LOGGER.error("Unable to open output.", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Node visitBasicLink(BasicLink basicLink) {
        Node node = super.visitBasicLink(basicLink);
        if (!current.isEmpty()) {
            builder.to(current.pop());
        }
        return node;
    }

    @Override
    public Node visitConsumerNode(ConsumerNode consumerNode) {
        pipeline = pipeline
                .apply(of(new DoFn<KV<IndexedRecord, AvroUtils.Metadata>, KV<IndexedRecord, AvroUtils.Metadata>>() {

                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        final KV<IndexedRecord, AvroUtils.Metadata> row = c.element();
                        consumerNode.getRowConsumer().accept(toDataSetRow(row.getKey(), row.getValue()));
                        c.output(row);
                    }

                }));
        return process(consumerNode, () -> super.visitConsumerNode(consumerNode));
    }

    @Override
    public Node visitSort(SortNode sortNode) {
        final BufferedExternalSorter.Options options = BufferedExternalSorter.options();
        final SortValues<Object, Object, IndexedRecord> sorter = SortValues.create(options);
        return process(sortNode, () -> super.visitSort(sortNode));
    }

    private static class Converter extends DoFn<DataSetRow, KV<IndexedRecord, AvroUtils.Metadata>> {

        @ProcessElement
        public void process(ProcessContext c) {
            final AvroUtils.Record record = AvroUtils.toRecord(c.element());
            c.output(KV.of(record.getIndexedRecord(), record.getMetadata()));
        }
    }

    @Override
    public Node visitStep(StepNode stepNode) {
        return process(stepNode, () -> super.visitStep(stepNode));
    }
}
