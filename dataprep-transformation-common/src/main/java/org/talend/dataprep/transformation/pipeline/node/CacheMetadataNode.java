package org.talend.dataprep.transformation.pipeline.node;

import java.util.function.Consumer;

import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.transformation.pipeline.Visitor;

public class CacheMetadataNode extends BasicNode {

    private Consumer<RowMetadata> cacheAction;

    public CacheMetadataNode(Consumer<RowMetadata> cacheAction) {
        this.cacheAction = cacheAction;
    }

    public Consumer<RowMetadata> getCacheAction() {
        return cacheAction;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitCacheMetadata(this);
    }
}
