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

package org.talend.dataprep.api.dataset;

import org.talend.dataprep.schema.SheetContent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class represents the result of schema parsing of a dataset file.
 */
public class Schema implements Serializable {

    /** Serialization UID. */
    private static final long serialVersionUID = 1L;

    private boolean draft;

    private List<SheetContent> sheetContents;

    private String sheetName;

    private Schema(boolean draft, List<SheetContent> sheetContents, String sheetName) {
        this.draft = draft;
        this.sheetContents = sheetContents;
        this.sheetName = sheetName;
    }

    private Schema() {
        //
    }

    public boolean draft() {
        return draft;
    }

    public List<SheetContent> getSheetContents() {
        return sheetContents;
    }

    public String getSheetName() {
        return sheetName;
    }

    public List<SheetContent.ColumnMetadata> metadata() {
        List<SheetContent> sheetContents = getSheetContents();
        return sheetContents != null && !sheetContents.isEmpty() ?
                sheetContents.iterator().next().getColumnMetadatas() :
                null;
    }

    @Override
    public String toString() {
        return "Schema{" + "draft=" + draft + ", sheetContents=" + sheetContents + ", sheetName='" + sheetName + '\''
                + '}';
    }

    public static class Builder {

        private boolean draft;

        private List<SheetContent> sheetContents;

        private String sheetName;

        public static Schema.Builder parserResult() {
            return new Builder();
        }

        public Builder draft(boolean draft) {
            this.draft = draft;
            return this;
        }

        public Builder sheetContents(List<SheetContent> sheetContents) {
            if (sheetContents == null) {
                return this;
            }
            this.sheetContents = new ArrayList<>();
            this.sheetContents.addAll(sheetContents.stream().map(SheetContent::clone).collect(Collectors.toList()));
            return this;
        }

        public Builder sheetName(String sheetName) {
            this.sheetName = sheetName;
            return this;
        }

        public Builder copy(Schema original) {
            this.draft = original.draft();
            this.sheetContents = original.getSheetContents();
            this.sheetName = original.getSheetName();
            return this;
        }

        public Schema build() {
            return new Schema(this.draft, this.sheetContents, this.sheetName);
        }
    }
}
