package org.talend.dataprep.schema;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public interface DeSerializer {

    /**
     * Mimic old Serializer but without generating the JSON leaving that part to the DataSetContentStore.
     *
     * @param rawContent the raw content of the file
     * @param format
     * @param content
     * @return a reader for our records
     */
    RecordReader deserialize(InputStream rawContent, Format format, SheetContent content);

    interface RecordReader extends Closeable {

        Record read() throws IOException;

    }

    class Record {

        private final Map<String, String> valuesById;

        public Record(Map<String, String> valuesById) {
            this.valuesById = valuesById;
        }

        public Map<String, String> getValues() {
            return valuesById;
        }
    }
}
