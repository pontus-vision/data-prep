package org.talend.dataprep.transformation.pipeline.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.actions.context.TransformationContext;
import org.talend.dataprep.transformation.pipeline.Signal;

class CleanUpRuntime implements RuntimeNode {

    private static final Logger LOGGER = LoggerFactory.getLogger(CleanUpRuntime.class);

    private final RuntimeNode nextNode;

    private final TransformationContext context;

    CleanUpRuntime(TransformationContext context, RuntimeNode nextNode) {
        this.nextNode = nextNode;
        this.context = context;
    }

    @Override
    public void receive(DataSetRow row) {
        nextNode.receive(row);
    }

    @Override
    public void signal(Signal signal) {
        if (signal == Signal.END_OF_STREAM || signal == Signal.CANCEL || signal == Signal.STOP) {
            try {
                context.cleanup();
            } catch (Exception e) {
                LOGGER.error("Unable to clean context at {}.", signal, e);
            }
        }
        if (nextNode != null) {
            nextNode.signal(signal);
        }
    }
}
