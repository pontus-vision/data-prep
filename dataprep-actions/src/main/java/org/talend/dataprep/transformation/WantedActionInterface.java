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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.talend.dataprep.api.action.ActionDefinition;
import org.talend.dataprep.api.type.Type;
import org.talend.dataprep.parameters.Parameter;

public interface WantedActionInterface {

    /** Unique identifier. */
    String getName();

    /** Representation for UI. */
    ActionForm getActionForm(Locale locale);

    CompiledAction compile(ActionContext context);

    boolean accept(ActionContext context);

    Set<ActionDefinition.Behavior> getBehavior();

    class ActionForm {
        public String name;
        public String label;
        public String description;
        public String documentationUrl;
        public String category;
        public List<Parameter> parameters;

        public String getName() {
            return name;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }

        public String getDocumentationUrl() {
            return documentationUrl;
        }

        public String getCategory() {
            return category;
        }

        public List<Parameter> getParameters() {
            return parameters;
        }
    }

    interface ActionContext {
        Row getSelectedRow();
        Column getSelectedColumn();
        /**
         * Modifies the output structure. Must not be modifiable once rows are passed to stream.
         */
        WantedActionInterface.Metadata getMetadata();

        Map<String, String> getParameters();

    }

    /**
     * Action function used for one run over one stream of data.
     */
    interface CompiledAction {

        /**
         * Read an input Row and produces the output.
         *
         * @param row the next row from the stream
         * @return list of row to add to the stream. empty or null means the row is deleted.
         */
        List<Row> apply(Row row);

    }

    interface Metadata {
        List<Column> getColumnMetadata();

        Column getColumn(int id);

        void addColumn(int index, Column column);

        void deleteColumn(int id);
    }

    interface Column {
        // ID is an arbitrary identifier, modelized by 4 digits as string in old ColumnMetadata
        int getId();
        // display name of the column, might be changed
        String getName();
        void setName(String name);
        // type may also be changed...
        Type getType();
        // setting type should set forced
        void setType(Type type);
        // Domain is the DQ analysed dominant domain. domain might not be changed.
        ImmutableSemanticDomain getDomain();
        // statistics are decided by the precedent analysis and not updated live
        ImmutableStatistics getStatistics();
    }

    class Row {

        private final String[] values;

        public Row(String[] values) {
            this.values = ArrayUtils.clone(values);
        }

        public String getValue(int columnId) {
            return values[columnId];
        }

        public Row setValue(int columnId, String newValue) {
            String[] newValues = this.values.clone();
            newValues[columnId] = newValue;
            return new Row(newValues);
        }

    }

}
