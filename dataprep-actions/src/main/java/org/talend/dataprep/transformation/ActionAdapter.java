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

import static java.lang.Integer.parseInt;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.leftPad;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.avro.generic.GenericRecord;
import org.talend.dataprep.api.action.ActionDefinition;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.api.type.Type;
import org.talend.dataprep.parameters.Parameter;
import org.talend.dataprep.transformation.actions.category.ScopeCategory;
import org.talend.dataprep.transformation.actions.common.*;
import org.talend.dataprep.transformation.api.action.context.ActionContext;

public class ActionAdapter implements InternalActionDefinition {

    private static final String COMPILED_ACTION_KEY = "compiled_action_key";

    private WantedActionInterface delegate;

    public ActionAdapter(WantedActionInterface delegate) {
        this.delegate = delegate;
    }

    /// IDENTIFICATION

    @Override
    public String getName() {
        return delegate.getName();
    }

    // FRONT REPRESENTATION

    @Override
    public String getCategory() {
        return delegate.getActionForm(Locale.getDefault()).getCategory();
    }

    @Override
    public String getLabel() {
        return delegate.getActionForm(Locale.getDefault()).getLabel();
    }

    @Override
    public String getDescription() {
        return delegate.getActionForm(Locale.getDefault()).getDescription();
    }

    @Override
    public String getDocUrl() {
        return delegate.getActionForm(Locale.getDefault()).getDocumentationUrl();
    }

    @Override
    public List<Parameter> getParameters() {
        return delegate.getActionForm(Locale.getDefault()).getParameters();
    }

    // OPTIMIZATION
    @Override
    public Set<Behavior> getBehavior() {
        return delegate.getBehavior();
    }


    // USELESS filtering tool redundant with scopes interfaces
    @Override
    public final boolean acceptScope(final ScopeCategory scope) {
        switch (scope) {
        case CELL:
            return this instanceof CellAction;
        case LINE:
            return this instanceof RowAction;
        case COLUMN:
            return this instanceof ColumnAction;
        case DATASET:
            return this instanceof DataSetAction;
        default:
            return false;
        }
    }

    @Override
    public Function<GenericRecord, GenericRecord> action(List<Parameter> parameters) {
        return r -> r;
    }


    // used to see if an action is applicable to column for suggestion engine
    // can be replaced by an "applicable" with context (access to selection...)
    @Override
    public boolean acceptField(ColumnMetadata column) {
        return delegate.accept(new WantedActionInterface.ActionContext() {

            @Override
            public WantedActionInterface.Row getSelectedRow() {
                return null;
            }

            @Override
            public WantedActionInterface.Column getSelectedColumn() {
                return new ColumnAdapter(column);
            }

            @Override
            public WantedActionInterface.Metadata getMetadata(){
                return null;
            }

            @Override
            public Map<String, String> getParameters() {
                return null;
            }
        });
    }

    // build the action
    // should return a stateful action that is applicable to the run
    @Override
    public void compile(ActionContext actionContext) {
        actionContext.get(COMPILED_ACTION_KEY, map -> delegate.compile(new ActionContextAdapter(actionContext)));
    }

    // This should be used by subclasses implementing one of the scope interfaces
    protected void apply(final DataSetRow row, final ActionContext context) {
        List<ColumnMetadata> columns = context.getRowMetadata().getColumns();
        // create input
        WantedActionInterface.Row rowIn = convertToRow(row, columns);

        // apply transformation
        WantedActionInterface.CompiledAction compiledAction = context.get(COMPILED_ACTION_KEY);
        List<WantedActionInterface.Row> rowsOut = compiledAction.apply(rowIn);

        // translate in other way
        if (rowsOut == null || rowsOut.isEmpty()) {
            row.setDeleted(true);
        } else if (rowsOut.size() == 1) {
            WantedActionInterface.Row modifiedRow = rowsOut.iterator().next();
            for (ColumnMetadata columnMetadata : columns) {
                int colId = Integer.parseInt(columnMetadata.getId());
                row.set(columnIdAsString(colId), modifiedRow.getValue(colId));
            }
        } else {
            throw new UnsupportedOperationException("Cannot yet create rows.");
        }
    }

    private WantedActionInterface.Row convertToRow(DataSetRow row, List<ColumnMetadata> columns) {
        WantedActionInterface.Row rowIn = new WantedActionInterface.Row(new String[columns.size()]);
        for (ColumnMetadata columnMetadata : columns) {
            String colIdAsString = columnMetadata.getId();
            int colId = Integer.parseInt(colIdAsString);
            rowIn = rowIn.setValue(colId, row.get(colIdAsString));
        }
        return rowIn;
    }

    private String columnIdAsString(int columnId) {
        return leftPad(Integer.toString(columnId), 4, '0');
    }

    // totally useless
    @Override
    public boolean implicitFilter() {
        return false;
    }
    // used to adapt parameters/labels to UI
    @Override
    public ActionDefinition adapt(ColumnMetadata column) {
        return this;
    }
    // useless
    @Override
    public ActionDefinition adapt(ScopeCategory scope) {
        return this;
    }

    @Override
    public List<String> getActionScope() {
        return emptyList();
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    private static class ColumnAdapter implements WantedActionInterface.Column {

        private final ColumnMetadata column;

        public ColumnAdapter(ColumnMetadata column) {this.column = column;}

        @Override
        public int getId() {
            return parseInt(column.getId());
        }

        @Override
        public String getName() {
            return column.getName();
        }

        @Override
        public void setName(String name) {
            column.setName(name);
        }

        @Override
        public Type getType() {
            String type = column.getType();
            return type == null ? null : Type.get(type);
        }

        @Override
        public void setType(Type type) {
            column.setType(type == null ? null : type.name());
            column.setTypeForced(true);
        }

        @Override
        public ImmutableSemanticDomain getDomain() {
            return new ImmutableSemanticDomain(column.getDomain(), column.getDomainLabel(), column.getDomainFrequency());
        }

        @Override
        public ImmutableStatistics getStatistics() {
            return new ImmutableStatistics(column.getStatistics());
        }
    }

    private static class ActionContextAdapter implements WantedActionInterface.ActionContext {

        private ActionContext delegate;

        public ActionContextAdapter(ActionContext context) {
            delegate = context;
        }

        @Override
        public WantedActionInterface.Row getSelectedRow() {
            return null;
        }

        @Override
            public WantedActionInterface.Column getSelectedColumn() {
            String columnId = delegate.getColumnId();
            ColumnMetadata column = delegate.getRowMetadata().getById(columnId);

            return new ColumnAdapter(column);
        }

        @Override
        public WantedActionInterface.Metadata getMetadata() {
            return null;
        }

        @Override
        public Map<String, String> getParameters() {
            return delegate.getParameters();
        }
    }

}

