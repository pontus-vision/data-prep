// ============================================================================
//
// Copyright (C) 2006-2018 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.transformation.format;

import static org.slf4j.LoggerFactory.getLogger;
import static org.talend.dataprep.transformation.format.JsonFormat.JSON;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.CommonErrorCodes;
import org.talend.dataprep.transformation.api.transformer.TransformerWriter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Writer to serialize a dataset content as JSON.
 * The JSON output is an object with two fields:<br/>
 * - "records": an array of key-value records objects<br/>
 * - "metadata": object with one field: "columns" which is an array of
 * {@link org.talend.dataprep.api.dataset.ColumnMetadata ColumnMetadata} objects.
 *
 * This writer buffers {@link RowMetadata} to write it at the end of the object.
 */
@Scope("prototype")
@Component("writer#" + JSON)
public class JsonWriter implements TransformerWriter {

    private static final Logger LOGGER = getLogger(JsonWriter.class);

    private static final String METADATA_FIELD_NAME = "metadata";

    private static final String METADATA_COLUMNS_FIELD_NAME = "columns";

    private static final String RECORDS_FIELD_NAME = "records";

    /** Where this writer should write. */
    private final OutputStream output;

    /** The data-prep ready jackson module. */
    @Autowired
    private ObjectMapper mapper;

    /** Jackson generator. */
    private JsonGenerator generator;

    /** Flag to mark the writer as only receiving records for now. */
    private boolean writingRecords;

    /** If receiving metadata while writing records => go to buffer and write at close. */
    private RowMetadata bufferedRowMetadata;

    /** Buffer to store records before we received the metadata. */
    private ObjectBuffer<BufferedDataSetRow> recordsBuffer;

    private boolean closed = false;

    /**
     * Default constructor.
     *
     * @param output Where this writer should write.
     */
    public JsonWriter(final OutputStream output) {
        this.output = output;
    }

    /**
     * <b>Needed</b> private constructor for the WriterRegistrationService.
     *
     * @param output where to write the transformation.
     * @param params ignored parameters.
     */
    private JsonWriter(final OutputStream output, final Map<String, String> params) {
        this(output);
    }

    /**
     * Init the writer.
     *
     * @throws IOException if an error occurs.
     */
    @PostConstruct
    private void init() throws IOException {
        this.generator = mapper.getFactory().createGenerator(output);
        openRootObject();
    }

    @Override
    public void write(final RowMetadata rowMetadata) throws IOException {
        this.bufferedRowMetadata = rowMetadata;
        writeRowMetadataObject(rowMetadata);
        if (!writingRecords) {
            startRecordsWriting();
        }
        writeRecordsBuffer();
    }

    private void writeRecordsBuffer() throws IOException {
        if (recordsBuffer != null) {
            try {
                recordsBuffer.readAll().forEach(row -> {
                    try {
                        generator.writeObject(row.values);
                    } catch (IOException e) {
                        LOGGER.debug("Could not write the records in the json.", e);
                    }
                });
            } finally {
                safeCloseObjectBuffer();
            }
        }
    }

    private void safeCloseObjectBuffer() throws IOException {
        if (recordsBuffer != null) {
            recordsBuffer.close();
            recordsBuffer = null;
        }
    }

    private void openRootObject() throws IOException {
        generator.writeStartObject(); // starts the main JSON object
    }

    private void startRecordsWriting() throws IOException {
        writingRecords = true;
        generator.writeFieldName(RECORDS_FIELD_NAME); // write record field
        generator.writeStartArray(); // open records array
    }

    @Override
    public void write(final DataSetRow row) throws IOException {
        if (bufferedRowMetadata == null) {
            if (recordsBuffer == null) {
                recordsBuffer = new ObjectBuffer<>(BufferedDataSetRow.class);
            }
            recordsBuffer.appendRow(new BufferedDataSetRow(row));
        } else {
            if (!writingRecords) {
                startRecordsWriting();
            }
            generator.writeObject(row.valuesWithId());
        }
    }

    private void endRecordsWriting() throws IOException {
        if (writingRecords) {
            generator.writeEndArray();
            writingRecords = false;
        }
    }

    private void writeRowMetadataObject(RowMetadata rowMetadata) throws IOException {
        generator.writeFieldName(METADATA_FIELD_NAME);
        generator.writeStartObject();
        generator.writeFieldName(METADATA_COLUMNS_FIELD_NAME);
        generator.writeStartArray();
        rowMetadata.getColumns().forEach(col -> {
            try {
                generator.writeObject(col);
            } catch (IOException e) {
                try {
                    // try to close JSon object before throwing an exception
                    generator.writeEndArray();
                    generator.writeEndObject();
                    closeRootObject();
                } catch (IOException e1) {
                    LOGGER.debug("Could not close JSon object after columns writing error.", e1);
                }
                throw new TDPException(CommonErrorCodes.UNABLE_TO_WRITE_JSON, e);
            }
        });
        generator.writeEndArray();
        generator.writeEndObject();
    }

    private void closeRootObject() throws IOException {
        generator.writeEndObject(); // close the main JSON object
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            // This ensure that we write the "records" field even if no record has been received.
            if (!writingRecords) {
                startRecordsWriting();
            }
            if (recordsBuffer != null) {
                writeRecordsBuffer();
            }
            endRecordsWriting();
            closeRootObject();
            generator.flush();
            generator.close();
            closed = true;
        }
    }

    @Override
    public void flush() throws IOException {
        generator.flush();
    }

    private static final class BufferedDataSetRow {

        public Map<String, Object> values;

        public BufferedDataSetRow() {
        }

        public BufferedDataSetRow(DataSetRow row) {
            values = row.valuesWithId();
        }
    }
}
