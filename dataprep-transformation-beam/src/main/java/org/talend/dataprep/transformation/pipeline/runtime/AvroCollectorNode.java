package org.talend.dataprep.transformation.pipeline.runtime;

import static org.talend.dataprep.api.dataset.row.AvroUtils.toDataSetRow;
import static org.talend.dataprep.api.dataset.row.AvroUtils.toRowMetadata;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.FileReader;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.dataprep.api.dataset.row.AvroUtils;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.node.CollectorNode;

class AvroCollectorNode extends CollectorNode {

    private static final Logger LOGGER = LoggerFactory.getLogger(AvroCollectorNode.class);

    private final String inputFile;

    AvroCollectorNode(String inputFile) {
        this.inputFile = inputFile;
    }

    @Override
    public List<DataSetRow> collect() {
        final List<DataSetRow> rows = new ArrayList<>();
        DatumReader<GenericRecord> datumReader = new GenericDatumReader<>();
        File file = new File(inputFile);
        try (FileReader<GenericRecord> reader = new DataFileReader<>(file, datumReader)) {
            while (reader.hasNext()) {
                GenericRecord record = reader.next();
                final AvroUtils.Metadata metadata = new AvroUtils.Metadata((Long) record.get(""), toRowMetadata(record.getSchema()).getColumns());
                rows.add(toDataSetRow(record, metadata));
            }
        } catch (IOException e) {
            LOGGER.warn("Unable to read all collected records (might be a zero-length file).");
            LOGGER.debug("Unable to read file due to exception.", e);
        } finally {
            if (!file.delete()) {
                LOGGER.warn("Unable to delete file '{}'.", file);
            }
        }
        return rows;
    }

}
