package org.talend.dataprep.transformation.pipeline.runtime;

import java.util.function.Predicate;

import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Signal;
import org.talend.dataprep.transformation.pipeline.node.FilterNode;

class FilterRuntime implements RuntimeNode {

    private final FilterNode filter;

    private final RuntimeNode nextNode;

    private final Predicate<DataSetRow> mergedFilter;

    FilterRuntime(FilterNode filter, RuntimeNode nextNode) {
        this.filter = filter;
        this.nextNode = nextNode;
        this.mergedFilter = dataSetRow -> {
            for (Predicate<DataSetRow> current : filter.getFilters()) {
                if(!current.test(dataSetRow)) {
                    return false;
                }
            }
            return true;
        };
    }

    @Override
    public void receive(DataSetRow row) {
        if (mergedFilter.test(row)) {
            nextNode.receive(row);
        } else {
            if (filter.getBehavior() == FilterNode.Behavior.INTERRUPT) {
                signal(Signal.STOP);
            }
        }
    }

    @Override
    public void signal(Signal signal) {
        if (nextNode == null) {
            return;
        }
        nextNode.signal(signal);
    }

    @Override
    public RuntimeNode getNext() {
        return nextNode;
    }
}
