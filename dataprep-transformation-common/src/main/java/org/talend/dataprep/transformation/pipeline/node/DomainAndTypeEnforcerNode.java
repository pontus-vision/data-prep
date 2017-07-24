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
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Visitor;

/**
 * Node that set the domain and type of a traversing row as forced.
 */
public class DomainAndTypeEnforcerNode extends ConsumerNode {

    public DomainAndTypeEnforcerNode() {
        super(DomainAndTypeEnforcerNode::forceDomainsAndType, null);
    }

    /**
     * Force all domains in the columns row
     */
    private static void forceDomainsAndType(final DataSetRow row) {
        forceDomainsAndType(row.getRowMetadata());
    }

    /**
     * Force all domains in the columns metadata
     */
    public static void forceDomainsAndType(final RowMetadata metadata) {
        metadata.getColumns().forEach(col -> {
            col.setTypeForced(true);
            col.setDomainForced(true);
        });
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitDomainAndTypeEnforcer(this);
    }

    @Override
    public Node copyShallow() {
        return new DomainAndTypeEnforcerNode();
    }
}
