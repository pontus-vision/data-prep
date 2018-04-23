package org.talend.dataprep.transformation.pipeline.runtime;

import java.util.Collection;
import java.util.function.Function;

import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Signal;

class ActionRuntime implements RuntimeNode {

    private final Function<DataSetRow, Collection<DataSetRow>> function;

    private final RuntimeNode nextNode;

    ActionRuntime(Function<DataSetRow, Collection<DataSetRow>> function, RuntimeNode nextNode) {
        this.function = function;
        this.nextNode = nextNode;
    }

    @Override
    public void receive(DataSetRow row) {
        if (nextNode != null) {
            final Collection<DataSetRow> modifiedRows = function.apply(row);
            for (DataSetRow dataSetRow : modifiedRows) {
                nextNode.receive(dataSetRow);
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
