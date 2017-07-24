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

import java.util.function.Predicate;

import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Visitor;

public class InvalidDetectionNode extends ColumnFilteredNode {

    public InvalidDetectionNode(final Predicate<? super ColumnMetadata> filter) {
        super(filter);
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitInvalidDetection(this);
    }

    @Override
    public Node copyShallow() {
        return new InvalidDetectionNode(filter);
    }

}
