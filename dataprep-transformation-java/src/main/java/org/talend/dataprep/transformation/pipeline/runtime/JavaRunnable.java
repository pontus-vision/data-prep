package org.talend.dataprep.transformation.pipeline.runtime;

import static org.talend.dataprep.transformation.pipeline.Signal.END_OF_STREAM;

import java.util.stream.Stream;

import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Signal;

class JavaRunnable implements ExecutorRunnable {

    private final RuntimeNode node;

    private final Stream<DataSetRow> stream;

    JavaRunnable(RuntimeNode node, Stream<DataSetRow> stream) {
        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null");
        }
        if (stream == null) {
            throw new IllegalArgumentException("Stream cannot be null");
        }
        this.node = node;
        this.stream = stream;
    }

    @Override
    public void signal(Signal signal) {
        node.signal(signal);
    }

    @Override
    public void run() {
        try (Stream<DataSetRow> records = stream) {
            records.onClose(() -> node.signal(END_OF_STREAM)).forEach(node::receive);
        }
    }
}
