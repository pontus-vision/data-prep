package org.talend.dataprep.transformation.pipeline.runtime;

import java.util.function.Function;

import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Signal;

class CompileRuntime implements RuntimeNode {

    private final Function<DataSetRow, DataSetRow> consumer;

    private final RuntimeNode nextNode;

    CompileRuntime(Function<DataSetRow, DataSetRow> consumer, RuntimeNode nextNode) {
        this.consumer = consumer;
        this.nextNode = nextNode;
    }

    @Override
    public void receive(DataSetRow row) {
        if (nextNode != null) {
            nextNode.receive(consumer.apply(row));
        }
    }

    @Override
    public void signal(Signal signal) {
        if (nextNode == null) {
            return;
        }
        nextNode.signal(signal);
    }
}
