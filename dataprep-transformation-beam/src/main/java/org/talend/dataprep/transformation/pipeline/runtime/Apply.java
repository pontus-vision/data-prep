package org.talend.dataprep.transformation.pipeline.runtime;

import static org.talend.dataprep.api.dataset.row.AvroUtils.toDataSetRow;

import java.io.Serializable;
import java.util.Collection;
import java.util.function.Function;

import org.apache.avro.generic.IndexedRecord;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.talend.dataprep.api.dataset.row.AvroUtils;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.node.ActionNode;
import org.talend.dataprep.transformation.pipeline.node.CompileNode;

class Apply extends DoFn<KV<IndexedRecord, AvroUtils.Metadata>, KV<IndexedRecord, AvroUtils.Metadata>> implements Serializable {

    private final ExecutorVisitor<?> beamExecutor;

    private final ActionNode action;

    private CompileNode compileNode;

    Apply(ExecutorVisitor<?> beamExecutor, ActionNode action, CompileNode compileNode) {
        this.beamExecutor = beamExecutor;
        this.action = action;
        this.compileNode = compileNode;
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        final KV<IndexedRecord, AvroUtils.Metadata> row = c.element();
        final Function<DataSetRow, DataSetRow> compile = beamExecutor.toConsumer(compileNode);
        final Function<DataSetRow, Collection<DataSetRow>> action = beamExecutor.toFunction(this.action);
        final Collection<DataSetRow> rows = action.apply(compile.apply(toDataSetRow(row.getKey(), row.getValue())));
        for (DataSetRow current : rows) {
            final AvroUtils.Record enhancedRecord = AvroUtils.toRecord(current);
            c.output(KV.of(enhancedRecord.getIndexedRecord(), enhancedRecord.getMetadata()));
        }
    }
}
