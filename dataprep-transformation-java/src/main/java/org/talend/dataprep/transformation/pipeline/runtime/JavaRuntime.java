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

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.talend.daikon.exception.TalendRuntimeException;
import org.talend.daikon.exception.error.CommonErrorCodes;
import org.talend.dataprep.api.dataset.DataSet;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.link.CloneLink;
import org.talend.dataprep.transformation.pipeline.node.ActionNode;
import org.talend.dataprep.transformation.pipeline.node.CacheMetadataNode;
import org.talend.dataprep.transformation.pipeline.node.CleanUpNode;
import org.talend.dataprep.transformation.pipeline.node.CollectorNode;
import org.talend.dataprep.transformation.pipeline.node.CompileNode;
import org.talend.dataprep.transformation.pipeline.node.ConsumerNode;
import org.talend.dataprep.transformation.pipeline.node.DomainAndTypeEnforcerNode;
import org.talend.dataprep.transformation.pipeline.node.FilterNode;
import org.talend.dataprep.transformation.pipeline.node.InvalidDetectionNode;
import org.talend.dataprep.transformation.pipeline.node.LimitNode;
import org.talend.dataprep.transformation.pipeline.node.LocalSourceNode;
import org.talend.dataprep.transformation.pipeline.node.MetadataEnforcerNode;
import org.talend.dataprep.transformation.pipeline.node.NToOneNode;
import org.talend.dataprep.transformation.pipeline.node.SourceNode;
import org.talend.dataprep.transformation.pipeline.node.StatisticsNode;
import org.talend.dataprep.transformation.pipeline.node.StepNode;
import org.talend.dataprep.transformation.pipeline.node.TypeDetectionNode;
import org.talend.dataprep.transformation.pipeline.node.WriterNode;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JavaRuntime extends ExecutorVisitor<RuntimeNode> {

    private final Map<NToOneNode, RuntimeNode> reducers = new HashMap<>();

    private Stream<DataSetRow> stream;

    private RuntimeNode lastNode;

    @Override
    public Node visitCloneLink(CloneLink cloneLink) {
        final Node node = super.visitCloneLink(cloneLink);
        final RuntimeNode nextNode = lastNode;

        final List<RuntimeNode> nodes = Stream
                .of(cloneLink.getNodes()) //
                .map(n -> {
                    n.accept(this);
                    return lastNode;
                }) //
                .collect(Collectors.toList());
        lastNode = Monitor.monitor(new CloneLinkRuntime(nodes, nextNode));
        return node;
    }

    @Override
    public Node visitNToOne(NToOneNode nToOneNode) {
        Node node = super.visitNToOne(nToOneNode);
        final RuntimeNode nextNode = lastNode;

        if (!reducers.containsKey(nToOneNode)) {
            final RuntimeNode nToOneNodeRuntime = new NToOneRuntime(nToOneNode, nextNode);
            reducers.put(nToOneNode, nToOneNodeRuntime);
        }
        lastNode = Monitor.monitor(reducers.get(nToOneNode));

        return node;
    }

    @Override
    public Node visitLimit(LimitNode limitNode) {
        Node node = super.visitLimit(limitNode);
        lastNode = Monitor.monitor(new LimitRuntime(limitNode, lastNode));
        return node;
    }

    @Override
    public Node visitAction(ActionNode actionNode) {
        final Node node = super.visitAction(actionNode);

        final Function<DataSetRow, Collection<DataSetRow>> function = toFunction(actionNode);
        lastNode = Monitor.monitor(new ActionRuntime(function, lastNode));
        return node;
    }

    @Override
    public Node visitCompile(CompileNode compileNode) {
        Node node = super.visitCompile(compileNode);

        final Function<DataSetRow, DataSetRow> consumer = toConsumer(compileNode);
        lastNode = Monitor.monitor(new CompileRuntime(consumer, lastNode));

        return node;
    }

    @Override
    public Node visitStep(StepNode stepNode) {
        Node node = super.visitStep(stepNode);

        lastNode = Monitor.monitor(new StepNodeRuntime(stepNode, stepMetadataRepository, lastNode));

        return node;
    }

    @Override
    public RuntimeNode getResult() {
        return lastNode;
    }

    @Override
    public ExecutorRunnable toRunnable() {
        return new JavaRunnable(lastNode, stream);
    }

    @Override
    public Node visitConsumerNode(ConsumerNode consumerNode) {
        Node node = super.visitConsumerNode(consumerNode);

        lastNode = Monitor.monitor(new ConsumerRuntime(consumerNode, lastNode));
        return node;
    }

    @Override
    public Node visitNode(Node node) {
        super.visitNode(node);
        return node;
    }

    @Override
    public Node visitSource(SourceNode sourceNode) {
        try {
            final URL url = new URL(sourceNode.getSourceUrl());
            final ObjectMapper mapper = new ObjectMapper();
            final JsonParser parser = mapper.getFactory().createParser(url.openStream());
            final DataSet dataSet = mapper.readerFor(DataSet.class).readValue(parser);

            stream = dataSet.getRecords();
            return super.visitSource(sourceNode);
        } catch (IOException e) {
            throw new TalendRuntimeException(CommonErrorCodes.UNEXPECTED_EXCEPTION, e);
        }
    }

    @Override
    public Node visitCleanUp(CleanUpNode cleanUpNode) {
        Node node = super.visitCleanUp(cleanUpNode);

        lastNode = Monitor.monitor(new CleanUpRuntime(context, lastNode));
        return node;
    }

    @Override
    public Node visitFilterNode(FilterNode filterNode) {
        Node node = super.visitFilterNode(filterNode);

        lastNode = Monitor.monitor(new FilterRuntime(filterNode, lastNode));
        return node;
    }

    @Override
    public Node visitLocalSource(LocalSourceNode localSourceNode) {
        stream = localSourceNode.getSource();
        super.visitLocalSource(localSourceNode);
        return localSourceNode;
    }

    @Override
    public Node visitCollector(CollectorNode collectorNode) {
        super.visitCollector(collectorNode);

        lastNode = Monitor.monitor(new CollectorRuntime(collectorNode, lastNode));
        return collectorNode;
    }

    @Override
    public Node visitInvalidDetection(InvalidDetectionNode invalidDetectionNode) {
        Node node = super.visitInvalidDetection(invalidDetectionNode);

        lastNode = Monitor.monitor(new InvalidDetectionRuntime(invalidDetectionNode, lastNode));
        return node;
    }

    @Override
    public Node visitWriterNode(WriterNode writerNode) {
        Node node = super.visitWriterNode(writerNode);

        lastNode = Monitor.monitor(new WriterRuntime(writerNode.getWriter(), outputStream, lastNode));

        return node;
    }

    @Override
    public Node visitCacheMetadata(CacheMetadataNode cacheMetadataNode) {
        Node node = super.visitCacheMetadata(cacheMetadataNode);

        lastNode = Monitor.monitor(new CacheMetadataRuntime(cacheMetadataNode, lastNode));

        return node;
    }

    @Override
    public Node visitMetadataEnforcer(MetadataEnforcerNode metadataEnforcerNode) {
        Node node = super.visitMetadataEnforcer(metadataEnforcerNode);

        lastNode = Monitor.monitor(new MetadataEnforcerRuntime(metadataEnforcerNode, lastNode));

        return node;
    }

    @Override
    public Node visitDomainAndTypeEnforcer(DomainAndTypeEnforcerNode domainAndTypeEnforcerNode) {
        Node node = super.visitDomainAndTypeEnforcer(domainAndTypeEnforcerNode);

        lastNode = Monitor.monitor(new DomainTypeEnforcerRuntime(lastNode));

        return node;
    }

    @Override
    public Node visitStatistics(StatisticsNode statisticsNode) {
        Node node = super.visitStatistics(statisticsNode);

        lastNode = Monitor.monitor(new StatisticsRuntime(statisticsNode, lastNode));

        return node;
    }

    @Override
    public Node visitTypeDetection(TypeDetectionNode typeDetectionNode) {
        Node node = super.visitTypeDetection(typeDetectionNode);

        lastNode = Monitor.monitor(new ReactiveTypeDetectionRuntime(typeDetectionNode, lastNode));

        return node;
    }



}
