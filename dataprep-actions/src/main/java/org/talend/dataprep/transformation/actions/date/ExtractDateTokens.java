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

package org.talend.dataprep.transformation.actions.date;

import org.talend.dataprep.api.action.Action;
import org.talend.dataprep.transformation.actions.category.ActionScope;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;

/**
 * Change the date pattern on a 'date' column.
 */
@Action(ExtractDateTokens.ACTION_NAME)
public class ExtractDateTokens extends ExtractDateTokensOrdered {

    /** Action name. */
    public static final String ACTION_NAME = "extract_date_tokens"; //$NON-NLS-1$

    private static final List<DateFieldMapping> DATE_FIELDS = Arrays.asList( //
            YEAR_MAPPING, //
            QUARTER_OF_YEAR, //
            MONTH_OF_YEAR_ID, //
            MONTH_OF_YEAR_LABEL, //
            WEEK_OF_YEAR, //
            DAY_OF_YEAR, //
            DAY_OF_MONTH, //
            DAY_OF_WEEK_ID, //
            DAY_OF_WEEK_LABEL, //
            HOUR_12, //
            AM_PM, //
            HOUR_24, //
            MINUTE, //
            SECOND //
    );

    @Override
    public String getName() {
        return ACTION_NAME;
    }

    @Override
    protected List<DateFieldMapping> getDateFields() {
        return DATE_FIELDS;
    }

    @Override
    public List<String> getActionScope() {
        return singletonList(ActionScope.HIDDEN_IN_ACTION_LIST.getDisplayName());
    }
}
