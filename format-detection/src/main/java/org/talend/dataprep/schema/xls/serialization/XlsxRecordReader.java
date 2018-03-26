package org.talend.dataprep.schema.xls.serialization;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.talend.dataprep.schema.DeSerializer;
import org.talend.dataprep.schema.xls.streaming.StreamingReader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class XlsxRecordReader implements DeSerializer.RecordReader {

    private static final Logger LOGGER = getLogger(XlsxRecordReader.class);

    private final String[] columnsIds;

    private final int numberOfHeaderLines;

    private boolean closed = false;

    private final Workbook workbook;

    private final Iterator<Row> sheetIterator;

    public XlsxRecordReader(InputStream rawContent, String[] columnsIds, String sheetName, int numberOfHeaderLines) {
        this.columnsIds = columnsIds;
        this.numberOfHeaderLines = numberOfHeaderLines;
        if (!rawContent.markSupported()) {
            rawContent = new BufferedInputStream(rawContent, 8);
        }

        workbook = StreamingReader.builder().bufferSize(4096).rowCacheSize(1).open(rawContent);
        sheetIterator = StringUtils.isEmpty(sheetName) ? workbook.getSheetAt(0).iterator() : workbook.getSheet(sheetName).iterator();

        skipHeader();
    }

    @Override
    public void close() {
        try {
            workbook.close();
        } catch (IOException e) {
            LOGGER.warn("Could not properly close XLSX workbook.", e);
        } finally {
            closed = true;
        }
    }

    @Override
    public DeSerializer.Record read() {
        DeSerializer.Record record;
        if (closed) {
            record = null;
        } else {
            String[] line;

            // read until next non-empty line
            Row nextRow;
            do {
                nextRow = sheetIterator.hasNext() ? sheetIterator.next() : null;
                line = convertRowToLine(nextRow, columnsIds.length);
            } while (line != null && isLineEmpty(line));

            if (line != null) {
                // we have a line
                Map<String, String> values = new HashMap<>(columnsIds.length);
                for (int columnId = 0; columnId < columnsIds.length; columnId++) {
                    String cellValue;
                    if (columnId < line.length && line[columnId] != null) {
                        // normal cell value
                        cellValue = line[columnId];
                    } else {
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

    private static String[] convertRowToLine(Row row, int size) {
        String[] line;
        if (row == null) {
            line = null;
        } else {
            line = new String[size];
            for (int colId = 0; colId < line.length; colId++) {
                Cell cell = row.getCell(colId);
                line[colId] = cell == null ? null : cell.getStringCellValue();
            }
        }
        return line;
    }

    private boolean isLineEmpty(String[] line) {
        return line.length == 1 && (StringUtils.isEmpty(line[0]) || line[0].charAt(0) == Character.MIN_VALUE);
    }

    private void skipHeader() {
        int i = 0;
        while (i++ < numberOfHeaderLines && sheetIterator.hasNext()) {
            sheetIterator.next();
        }
    }
}
