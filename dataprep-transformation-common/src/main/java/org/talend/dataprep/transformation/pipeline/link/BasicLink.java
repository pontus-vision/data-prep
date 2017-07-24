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

package org.talend.dataprep.transformation.pipeline.link;

import org.talend.dataprep.transformation.pipeline.Link;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Visitor;

public class BasicLink implements Link {

    private final Node target;

    public BasicLink(Node target) {
        this.target = target;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitBasicLink(this);
    }

    @Override
    public Node getTarget() {
        return target;
    }
}
