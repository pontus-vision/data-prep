package org.talend.dataprep.transformation.pipeline.runtime;

import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Signal;
import org.talend.dataprep.transformation.pipeline.node.ConsumerNode;

class ConsumerRuntime implements RuntimeNode {

    private final ConsumerNode consumerNode;

    private final RuntimeNode nextNode;

    ConsumerRuntime(ConsumerNode consumerNode, RuntimeNode nextNode) {
        this.consumerNode = consumerNode;
        this.nextNode = nextNode;
    }

    @Override
    public void receive(DataSetRow row) {
        consumerNode.getRowConsumer().accept(row);
        nextNode.receive(row);
    }

    @Override
    public void signal(Signal signal) {
        try {
            consumerNode.getSignalConsumer().accept(signal);
        } finally {
            nextNode.signal(signal);
        }
    }
}
