package org.talend.dataprep.schema;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Real schema.
 */
public class SheetContent implements Serializable {

    /** Serialization UID. */
    private static final long serialVersionUID = 1L;

    /** The sheet name. */
    private String name;

    /** List of column metadata. */
    // WARN: correcting this grammar error might hurt the mongo serialization compatibility
    private List<ColumnMetadata> columnMetadatas;

    private Map<String, String> parameters;

    /**
     * Default empty constructor.
     */
    public SheetContent() {
        // no op
    }

    /**
     * Constructor.
     *
     * @param name the sheet name.
     * @param columnMetadatas the list of column metadata.
     */
    public SheetContent(String name, List<ColumnMetadata> columnMetadatas) {
        this.name = name;
        this.columnMetadatas = columnMetadatas;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ColumnMetadata> getColumnMetadatas() {
        return columnMetadatas;
    }

    public void setColumnMetadatas(List<ColumnMetadata> columnMetadatas) {
        this.columnMetadatas = columnMetadatas;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public SheetContent clone() {
        List<ColumnMetadata> columns = null;
        if (this.columnMetadatas != null) {
            columns = new ArrayList<>();
            for (ColumnMetadata column : this.columnMetadatas) {
                columns.add(ColumnMetadata.Builder.column().copy(column).build());
            }
        }
        SheetContent sheetContent = new SheetContent(this.name, columns);
        sheetContent.setParameters(getParameters());
        return sheetContent;
    }

    @Override
    public String toString() {
        return "SheetContent{" + "name='" + name + '\'' + ", columnMetadata=" + columnMetadatas + ", parameters="
                + parameters + '}';
    }

    public static class ColumnMetadata {

        private int id;

        private String name;

        private int headerSize;

        public ColumnMetadata() {
            // for jackson
        }

        public ColumnMetadata(int id, String name, int headerSize) {
            this.id = id;
            this.name = name;
            this.headerSize = headerSize;
        }

        public int getId() {
            return id;
        }

        public String idAsApiColumnId() {
            return StringUtils.leftPad(Integer.toString(getId()), 4, '0');
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getHeaderSize() {
            return headerSize;
        }

        public void setHeaderSize(int headerSize) {
            this.headerSize = headerSize;
        }

        @Override
        public String toString() {
            return "ColumnMetadata{" + "id=" + id + ", name='" + name + '\'' + ", headerSize=" + headerSize + '}';
        }

        public static class Builder {

            private int id;

            private String name;

            private int headerSize;

            public static ColumnMetadata.Builder column() {
                return new ColumnMetadata.Builder();
            }

            public ColumnMetadata.Builder id(int id) {
                this.id = id;
                return this;
            }

            public ColumnMetadata.Builder name(String name) {
                this.name = name;
                return this;
            }

            public ColumnMetadata.Builder headerSize(int headerSize) {
                this.headerSize = headerSize;
                return this;
            }

            public ColumnMetadata.Builder copy(ColumnMetadata columnMetadata) {
                this.id = columnMetadata.getId();
                this.name = columnMetadata.getName();
                this.headerSize = columnMetadata.getHeaderSize();
                return this;
            }

            public ColumnMetadata build() {
                return new ColumnMetadata(id, name, headerSize);
            }
        }
    }

}
