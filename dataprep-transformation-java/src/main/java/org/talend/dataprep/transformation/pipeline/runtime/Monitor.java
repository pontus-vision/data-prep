package org.talend.dataprep.transformation.pipeline.runtime;

import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Signal;

public class Monitor implements RuntimeNode {

    private final RuntimeNode delegate;

    private long elapsedTime = 0L;

    private Monitor(RuntimeNode delegate) {
        this.delegate = delegate;
    }

    public static RuntimeNode monitor(RuntimeNode delegate) {
        return new Monitor(delegate);
    }

    @Override
    public void receive(DataSetRow row) {
        final long start = System.currentTimeMillis();
        delegate.receive(row);
        elapsedTime += (System.currentTimeMillis()) - start;
    }

    @Override
    public void signal(Signal signal) {
        final long start = System.currentTimeMillis();
        delegate.signal(signal);
        elapsedTime += (System.currentTimeMillis()) - start;
    }

    @Override
    public RuntimeNode getNext() {
        return delegate.getNext();
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    @Override
    public String toString() {
        return "Monitor(" + delegate.getClass().getSimpleName() + ')';
    }
}
