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
package org.talend.dataprep.transformation.actions.math;

import static org.apache.commons.math3.util.FastMath.exp;
import static org.talend.daikon.number.BigDecimalParser.toBigDecimal;
import static org.talend.dataprep.transformation.actions.math.Exponential.EXPONENTIAL_NAME;

import org.talend.dataprep.api.action.Action;
import org.talend.dataprep.transformation.api.action.context.ActionContext;

/**
 * Create a new column with Exponential
 */
@Action(EXPONENTIAL_NAME)
public class Exponential extends AbstractMathNoParameterAction {

    public static final String EXPONENTIAL_NAME = "exponential_numbers";

    @Override
    protected String calculateResult(String columnValue, ActionContext context) {
        double value = toBigDecimal(columnValue).doubleValue();

        return Double.toString(exp(value));
    }

    protected String getSuffix(ActionContext context) {
        return "_exponential";
    }

    @Override
    public String getName() {
        return EXPONENTIAL_NAME;
    }

}
