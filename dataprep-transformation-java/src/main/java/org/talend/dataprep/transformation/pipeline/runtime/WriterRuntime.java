package org.talend.dataprep.transformation.pipeline.runtime;

import static org.talend.dataprep.transformation.pipeline.Signal.END_OF_STREAM;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Signal;
import org.talend.dataprep.transformation.pipeline.node.TransformerWriter;

class WriterRuntime implements RuntimeNode {

    private static final Logger LOGGER = LoggerFactory.getLogger(WriterRuntime.class);

    private final RuntimeNode nextNode;

    private final AtomicBoolean isStopped = new AtomicBoolean();

    private final TransformerWriter writer;

    private RowMetadata lastRowMetadata;

    private boolean startRecords;

    WriterRuntime(TransformerWriter writer, OutputStream outputStream, RuntimeNode nextNode) {
        this.writer = writer;
        this.nextNode = nextNode;
        startRecords = false;

        this.writer.setOutput(outputStream);
    }

    @Override
    public void receive(DataSetRow row) {
        // do not write this row if the writer is stopped
        if (isStopped.get()) {
            LOGGER.debug("already finished or canceled, let's skip this row");
            return;
        }

        try {
            if (!startRecords) {
                writer.startObject();
                writer.fieldName("records");
                writer.startArray();
                startRecords = true;
            }
            lastRowMetadata = row.getRowMetadata();
            if (row.shouldWrite()) {
                writer.write(row);
            }
        } catch (IOException e) {
            LOGGER.error("Unable to write record.", e);
        } finally {
            if (nextNode != null) {
                nextNode.receive(row);
            }
        }
    }

    @Override
    public void signal(Signal signal) {
        LOGGER.debug("receive {}", signal);

        switch (signal) {
        case END_OF_STREAM:
            endOfStream();
            break;
        case CANCEL:
            cancel();
            break;
        default:
            LOGGER.debug("Unhandled signal {}.", signal);
        }
        if (nextNode != null) {
            nextNode.signal(signal);
        }
    }

    /**
     * Deal with the cancel signal.
     */
    private void cancel() {
        // just set stopped flag to true so that the writer is not used anymore
        this.isStopped.set(true);
    }

    /**
     * Deal with end of stream signal.
     */
    private void endOfStream() {
        if (isStopped.get()) {
            LOGGER.debug("cannot process {} because WriterNode is already finished or canceled", END_OF_STREAM);
            return;
        }

        // set this writer to stopped
        this.isStopped.set(true);

        try {
            // no row received, let's switch to the fallback row metadata
            if (!startRecords) {
                writer.startObject();
                writer.fieldName("records");
                writer.startArray();
            }

            writer.endArray(); // <- end records
            writer.fieldName("metadata"); // <- start metadata
            writer.startObject();

            writer.fieldName("columns");
            if (lastRowMetadata != null) {
                writer.write(lastRowMetadata);
            }

            writer.endObject();

            writer.endObject(); // <- end data set
            writer.flush();
        } catch (IOException e) {
            LOGGER.error("Unable to end writer.", e);
        }
    }
}
