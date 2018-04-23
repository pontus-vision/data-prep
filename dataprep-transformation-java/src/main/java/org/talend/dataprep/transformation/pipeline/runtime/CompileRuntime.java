package org.talend.dataprep.transformation.pipeline.runtime;

import java.util.function.Function;

import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Signal;

class CompileRuntime implements RuntimeNode {

    private final Function<DataSetRow, DataSetRow> consumer;

    private final RuntimeNode nextNode;

    private RowMetadata rowMetadata;

    CompileRuntime(Function<DataSetRow, DataSetRow> consumer, RuntimeNode nextNode) {
        this.consumer = consumer;
        this.nextNode = nextNode;
    }

    @Override
    public void receive(DataSetRow row) {
        if (nextNode != null) {
            if (rowMetadata == null) {
                final DataSetRow compiledRow = consumer.apply(row);
                rowMetadata = compiledRow.getRowMetadata();
                nextNode.receive(compiledRow);
            } else {
                nextNode.receive(row.setRowMetadata(rowMetadata));
            }

        }
    }

    @Override
    public void signal(Signal signal) {
        if (nextNode == null) {
            return;
        }
        nextNode.signal(signal);
    }

    @Override
    public RuntimeNode getNext() {
        return nextNode;
    }
}
