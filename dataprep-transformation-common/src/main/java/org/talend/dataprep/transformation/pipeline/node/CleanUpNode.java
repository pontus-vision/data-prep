// ============================================================================
// Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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

import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Visitor;

/**
 * A node implementation that cleans up transformation context when end of stream is reached.
 */
public class CleanUpNode extends BasicNode {

    public CleanUpNode() {
    }

    @Override
    public Node copyShallow() {
        return new CleanUpNode();
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitCleanUp(this);
    }
}
