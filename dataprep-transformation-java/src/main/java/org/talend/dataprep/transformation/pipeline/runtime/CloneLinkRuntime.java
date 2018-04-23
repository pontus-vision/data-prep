package org.talend.dataprep.transformation.pipeline.runtime;

import java.util.List;

import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Signal;

class CloneLinkRuntime implements RuntimeNode {

    private final List<RuntimeNode> nodes;

    private final RuntimeNode nextNode;

    CloneLinkRuntime(List<RuntimeNode> nodes, RuntimeNode nextNode) {
        this.nodes = nodes;
        this.nextNode = nextNode;
    }

    @Override
    public void receive(DataSetRow row) {
        for (RuntimeNode node : nodes) {
            node.receive(row.clone());
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
