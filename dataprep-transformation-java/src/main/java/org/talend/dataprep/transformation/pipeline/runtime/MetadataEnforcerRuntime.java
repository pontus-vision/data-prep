package org.talend.dataprep.transformation.pipeline.runtime;

import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Signal;
import org.talend.dataprep.transformation.pipeline.node.MetadataEnforcerNode;

class MetadataEnforcerRuntime implements RuntimeNode {

    private final MetadataEnforcerNode metadataEnforcerNode;
    private final RuntimeNode nextNode;

    public MetadataEnforcerRuntime(MetadataEnforcerNode metadataEnforcerNode, RuntimeNode nextNode) {
        this.metadataEnforcerNode = metadataEnforcerNode;
        this.nextNode = nextNode;
    }

    @Override
    public void receive(DataSetRow row) {
        final DataSetRow modified = row.setRowMetadata(metadataEnforcerNode.getEnforcedMetadata());
        nextNode.receive(modified);
    }

    @Override
    public void signal(Signal signal) {
        if (nextNode == null) {
            return;
        }
        nextNode.signal(signal);
    }
}
