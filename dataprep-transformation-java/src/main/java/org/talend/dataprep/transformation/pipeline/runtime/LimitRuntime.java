package org.talend.dataprep.transformation.pipeline.runtime;

import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Signal;
import org.talend.dataprep.transformation.pipeline.node.LimitNode;

class LimitRuntime implements RuntimeNode {

    private final LimitNode limitNode;

    private final RuntimeNode nextNode;

    private long count;

    LimitRuntime(LimitNode limitNode, RuntimeNode nextNode) {
        this.limitNode = limitNode;
        this.nextNode = nextNode;
        count = 0;
    }

    @Override
    public void receive(DataSetRow row) {
        if (count < limitNode.getLimit()) {
            try {
                nextNode.receive(row);
            } finally {
                count++;
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
