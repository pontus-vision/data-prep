package org.talend.dataprep.transformation.pipeline.runtime;

import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Signal;
import org.talend.dataprep.transformation.pipeline.node.CacheMetadataNode;

class CacheMetadataRuntime implements RuntimeNode {

    private final RuntimeNode nextNode;

    private final CacheMetadataNode cacheMetadataNode;

    private RowMetadata metadata;

    CacheMetadataRuntime(CacheMetadataNode cacheMetadataNode, RuntimeNode nextNode) {
        this.nextNode = nextNode;
        this.cacheMetadataNode = cacheMetadataNode;
    }

    @Override
    public void receive(DataSetRow row) {
        metadata = row.getRowMetadata();
        if (nextNode != null) {
            nextNode.receive(row);
        }
    }

    @Override
    public void signal(Signal signal) {
        if (signal == Signal.END_OF_STREAM) {
            cacheMetadataNode.getCacheAction().accept(metadata);
        }
        if (nextNode != null) {
            nextNode.signal(signal);
        }
    }
}
