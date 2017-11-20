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
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.talend.dataprep.api.action.ActionDefinition;
import org.talend.dataprep.api.type.Type;
import org.talend.dataprep.parameters.Parameter;

/**
 * Action interface
 */

/*
Features that must be present:
- [v] modify any value une the current row
- [v] add/remove rows (return 0 to many rows for one input)
- [v] change headers (pass metadata to compiled and modify it from here)
- [v] access statistical analysis of columns (passed to compiled)
- [v] modify typing of columns (through column access)
- [v] add/remove column (=> change metadata)
- [v] define form for UI representation via ActionForm
- [v] give hints for UI (action scope...) => ActionForm category
- [] access other datasets/preparations/versions (lookup...) => through context.

...


 */

public interface WantedActionInterface {

    /** Unique identifier. */
    String getName();

    /** Representation for UI. */
    ActionForm getActionForm(ActionContext context, Locale locale);

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
         * Direct access on the current dataset, data and metadata. I think this is a good idea if an action can read a whole
         * dataset if needed in action compilation, just as it may need to read other dataset.
         */
        Dataset getDataset();

        Map<String, String> getParameters();

        /** Retrieve another dataset that can be used to compile an action. */
        Dataset getDataset(String name);

    }

    /**
     * Action function used for one run over one stream of data.
     * This must be totally independent from the context as it may be serialized in another env, parallelized...
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

    interface Dataset {

        Stream<Row> getContent();

        String getName();

        List<Column> getMetadata();

        Column getColumn(int id);

        /** shortcut for getColumnMetadata().add(index, column) */
        void addColumn(int index, Column column);

        /** shortcut for getColumnMetadata().remove(index) */
        void deleteColumn(int id);
    }

    interface Column {
        // ID is an arbitrary identifier, modeled by 4 digits as string in old ColumnMetadata
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

        private final Cell[] values;

        public Row(Cell[] values) {
            this.values = ArrayUtils.clone(values);
        }

        public Cell getCell(int columnId) {
            return values[columnId];
        }

        public Row setCell(int columnId, Cell newValue) {
            Cell[] newValues = ArrayUtils.clone(this.values);
            newValues[columnId] = newValue;
            return new Row(newValues);
        }

    }

    // Might be a good encapsulation to access specific cell metadata
    // hesitate with a row method allowing access to cell metadata separately like boolean Row.isValid(int columnId)
    // or even CellMetadata Row.getMetadata(int columnId)
    class Cell {

        private final String value;

        // null means value has not been checked for validity
        private final Boolean valid;

        public Cell(String value) {
            this(value, null);
        }

        // not sure if an action should be able to define a celle as valid
        public Cell(String value, Boolean valid) {
            this.value = value;
            this.valid = valid;
        }

        public String getValue() {
            return value;
        }

        public Boolean isValid() {
            return valid;
        }
    }

}
