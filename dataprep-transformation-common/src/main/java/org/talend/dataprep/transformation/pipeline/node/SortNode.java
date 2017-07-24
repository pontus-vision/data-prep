package org.talend.dataprep.transformation.pipeline.node;

import org.talend.dataprep.transformation.pipeline.Visitor;

public class SortNode extends BasicNode {

    private final String sortField;

    private final String direction;

    public SortNode(String sortField, String direction) {
        this.sortField = sortField;
        this.direction = direction;
    }

    public String getSortField() {
        return sortField;
    }

    public String getDirection() {
        return direction;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitSort(this);
    }
}
