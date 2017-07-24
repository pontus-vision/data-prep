package org.talend.dataprep.transformation.pipeline.runtime;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.IndexedRecord;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.dataprep.api.dataset.row.AvroUtils;

class ToAvroCollector extends DoFn<KV<IndexedRecord, AvroUtils.Metadata>, KV<IndexedRecord, AvroUtils.Metadata>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToAvroCollector.class);

    private static final Map<String, DataFileWriter<IndexedRecord>> writers = new HashMap<>();

    private final String outputFile;

    private DataFileWriter<IndexedRecord> writer;

    private Schema schema;

    ToAvroCollector(String outputFile) {
        this.outputFile = outputFile;
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        final KV<IndexedRecord, AvroUtils.Metadata> row = c.element();
        try {
            if (schema == null) {
                schema = row.getKey().getSchema();
            }
            synchronized (writers) {
                if (writer == null) {
                    if (writers.get(outputFile) == null) {
                        writer = new DataFileWriter<>(new GenericDatumWriter<>(schema));
                        writer.create(schema, new File(outputFile));
                        writers.put(outputFile, writer);
                    } else {
                        writer = writers.get(outputFile);
                    }
                }
            }

            synchronized (writer) {
                writer.append(row.getKey());
            }
        } catch (IOException e) {
            LOGGER.error("Unable to append record {}.", row, e);
            throw new RuntimeException(e);
        }
        c.output(row);
    }

    @Teardown
    public synchronized void cleanup() {
        if (writer == null) {
            return;
        }
        try {
            writer.flush();
            writer.close();
        } catch (Exception e) {
            LOGGER.error("Unable to close writer.", e);
        } finally {
            writer = null;
        }
    }

}
