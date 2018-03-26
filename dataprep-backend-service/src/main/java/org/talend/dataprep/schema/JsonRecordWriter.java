package org.talend.dataprep.schema;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * Writer for {@link org.talend.dataprep.schema.DeSerializer.Record Records} in a JSON stream as an array of objects.
 */
public class JsonRecordWriter implements Closeable {

    private final JsonGenerator generator;

    public JsonRecordWriter(OutputStream out) throws IOException {
        generator = new JsonFactory().createGenerator(out);
        generator.writeStartArray();
    }

    public void writeRecord(DeSerializer.Record record) throws IOException {
        if (record != null) {
            generator.writeStartObject();
            Map<String, String> values = record.getValues();
            for (Map.Entry<String, String> recordField : values.entrySet()) {
                generator.writeStringField(recordField.getKey(), recordField.getValue());
            }
            generator.writeEndObject();
        }
    }

    @Override
    public void close() throws IOException {
        generator.close();
    }
}
