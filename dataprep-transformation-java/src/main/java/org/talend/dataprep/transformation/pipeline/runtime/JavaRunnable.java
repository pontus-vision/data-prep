package org.talend.dataprep.transformation.pipeline.runtime;

import static org.talend.dataprep.transformation.pipeline.Signal.END_OF_STREAM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

        System.out.println("Elapsed times:");
        RuntimeNode current = node;
        List<Monitor> monitors = new ArrayList<>();
        while (current != null) {
            if (current instanceof Monitor) {
                monitors.add((Monitor) current);
            }
            current = current.getNext();
        }

        int index = monitors.size();
        Collections.reverse(monitors);
        long previous = 0;
        final long total = monitors.get(monitors.size() - 1).getElapsedTime();
        System.out.println("Total: " + total);
        for (Monitor monitor : monitors) {
            final long elapsedTime = monitor.getElapsedTime();
            final long ownTime = elapsedTime - previous;
            System.out.println(
                    index-- + ") Node " + monitor + ": own time -> " + ownTime + " (" + (ownTime * 100 / total) + "%)");
            previous = elapsedTime;
        }
    }
}
