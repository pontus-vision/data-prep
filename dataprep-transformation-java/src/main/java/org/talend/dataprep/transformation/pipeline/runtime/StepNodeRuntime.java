package org.talend.dataprep.transformation.pipeline.runtime;

import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Signal;
import org.talend.dataprep.transformation.pipeline.node.StepNode;
import org.talend.dataprep.transformation.service.StepMetadataRepository;

class StepNodeRuntime implements RuntimeNode {

    private final StepNode stepNode;

    private final RuntimeNode nextNode;

    private final StepMetadataRepository stepMetadataRepository;

    private RowMetadata metadata;

    StepNodeRuntime(StepNode stepNode, StepMetadataRepository stepMetadataRepository, RuntimeNode nextNode) {
        this.stepNode = stepNode;
        this.stepMetadataRepository = stepMetadataRepository;
        this.nextNode = nextNode;
    }

    @Override
    public void receive(DataSetRow row) {
        metadata = row.getRowMetadata();
        if (nextNode != null) {
            nextNode.receive(row);
        }
    }

    @Override
    public void signal(Signal signal) {
        if (signal == Signal.END_OF_STREAM) {
            stepMetadataRepository.update(stepNode.getStep().id(), metadata);
        }
        if (nextNode != null) {
            nextNode.signal(signal);
        }
    }
}
