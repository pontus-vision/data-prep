package org.talend.dataprep.transformation.pipeline.runtime;

import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Signal;
import org.talend.dataprep.transformation.pipeline.node.NToOneNode;

class NToOneRuntime implements RuntimeNode {

    private final NToOneNode nToOneNode;

    private final RuntimeNode nextNode;

    private int index;

    private final DataSetRow[] input;

    NToOneRuntime(NToOneNode nToOneNode, RuntimeNode nextNode) {
        this.nToOneNode = nToOneNode;
        this.nextNode = nextNode;
        index = 0;
        input = new DataSetRow[nToOneNode.getInputSize()];
    }

    @Override
    public void receive(DataSetRow row) {
        input[index++] = row;
        if (index == nToOneNode.getInputSize()) {
            try {
                final DataSetRow reducedRow = nToOneNode.getRowReducer().apply(input);
                nextNode.receive(reducedRow);
            } finally {
                index = 0;
            }
        }
    }

    @Override
    public void signal(Signal signal) {
        if (nextNode != null) {
            nextNode.signal(signal);
        }
    }
}
