package org.talend.dataprep.transformation.pipeline.runtime;

import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Signal;

class BasicNodeRuntime implements RuntimeNode {

    private final RuntimeNode nextNode;

    BasicNodeRuntime(RuntimeNode nextNode) {
        this.nextNode = nextNode;
    }

    @Override
    public void receive(DataSetRow row) {
        if (nextNode == null) {
            return;
        }
        nextNode.receive(row);
    }

    @Override
    public void signal(Signal signal) {
        if (nextNode == null) {
            return;
        }
        nextNode.signal(signal);
    }
}
