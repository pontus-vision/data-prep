// ============================================================================
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

package org.talend.dataprep.schema.xls;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.daikon.exception.ExceptionContext;
import org.talend.daikon.exception.TalendRuntimeException;
import org.talend.daikon.exception.error.CommonErrorCodes;
import org.talend.dataprep.schema.SchemaParser;
import org.talend.dataprep.schema.SheetContent;
import org.talend.dataprep.schema.xls.streaming.StreamingReader;
import org.talend.dataprep.schema.xls.streaming.StreamingSheet;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import org.talend.dataprep.schema.Type;

/**
 * This class is in charge of parsing excel file (note apache poi is used for reading .xls)
 *
 * @see <a hrerf="https://poi.apache.org/
 */
public class XlsSchemaParser implements SchemaParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(XlsSchemaParser.class);

    /** Constant used to record blank cell. */
    private static final String BLANK = "blank";

    private int maxNumberOfColumns = Integer.MAX_VALUE;

    @Override
    public List<SheetContent> parse(Request request) {
        LOGGER.debug("parsing {}", request);
        try {
            List<SheetContent> sheetContents = parseAllSheets(request);
            if (sheetContents.isEmpty()) {
                // nothing to parse
                throw new TalendRuntimeException(CommonErrorCodes.UNABLE_TO_READ_CONTENT);
            }
            return sheetContents;
        } catch (TalendRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.debug("IOException during parsing xls request :" + e.getMessage(), e);
            throw new TalendRuntimeException(CommonErrorCodes.UNEXPECTED_EXCEPTION, e);
        }

    }

    /**
     * Parse all xls sheets.
     *
     * @param request the schema parser request.
     * @return the list of parsed xls sheet.
     * @throws IOException if an error occurs.
     */
    protected List<SheetContent> parseAllSheets(Request request) throws IOException {
        InputStream inputStream = request.getContent();
        if (!inputStream.markSupported()) {
            inputStream = new BufferedInputStream(inputStream, 8);
        }
        boolean newExcelFormat = XlsUtils.isNewExcelFormat(inputStream);
        // parse the xls input stream using the correct format
        if (newExcelFormat) {
            return parseAllSheetsStream(inputStream);
        } else {
            return parseAllSheetsOldFormat(inputStream);
        }
    }

    private List<SheetContent> parseAllSheetsStream(InputStream content) {
        Workbook workbook = StreamingReader.builder() //
                .bufferSize(4096) //
                .rowCacheSize(1) //
                .open(content);
        try {
            List<SheetContent> schemas = new ArrayList<>();
            int sheetNumber = 0;
            for (Sheet sheet : workbook) {
                List<SheetContent.ColumnMetadata> columnsMetadata = createMetadataFromFirstNonEmptyRowAndInitSheet(sheet);
                int totalColumnsNumber = getTotalColumnsNumber((StreamingSheet) sheet);

                /*
                 * Protecting the app against too large data sets => It would break mongo by submitting too large empty
                 * column metadata or saturate the memory during analysis.
                 *
                 * @see https://jira.talendforge.org/browse/TDP-3459
                 */
                if (totalColumnsNumber > maxNumberOfColumns) {
                    throw new TalendRuntimeException(XlsErrorCodes.DATASET_HAS_TOO_MANY_COLUMNS, ExceptionContext.build()
                            .put("number-of-columns", totalColumnsNumber).put("max-allowed", maxNumberOfColumns));
                }

                String sheetName = sheet.getSheetName();
                SheetContent sheetContent = new SheetContent(
                        StringUtils.isEmpty(sheetName) ? "sheet-" + sheetNumber : sheetName, columnsMetadata);

                // if less columns found than the metadata we complete
                completeWithEmptyColumnsMetadata(columnsMetadata, totalColumnsNumber);

                HashMap<String, String> parameters = new HashMap<>();
                parameters.put(XlsFormatFamily.HEADER_NB_LINES_PARAMETER, getHeaderSize(columnsMetadata).toString());
                sheetContent.setParameters(parameters);
                schemas.add(sheetContent);
            }
            return schemas;
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                LOGGER.error("Unable to close excel file.", e);
            }
        }
    }

    private static Integer getHeaderSize(List<SheetContent.ColumnMetadata> columns) {
        return columns.stream().map(SheetContent.ColumnMetadata::getHeaderSize).min(Comparator.naturalOrder()).orElse(0);
    }

    private int getTotalColumnsNumber(StreamingSheet sheet) {
        int maxColNumber = sheet.getReader().getColNumber();
        String dimension = sheet.getReader().getDimension();

        if (StringUtils.isNotEmpty(dimension)) {
            int maxColNumberFromDimension = XlsUtils.getColumnsNumberFromDimension(dimension);
            // well for some files they can disagree so we use the biggest one
            if (maxColNumberFromDimension > maxColNumber) {
                maxColNumber = maxColNumberFromDimension;
            }
        }
        return maxColNumber;
    }

    private void completeWithEmptyColumnsMetadata(List<SheetContent.ColumnMetadata> columnsMetadata, int maxColNumber) {
        int numberOfColumnsAlreadyFound = columnsMetadata.size();
        if (numberOfColumnsAlreadyFound < maxColNumber) {
            int columnId = 0;
            for (int appendedColumnId = numberOfColumnsAlreadyFound; appendedColumnId < maxColNumber; appendedColumnId++) {
                columnsMetadata.add(SheetContent.ColumnMetadata.Builder //
                        .column() //
                        .id(columnId) //
                        .name("col_" + columnId) //
                        .headerSize(1) //
                        .build());
                columnId++;
            }
        }
    }

    private List<SheetContent.ColumnMetadata> createMetadataFromFirstNonEmptyRowAndInitSheet(Sheet sheet) {
        List<SheetContent.ColumnMetadata> columnsMetadata = new ArrayList<>();
        int rowNumber = 0;
        for (Row r : sheet) {
            if (rowNumber < 1) {
                if (((StreamingSheet) sheet).getReader().getFirstRowIndex() == 1) {
                    int colId = 0;
                    for (Cell c : r) {
                        String headerText = StringUtils.trim(c.getStringCellValue());
                        // header text cannot be null so use a default one
                        if (StringUtils.isEmpty(headerText)) {
                            headerText = "col_" + (columnsMetadata.size() + 1); // +1 because it starts from 0
                        }
                        columnsMetadata.add(SheetContent.ColumnMetadata.Builder //
                                .column() //
                                .id(colId++) //
                                .name(headerText) //
                                .headerSize(1) //
                                .build());
                    }
                }
            } else {
                break;
            }
            rowNumber++;
        }
        return columnsMetadata;
    }

    /**
     * Parse all xls sheets for old excel document type
     *
     * @return The parsed sheets request.
     * @param content
     */
    private List<SheetContent> parseAllSheetsOldFormat(InputStream content) {
        try {
            List<SheetContent> schemas;
            try (Workbook workbook = WorkbookFactory.create(content)) {
                if (workbook == null) {
                    // TODO: exception thrown is caught in the same method
                    throw new IOException("Could not open parse request content as an excel file");
                }
                int sheetNumber = workbook.getNumberOfSheets();
                if (sheetNumber < 1) {
                    LOGGER.debug("has not sheet to read");
                    return Collections.emptyList();
                }
                schemas = new ArrayList<>();
                for (int i = 0; i < sheetNumber; i++) {
                    Sheet sheet = workbook.getSheetAt(i);
                    if (sheet.getLastRowNum() < 1) {
                        LOGGER.debug("sheet '{}' do not have rows skip ip", sheet.getSheetName());
                        continue;
                    }
                    List<SheetContent.ColumnMetadata> columnsMetadata = parsePerSheet(sheet, //
                            //
                            workbook.getCreationHelper().createFormulaEvaluator());
                    String sheetName = sheet.getSheetName();
                    // update XlsSerializer if this default sheet naming change!!!
                    SheetContent sheetContent = new SheetContent(sheetName == null ? "sheet-" + i : sheetName, columnsMetadata);

                    HashMap<String, String> parameters = new HashMap<>();
                    parameters.put(XlsFormatFamily.HEADER_NB_LINES_PARAMETER, getHeaderSize(columnsMetadata).toString());
                    sheetContent.setParameters(parameters);

                    schemas.add(sheetContent);
                }
            }
            return schemas;
        } catch (Exception e) {
            LOGGER.debug("Exception during parsing xls request :" + e.getMessage(), e);
            throw new TalendRuntimeException(CommonErrorCodes.UNEXPECTED_EXCEPTION, e);
        }
    }

    /**
     * Return the columns metadata for the given sheet.
     *
     * @param sheet the sheet to look at.
     * @return the columns metadata for the given sheet.
     */
    private List<SheetContent.ColumnMetadata> parsePerSheet(Sheet sheet, FormulaEvaluator formulaEvaluator) {

        LOGGER.debug("parsing sheet '{}'", sheet.getSheetName());

        // Map<ColId, Map<RowId, type>>
        SortedMap<Integer, SortedMap<Integer, String>> cellsTypeMatrix = collectSheetTypeMatrix(sheet, formulaEvaluator);
        int averageHeaderSize = guessHeaderSize(cellsTypeMatrix);

        // here we have information regarding types for each rows/col (yup a Matrix!! :-) )
        // so we can analyse and guess metadata (column type, header value)
        final List<SheetContent.ColumnMetadata> columnsMetadata = new ArrayList<>(cellsTypeMatrix.size());

        cellsTypeMatrix.forEach((colId, typePerRowMap) -> {

            Type type = guessColumnType(colId, typePerRowMap, averageHeaderSize);

            String headerText = null;
            if (averageHeaderSize == 1 && sheet.getRow(0) != null) {
                // so header value is the first row of the column
                Cell headerCell = sheet.getRow(0).getCell(colId);
                headerText = XlsUtils.getCellValueAsString(headerCell, formulaEvaluator);
            }

            // header text cannot be null so use a default one
            if (StringUtils.isEmpty(headerText)) {
                // +1 because it starts from 0
                headerText = "COL" + colId++;
            }

            // FIXME what do we do if header size is > 1 concat all lines?
            columnsMetadata.add(SheetContent.ColumnMetadata.Builder //
                    .column() //
                    .id(colId) //
                    .headerSize(averageHeaderSize) //
                    .name(headerText) //
                    .build());

        });

        return columnsMetadata;
    }

    /**
     *
     *
     * @param colId the column id.
     * @param columnRows all rows with previously guessed type: key=row number, value= guessed type
     * @param averageHeaderSize
     * @return
     */
    private Type guessColumnType(Integer colId, SortedMap<Integer, String> columnRows, int averageHeaderSize) {

        // calculate number per type

        Map<String, Long> perTypeNumber = columnRows.tailMap(averageHeaderSize).values() //
                .stream() //
                .collect(Collectors.groupingBy(w -> w, Collectors.counting()));

        OptionalLong maxOccurrence = perTypeNumber.values().stream().mapToLong(Long::longValue).max();

        if (!maxOccurrence.isPresent()) {
            return Type.ANY;
        }

        List<String> duplicatedMax = new ArrayList<>();

        perTypeNumber.forEach((type1, aLong) -> {
            if (aLong >= maxOccurrence.getAsLong()) {
                duplicatedMax.add(type1);
            }
        });

        String guessedType;
        if (duplicatedMax.size() == 1) {
            guessedType = duplicatedMax.get(0);
        } else {
            // as we have more than one type we guess ANY
            guessedType = Type.ANY.getName();
        }

        LOGGER.debug("guessed type for column #{} is {}", colId, guessedType);
        return Type.get(guessedType);
    }

    /**
     * We store (cell types per row) per column.
     *
     * @param sheet key is the column number, value is a Map with key row number and value Type
     * @return A Map&lt;colId, Map&lt;rowId, type&gt;&gt;
     */
    private SortedMap<Integer, SortedMap<Integer, String>> collectSheetTypeMatrix(Sheet sheet,
            FormulaEvaluator formulaEvaluator) {

        int firstRowNum = sheet.getFirstRowNum();
        int lastRowNum = sheet.getLastRowNum();

        LOGGER.debug("firstRowNum: {}, lastRowNum: {}", firstRowNum, lastRowNum);

        SortedMap<Integer, SortedMap<Integer, String>> cellsTypeMatrix = new TreeMap<>();

        // we start analysing rows
        for (int rowCounter = firstRowNum; rowCounter <= lastRowNum; rowCounter++) {

            int cellCounter = 0;

            Row row = sheet.getRow(rowCounter);
            if (row == null) {
                continue;
            }

            Iterator<Cell> cellIterator = row.cellIterator();

            String currentType;

            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();

                int xlsType = Cell.CELL_TYPE_STRING;

                try {
                    xlsType = cell.getCellType() == Cell.CELL_TYPE_FORMULA ? //
                            formulaEvaluator.evaluate(cell).getCellType() : cell.getCellType();
                } catch (Exception e) {
                    // ignore formula error evaluation get as a String with the formula
                }
                switch (xlsType) {
                case Cell.CELL_TYPE_BOOLEAN:
                    currentType = Type.BOOLEAN.getName();
                    break;
                case Cell.CELL_TYPE_NUMERIC:
                    currentType = getTypeFromNumericCell(cell);
                    break;
                case Cell.CELL_TYPE_BLANK:
                    currentType = BLANK;
                    break;
                case Cell.CELL_TYPE_FORMULA:
                case Cell.CELL_TYPE_STRING:
                    currentType = Type.STRING.getName();
                    break;
                case Cell.CELL_TYPE_ERROR:
                    // we cannot really do anything with an error
                default:
                    currentType = Type.ANY.getName();
                }

                SortedMap<Integer, String> cellInfo = cellsTypeMatrix.get(cellCounter);

                if (cellInfo == null) {
                    cellInfo = new TreeMap<>();
                }
                cellInfo.put(rowCounter, currentType);

                cellsTypeMatrix.put(cellCounter, cellInfo);
                cellCounter++;
            }
        }

        LOGGER.trace("cellsTypeMatrix: {}", cellsTypeMatrix);
        return cellsTypeMatrix;
    }

    private String getTypeFromNumericCell(Cell cell) {
        try {
            return HSSFDateUtil.isCellDateFormatted(cell) ? Type.DATE.getName() : Type.NUMERIC.getName();
        } catch (IllegalStateException e) {
            return Type.ANY.getName();
        }
    }

    /**
     * <p>
     * As we can try to be smart and user friendly and not those nerd devs who doesn't mind about users so we try to
     * guess the header size (we assume those bloody users don't have complicated headers!!)
     * </p>
     * <p>
     * We scan all entries to find a common header size value (i.e row line with value type change) more simple all
     * columns/lines with type String
     * </p>
     *
     * @param cellsTypeMatrix key: column number value: row where the type change from String to something else
     * @return The guessed header size.
     */
    private int guessHeaderSize(Map<Integer, SortedMap<Integer, String>> cellsTypeMatrix) {
        SortedMap<Integer, Integer> cellTypeChange = new TreeMap<>();

        cellsTypeMatrix.forEach((colId, typePerRow) -> {

            String firstType = null;
            int rowChange = 0;

            for (Map.Entry<Integer, String> typePerRowEntry : typePerRow.entrySet()) {
                if (firstType == null) {
                    firstType = typePerRowEntry.getValue();
                } else {
                    if (!typePerRowEntry.getValue().equals(firstType) && !typePerRowEntry.getValue().equals(Type.STRING.getName())) {
                        rowChange = typePerRowEntry.getKey();
                        break;
                    }
                }
            }

            cellTypeChange.put(colId, rowChange);
        });

        // FIXME think more about header size calculation
        // currently can fail so force an header of size 1
        int averageHeaderSize = 1;
        LOGGER.debug("averageHeaderSize (forced to): {}, cellTypeChange: {}", averageHeaderSize, cellTypeChange);
        return averageHeaderSize;
    }

    public int getMaxNumberOfColumns() {
        return maxNumberOfColumns;
    }

    public void setMaxNumberOfColumns(int maxNumberOfColumns) {
        this.maxNumberOfColumns = maxNumberOfColumns;
    }
}
