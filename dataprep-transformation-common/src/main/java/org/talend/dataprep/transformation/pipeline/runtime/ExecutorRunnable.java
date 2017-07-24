package org.talend.dataprep.transformation.pipeline.runtime;

import org.talend.dataprep.transformation.pipeline.Signal;

/**
 * Interface only used to be able to interact with a Data Prep pipeline, not depending on underlying chosen execution
 * framework.
 */
public interface ExecutorRunnable extends Runnable {

    /**
     * Send the given signal to the running transformation.
     *
     * @param signal the signal to send.
     */
    void signal(Signal signal);

}
