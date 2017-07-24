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

package org.talend.dataprep.transformation.pipeline.builder;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.preparation.Action;
import org.talend.dataprep.transformation.actions.ActionDefinition;

public class ActionsProfile {

    private final boolean needFullAnalysis;

    private final boolean needOnlyInvalidAnalysis;

    private final Predicate<ColumnMetadata> filterForFullAnalysis;

    private final Predicate<ColumnMetadata> filterForInvalidAnalysis;

    private final Predicate<ColumnMetadata> filterForPatternAnalysis;

    private final Map<Action, ActionDefinition> behaviors;

    ActionsProfile(final boolean needFullAnalysis, //
                   final boolean needOnlyInvalidAnalysis, //
                   final Predicate<ColumnMetadata> filterForFullAnalysis, //
                   final Predicate<ColumnMetadata> filterForInvalidAnalysis, //
                   final Predicate<ColumnMetadata> filterForPatternAnalysis,
                   final Map<Action, ActionDefinition> behaviors) {
        this.needFullAnalysis = needFullAnalysis;
        this.needOnlyInvalidAnalysis = needOnlyInvalidAnalysis;
        this.filterForFullAnalysis = filterForFullAnalysis;
        this.filterForInvalidAnalysis = filterForInvalidAnalysis;
        this.filterForPatternAnalysis = filterForPatternAnalysis;
        this.behaviors = behaviors;
    }

    public Predicate<ColumnMetadata> getFilterForFullAnalysis() {
        return filterForFullAnalysis;
    }

    public Predicate<ColumnMetadata> getFilterForPatternAnalysis() {
        return filterForPatternAnalysis;
    }

    public Predicate<ColumnMetadata> getFilterForInvalidAnalysis() {
        return filterForInvalidAnalysis;
    }

    public boolean needFullAnalysis() {
        return needFullAnalysis;
    }

    public boolean needOnlyInvalidAnalysis() {
        return needOnlyInvalidAnalysis;
    }

    public Set<ActionDefinition.Behavior> getBehavior(Action action) {
        final ActionDefinition definition = behaviors.get(action);
        if (definition != null) {
            return definition.getBehavior();
        } else {
            return null;
        }
    }
}
