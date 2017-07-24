// ============================================================================
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.transformation.pipeline.node;

import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Visitor;

/**
 * Node that enforce the metadata of a traversing row with a fixed metadata.
 */
public class MetadataEnforcerNode extends BasicNode {

    private final RowMetadata enforcedMetadata;

    public MetadataEnforcerNode(RowMetadata enforcedMetadata) {
        this.enforcedMetadata = enforcedMetadata;
    }

    public RowMetadata getEnforcedMetadata() {
        return enforcedMetadata;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitMetadataEnforcer(this);
    }

    @Override
    public Node copyShallow() {
        return new MetadataEnforcerNode(enforcedMetadata);
    }
}
