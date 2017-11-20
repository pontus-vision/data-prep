/*
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * https://github.com/Talend/data-prep/blob/master/LICENSE
 *
 * You should have received a copy of the agreement
 * along with this program; if not, write to Talend SA
 * 9 rue Pages 92150 Suresnes, France
 */

package org.talend.dataprep.transformation;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.talend.dataprep.parameters.ParameterType.LIST;
import static org.talend.dataprep.parameters.ParameterType.STRING;
import static org.talend.dataprep.transformation.actions.datablending.Lookup.Parameters.*;

import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.talend.dataprep.api.action.ActionDefinition;
import org.talend.dataprep.api.dataset.DataSetMetadata;
import org.talend.dataprep.parameters.Parameter;
import org.talend.dataprep.transformation.actions.common.ImplicitParameters;

public class LookupWithNewApi implements WantedActionInterface {

    /** The action name. */
    public static final String LOOKUP_ACTION_NAME = "lookup_v2"; //$NON-NLS-1$

    @Override
    public String getName() {
        return LOOKUP_ACTION_NAME;
    }

    @Override
    public ActionForm getActionForm(ActionContext context, Locale locale) {
        ActionForm actionForm = new ActionForm();
        final List<Parameter> parameters = new ArrayList<>();
        parameters.add(ImplicitParameters.COLUMN_ID.getParameter());
        parameters.add(ImplicitParameters.FILTER.getParameter());
        DataSetMetadata currentDatasetMetadata = context.getDataset().getMetadata();
        parameters.add(new Parameter(LOOKUP_DS_NAME.getKey(), STRING, currentDatasetMetadata.getName(), false, false, StringUtils.EMPTY));
        parameters.add(new Parameter(LOOKUP_DS_ID.getKey(), STRING, currentDatasetMetadata.getId(), false, false, StringUtils.EMPTY));
        parameters.add(new Parameter(LOOKUP_JOIN_ON.getKey(), STRING, EMPTY, false, false, StringUtils.EMPTY));
        parameters.add(new Parameter(LOOKUP_JOIN_ON_NAME.getKey(), STRING, EMPTY, false, false, StringUtils.EMPTY));
        parameters.add(new Parameter(LOOKUP_SELECTED_COLS.getKey(), LIST, EMPTY, false, false, StringUtils.EMPTY));

        actionForm.parameters = parameters;


        return actionForm;
    }

    @Override
    public CompiledAction compile(ActionContext context) {
        return null;
    }

    @Override
    public boolean accept(ActionContext context) {
        return false; // but should be true, the action should not decide how it must be rendered only that it can or cannot be used
    }

    @Override
    public Set<ActionDefinition.Behavior> getBehavior() {
        return EnumSet.of(ActionDefinition.Behavior.METADATA_CREATE_COLUMNS);
    }
}
