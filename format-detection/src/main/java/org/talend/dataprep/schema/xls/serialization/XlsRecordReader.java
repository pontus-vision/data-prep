package org.talend.dataprep.schema.xls.serialization;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.talend.dataprep.schema.DeSerializer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.slf4j.LoggerFactory.getLogger;
import static org.talend.dataprep.schema.xls.XlsUtils.getCellValueAsString;

public class XlsRecordReader implements DeSerializer.RecordReader {

    private static final Logger LOGGER = getLogger(XlsRecordReader.class);

    private final String[] columnsIds;

    private final int numberOfHeaderLines;

    private boolean closed = false;

    private final Workbook workbook;

    private final Iterator<Row> sheetIterator;

    public XlsRecordReader(InputStream rawContent, String[] columnsIds, String sheetName, int numberOfHeaderLines)
            throws IOException, InvalidFormatException {
        this.columnsIds = columnsIds;
        this.numberOfHeaderLines = numberOfHeaderLines;
        if (!rawContent.markSupported()) {
            rawContent = new BufferedInputStream(rawContent, 8);
        }

        workbook = WorkbookFactory.create(rawContent);
        sheetIterator = resolveExcelSheet(sheetName, workbook).iterator();

        skipHeader();
    }

    private static Sheet resolveExcelSheet(String sheetName, Workbook workbook) {
        Sheet poiSheet;
        // if no sheet name just get the first one (take it easy mate :-) )
        if (isEmpty(sheetName)) {
            poiSheet = workbook.getSheetAt(0);
        } else {
            poiSheet = workbook.getSheet(sheetName);
            if (poiSheet == null && StringUtils.startsWith(sheetName, "sheet-")) {
                // If the name was not properly given to the sheet ?
                String sheetNumberStr = StringUtils.removeStart(sheetName, "sheet-");
                poiSheet = workbook.getSheetAt(Integer.valueOf(sheetNumberStr));
            } else {
                poiSheet = workbook.getSheetAt(0);
            }
        }
        return poiSheet;
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
                line[colId] = getCellValueAsString(row.getCell(colId), row.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator());
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
