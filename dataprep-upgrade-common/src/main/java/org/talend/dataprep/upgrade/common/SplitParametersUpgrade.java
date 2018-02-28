// ============================================================================
//
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

package org.talend.dataprep.upgrade.common;

import static org.talend.tql.api.TqlBuilder.eq;

import java.util.Map;

import org.talend.dataprep.api.preparation.Action;
import org.talend.dataprep.preparation.store.PreparationRepository;
import org.talend.dataprep.transformation.actions.text.Split;

public class SplitParametersUpgrade {

    private static final String SEPARATOR_PARAMETER = "separator";

    private static final String OLD_OTHER_REGEX_PARAMETER = "other (regex)";

    private static final String OLD_OTHER_STRING_PARAMETER = "other (string)";

    private static final String NEW_OTHER_REGEX_PARAMETER = "other_regex";

    private static final String NEW_OTHER_STRING_PARAMETER = "other_string";

    private SplitParametersUpgrade() {
    }

    public static void upgradeActions(PreparationRepository preparationRepository) {
        ParameterMigration.upgradeParameters(preparationRepository, eq("actions.action", //
                Split.SPLIT_ACTION_NAME), //
                SplitParametersUpgrade::updateAction);
    }

    private static void updateAction(Action action) {
        final Map<String, String> parameters = action.getParameters();
        final String separator = parameters.get(SEPARATOR_PARAMETER);
        if (OLD_OTHER_REGEX_PARAMETER.equals(separator)) {
            parameters.put(SEPARATOR_PARAMETER, NEW_OTHER_REGEX_PARAMETER);
        } else if (OLD_OTHER_STRING_PARAMETER.equals(separator)) {
            parameters.put(SEPARATOR_PARAMETER, NEW_OTHER_STRING_PARAMETER);
        }
    }
}
