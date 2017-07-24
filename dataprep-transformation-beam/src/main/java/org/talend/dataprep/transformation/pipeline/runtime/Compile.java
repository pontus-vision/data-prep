package org.talend.dataprep.transformation.pipeline.runtime;

import java.io.Serializable;

import org.apache.beam.sdk.transforms.DoFn;
import org.talend.dataprep.api.dataset.row.AvroUtils;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.node.CompileNode;

class Compile extends DoFn<AvroUtils.Record, AvroUtils.Record> implements Serializable {

    private final ExecutorVisitor<?> beamExecutor;

    private final CompileNode compile;

    Compile(ExecutorVisitor<?> beamExecutor, CompileNode compile) {
        this.beamExecutor = beamExecutor;
        this.compile = compile;
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        AvroUtils.Record row = c.element();
        final DataSetRow modified = beamExecutor.toConsumer(compile).apply(AvroUtils.toDataSetRow(row));
        c.output(AvroUtils.toRecord(modified));
    }
}
