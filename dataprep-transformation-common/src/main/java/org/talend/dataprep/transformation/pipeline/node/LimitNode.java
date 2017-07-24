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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Visitor;

/**
 * Node that limit input. When the input limit received is reached, a callback can be triggered.
 */
public class LimitNode extends BasicNode {

    /** This class' logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LimitNode.class);

    private final long limit;

    public LimitNode(final long limit) {
        LOGGER.trace("Limit set to {}", limit);
        this.limit = limit;
    }

    @Override
    public Node copyShallow() {
        return new LimitNode(limit);
    }

    public long getLimit() {
        return limit;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitLimit(this);
    }
}
