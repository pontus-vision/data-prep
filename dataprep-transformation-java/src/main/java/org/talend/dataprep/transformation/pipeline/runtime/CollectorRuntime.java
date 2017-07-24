package org.talend.dataprep.transformation.pipeline.runtime;

import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Signal;
import org.talend.dataprep.transformation.pipeline.node.CollectorNode;

class CollectorRuntime implements RuntimeNode {

    private final CollectorNode collectorNode;

    private final RuntimeNode nextNode;

    CollectorRuntime(CollectorNode collectorNode, RuntimeNode nextNode) {
        this.collectorNode = collectorNode;
        this.nextNode = nextNode;
    }

    @Override
    public void receive(DataSetRow row) {
        collectorNode.add(row);
    }

    @Override
    public void signal(Signal signal) {
        if (nextNode != null) {
            nextNode.signal(signal);
        }
    }
}
