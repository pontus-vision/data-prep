package org.talend.dataprep.schema.csv;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.dataprep.schema.DeSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

class CsvRecordReader implements DeSerializer.RecordReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvRecordReader.class);

    private final String[] columnsIds;

    private final String separator;

    private final int numberOfHeaderLines;

    private boolean closed = false;

    private CSVReader reader;

    public CsvRecordReader(InputStream rawContent, Charset encoding, char actualSeparator, char textEnclosureChar,
            char escapeChar, String[] columnsIds, String separator, int numberOfHeaderLines)
            throws IOException {
        this.columnsIds = columnsIds;
        this.separator = separator;
        this.numberOfHeaderLines = numberOfHeaderLines;
        reader = new CSVReader(new InputStreamReader(rawContent, encoding), actualSeparator, textEnclosureChar,
                escapeChar);
        skipHeader();
    }

    @Override
    public void close() {
        try {
            reader.close();
        } catch (IOException e) {
            LOGGER.warn("Could not properly close CSV stream reader.", e);
        } finally {
            closed = true;
        }
    }

    @Override
    public DeSerializer.Record read() throws IOException {
        DeSerializer.Record record;
        if (closed) {
            record = null;
        } else {
            String[] line;

            // read until next non-empty line
            do {
                line = reader.readNext();
            } while (line != null && isLineEmpty(line));

            if (line != null) {
                // we have a line
                Map<String, String> values = new HashMap<>(columnsIds.length);
                for (int columnId = 0; columnId < columnsIds.length; columnId++) {
                    String cellValue;
                    if (columnId == columnsIds.length - 1 && line.length > columnsIds.length) {
                        // when they are more column that in header => put it in the last cell
                        String additionalContent = getRemainingColumns(line, columnId, separator);
                        cellValue = cleanCharacters(additionalContent);
                    } else if (columnId < line.length && line[columnId] != null) {
                        // normal cell value
                        cellValue = cleanCharacters(line[columnId]);
                    } else {
                        // cell is not present
                        cellValue = null;
                    }
                    values.put(columnsIds[columnId], cellValue);
                }
                record = new DeSerializer.Record(values);
            } else {
                // we hit the end of the stream or the limit, close stream
                close();
                record = null;
            }
        }
        return record;
    }

    private boolean isLineEmpty(String[] line) {
        return line.length == 1 && (StringUtils.isEmpty(line[0]) || line[0].charAt(0) == Character.MIN_VALUE);
    }

    private void skipHeader() throws IOException {
        int i = 0;
        while (i++ < numberOfHeaderLines) {
            reader.readNext();
        }
    }

    private static String cleanCharacters(final String value) {
        return StringUtils.remove(value, Character.MIN_VALUE); // unicode null character
    }

    /**
     * Return the remaining raw (with separators) content of the column.
     *
     * @param line the line to parse.
     * @param start where to start in the line.
     * @param separator the separator to append.
     * @return the remaining raw (with separators) content of the column.
     */
    private static String getRemainingColumns(String[] line, int start, String separator) {
        StringBuilder buffer = new StringBuilder();
        for (int j = start; j < line.length; j++) {
            buffer.append(line[j]);
            if (j < line.length - 1) {
                buffer.append(separator);
            }
        }
        return buffer.toString();
    }
}
