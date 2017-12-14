package org.talend.dataprep.test;

import java.util.*;

import org.talend.dataprep.api.action.Action;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.parameters.Parameter;
import org.talend.dataprep.parameters.UISchemaParameter;
import org.talend.dataprep.transformation.actions.common.AbstractActionMetadata;
import org.talend.dataprep.transformation.actions.common.ColumnAction;
import org.talend.dataprep.transformation.api.action.context.ActionContext;

@Action(AbstractActionMetadata.ACTION_BEAN_PREFIX + "myUiSchemaAction")
public class UISchemaAction extends AbstractActionMetadata implements ColumnAction  {

    @Override
    public String getName() {
        return "myUISchemaAction";
    }

    @Override
    public String getCategory(Locale locale) {
        return "Test Functions";
    }

    @Override
    public boolean acceptField(ColumnMetadata column) {
        return true;
    }

    @Override
    public List<Parameter> getParameters(Locale locale) {
        return Collections.singletonList(new UISchemaParameter("{\"message\": \"hey regarde, c un bo param\"}"));
    }

    @Override
    public Set<Behavior> getBehavior() {
        return EnumSet.of(Behavior.VALUES_COLUMN);
    }

    @Override
    public void applyOnColumn(DataSetRow row, ActionContext context) {
        // Do nothing.
    }
}
