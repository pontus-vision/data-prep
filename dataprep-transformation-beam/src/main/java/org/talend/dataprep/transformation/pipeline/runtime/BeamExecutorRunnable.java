package org.talend.dataprep.transformation.pipeline.runtime;

import java.io.IOException;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.dataprep.transformation.actions.context.TransformationContext;
import org.talend.dataprep.transformation.pipeline.Signal;

class BeamExecutorRunnable implements ExecutorRunnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(BeamExecutorRunnable.class);

    private final Pipeline pipeline;

    private final TransformationContext context;

    private PipelineResult currentRun;

    BeamExecutorRunnable(Pipeline pipeline, TransformationContext context) {
        this.pipeline = pipeline;
        this.context = context;
    }

    @Override
    public void signal(Signal signal) {
        if (currentRun == null) {
            return;
        }
        switch (signal) {
        case END_OF_STREAM:
            currentRun.waitUntilFinish();
            break;
        case STOP:
        case CANCEL:
            try {
                currentRun.cancel();
            } catch (IOException e) {
                LOGGER.error("Unable to cancel pipeline '{}'.", pipeline, e);
            } finally {
                context.cleanup();
            }
            break;
        }
    }

    @Override
    public void run() {
        try {
            currentRun = pipeline.run();
            currentRun.waitUntilFinish();
        } finally {
            context.cleanup();
        }
    }
}
