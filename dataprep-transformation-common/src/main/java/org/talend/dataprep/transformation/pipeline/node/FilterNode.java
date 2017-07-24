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

import java.util.function.Predicate;

import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Visitor;

/**
 * Node that filter input using a provided predicate.
 * If the predicate returns true, it is emitted to the next node.
 */
public class FilterNode extends BasicNode {

    private final Behavior behavior;

    private final Predicate<DataSetRow>[] filters;

    @SafeVarargs
    public FilterNode(final Predicate<DataSetRow>... filters) {
        this(Behavior.CONTINUE, filters);
    }

    @SafeVarargs
    public FilterNode(Behavior behavior, final Predicate<DataSetRow>... filters) {
        this.behavior = behavior;
        this.filters = filters;
    }

    public Behavior getBehavior() {
        return behavior;
    }

    public Predicate<DataSetRow>[] getFilters() {
        return filters;
    }

    @Override
    public Node copyShallow() {
        return new FilterNode(filters);
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitFilterNode(this);
    }

    public enum Behavior {
        INTERRUPT,
        CONTINUE
    }
}
