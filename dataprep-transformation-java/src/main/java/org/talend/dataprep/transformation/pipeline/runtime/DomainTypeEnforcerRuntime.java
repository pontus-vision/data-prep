package org.talend.dataprep.transformation.pipeline.runtime;

import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Signal;
import org.talend.dataprep.transformation.pipeline.node.DomainAndTypeEnforcerNode;

class DomainTypeEnforcerRuntime implements RuntimeNode {

    private final RuntimeNode nextNode;

    public DomainTypeEnforcerRuntime(RuntimeNode nextNode) {
        this.nextNode = nextNode;
    }

    @Override
    public void receive(DataSetRow row) {
        final RowMetadata rowMetadata = row.getRowMetadata();
        DomainAndTypeEnforcerNode.forceDomainsAndType(rowMetadata);
        final DataSetRow modified = row.setRowMetadata(rowMetadata);
        nextNode.receive(modified);
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
