package org.talend.dataprep.transformation.pipeline.node;

import java.util.ArrayList;
import java.util.List;

import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Visitor;

public class CollectorNode extends BasicNode {

    private final List<DataSetRow> rows = new ArrayList<>();

    public List<DataSetRow> collect() {
        List<DataSetRow> collected = new ArrayList<>(rows);
        rows.clear();
        return collected;
    }

    public void add(DataSetRow row) {
        rows.add(row);
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitCollector(this);
    }
}
