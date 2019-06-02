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

import java.math.RoundingMode;

import org.talend.dataprep.api.action.Action;

/**
 * Round towards zero. Never increments the digit prior to a discarded fraction (i.e. truncates)
 *
 * @see RoundingMode#DOWN
 */
@Action(RoundDownReal.ACTION_NAME)
public class RoundDownReal extends AbstractRound {

    /**
     * The action name. round_down was taken by RemoveFractionalPart action, and i don't want to change its id
     */
    public static final String ACTION_NAME = "round_down_real"; //$NON-NLS-1$

    @Override
    public String getName() {
        return ACTION_NAME;
    }

    @Override
    protected RoundingMode getRoundingMode() {
        return RoundingMode.DOWN;
    }
}
