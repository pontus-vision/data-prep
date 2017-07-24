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

package org.talend.dataprep.transformation.actions.common;

import static org.talend.dataprep.transformation.actions.common.RunnableAction.Builder.builder;

import java.util.HashMap;
import java.util.Map;

import org.talend.dataprep.transformation.actions.ActionDefinition;
import org.talend.dataprep.transformation.actions.category.ScopeCategory;
import org.talend.dataprep.transformation.actions.validation.ActionMetadataValidation;

public class ActionFactory {

    /** The validator. */
    private final ActionMetadataValidation validator = new ActionMetadataValidation();

    /**
     * Get the scope category from parameters
     *
     * @param parameters the transformation parameters
     * @return the scope
     */
    private ScopeCategory getScope(final Map<String, String> parameters) {
        return ScopeCategory.from(parameters.get(ImplicitParameters.SCOPE.getKey()));
    }

    public final RunnableAction create(ActionDefinition actionDefinition, Map<String, String> parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null.");
        }
        validator.checkScopeConsistency(actionDefinition, parameters);

        final Map<String, String> parametersCopy = new HashMap<>(parameters);
        final ScopeCategory scope = getScope(parametersCopy);
        actionDefinition = actionDefinition.adapt(scope);

        return builder().withName(actionDefinition.getName()) //
                .withParameters(parametersCopy) //
                .withCompile(new CompileDataSetRowAction(parametersCopy, actionDefinition, scope))
                .withRow(new ApplyDataSetRowAction(actionDefinition, parameters, scope)) //
                .build();
    }

}
