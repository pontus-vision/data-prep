// ============================================================================
//
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

package org.talend.dataprep.transformation.actions.bool;

import static java.util.Collections.singletonList;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang.WordUtils;
import org.talend.dataprep.api.action.Action;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.api.type.Type;
import org.talend.dataprep.transformation.ActionAdapter;
import org.talend.dataprep.transformation.WantedActionInterface;
import org.talend.dataprep.transformation.actions.category.ActionCategory;
import org.talend.dataprep.transformation.actions.common.AbstractActionMetadata;
import org.talend.dataprep.transformation.actions.common.ColumnAction;
import org.talend.dataprep.transformation.api.action.context.ActionContext;

/**
 * Negate a boolean.
 *
 * @see Negate
 */
@Action(AbstractActionMetadata.ACTION_BEAN_PREFIX + Negate.NEGATE_ACTION_NAME)
public class Negate extends ActionAdapter implements ColumnAction {

    static final String NEGATE_ACTION_NAME = "negate";

    public Negate() {
        super(new NewNegate());
    }

    @Override
    public void applyOnColumn(DataSetRow row, ActionContext context) {
        apply(row, context);
    }

    private static class NewNegate implements WantedActionInterface {

        @Override
        public String getName() {
            return NEGATE_ACTION_NAME;
        }

        @Override
        public ActionForm getActionForm(Locale locale) {
            ActionForm actionForm = new ActionForm();
            actionForm.name = NEGATE_ACTION_NAME;
            actionForm.category = ActionCategory.BOOLEAN.getDisplayName();
            return actionForm;
        }

        @Override
        public CompiledAction compile(ActionContext context) {
            return new CompiledNegate(context.getSelectedColumn().getId());
        }

        @Override
        public boolean accept(ActionContext context) {
            return Type.BOOLEAN.equals(context.getSelectedColumn().getType());
        }

        @Override
        public Set<Behavior> getBehavior() {
            return EnumSet.of(Behavior.VALUES_COLUMN);
        }

    }

    private static class CompiledNegate implements WantedActionInterface.CompiledAction {

        private final int targetColumnId;

        private CompiledNegate(int targetColumnId) {this.targetColumnId = targetColumnId;}

        @Override
        public List<WantedActionInterface.Row> apply(WantedActionInterface.Row row) {
            final String value = row.getValue(targetColumnId);
            if (isBoolean(value)) {
                final Boolean boolValue = Boolean.valueOf(value);
                row = row.setValue(targetColumnId, WordUtils.capitalizeFully("" + !boolValue));
            }
            return singletonList(row);
        }

        private boolean isBoolean(final String value) {
            return value != null && ("true".equalsIgnoreCase(value.trim()) || "false".equalsIgnoreCase(value.trim()));
        }
    }
}
