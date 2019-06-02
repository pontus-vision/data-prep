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

package org.talend.dataprep.schema.xls.serialization;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.DataSetMetadata;
import org.talend.dataprep.schema.xls.XlsSerializer;
import org.talend.dataprep.schema.xls.streaming.StreamingReader;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Serialize XLSX file using stream parsing.
 */
public class XlsxStreamRunnable implements Runnable {

    /** This class' logger. */
    private static final Logger LOG = getLogger(XlsxStreamRunnable.class);

    /** Where to serialize the json. */
    private final OutputStream jsonOutput;

    /** The xlsx raw input. */
    private final InputStream rawContent;

    /** The dataset metadata to serialize. */
    private final DataSetMetadata metadata;

    private final long limit;

    /** The jackson factory to use for the serialization. */
    private final JsonFactory jsonFactory;

    /**
     * Constructor.
     *  @param jsonOutput Where to serialize the json.
     * @param rawContent The xlsx raw input.
     * @param metadata The dataset metadata to serialize.
     * @param limit A limit to indicate to serializer when to stop. Use -1 for "no limit".
     * @param factory The jackson factory to use for the serialization.
     */
    public XlsxStreamRunnable(OutputStream jsonOutput, InputStream rawContent, DataSetMetadata metadata, long limit, JsonFactory factory) {
        this.jsonOutput = jsonOutput;
        this.rawContent = rawContent;
        this.metadata = metadata;
        this.limit = limit;
        this.jsonFactory = factory;
    }

    /**
     * @see Runnable#run()
     */
    @Override
    public void run() {
        try {
            JsonGenerator generator = jsonFactory.createGenerator(jsonOutput);
            Workbook workbook = StreamingReader.builder() //
                    .bufferSize(4096) //
                    .rowCacheSize(1) //
                    .open(rawContent);
            try {
                Sheet sheet = StringUtils.isEmpty(metadata.getSheetName()) ? //
                        workbook.getSheetAt(0) : workbook.getSheet(metadata.getSheetName());
                generator.writeStartArray();
                for (Row row : sheet) {
                    if (limit > 0 && row.getRowNum() > limit) {
                        break;
                    }
                    if (!XlsSerializer.isHeaderLine(row.getRowNum(), metadata.getRowMetadata().getColumns())) {
                        generator.writeStartObject();
                        // data quality Analyzer doesn't like to not have all columns even if we don't have any values
                        // so create so field with empty value otherwise we get exceptions
                        int i = 0;
                        for (ColumnMetadata columnMetadata : metadata.getRowMetadata().getColumns()) {
                            Cell cell = row.getCell(i);
                            String cellValue = cell == null ? null : cell.getStringCellValue(); // StringUtils.EMPTY
                            generator.writeFieldName(columnMetadata.getId());
                            if (cellValue != null) {
                                generator.writeString(cellValue);
                            } else {
                                generator.writeNull();
                            }
                            i++;
                        }
                        generator.writeEndObject();
                    }
                }
                generator.writeEndArray();
                generator.flush();
            } finally {
                workbook.close();
            }
        } catch (Exception e) {
            // Consumer may very well interrupt consumption of stream (in case of limit(n) use for sampling).
            // This is not an issue as consumer is allowed to partially consumes results, it's up to the
            // consumer to ensure data it consumed is consistent.
            LOG.debug("Unable to continue serialization for {}. Skipping remaining content.", metadata.getId(), e);
        } finally {
            try {
                jsonOutput.close();
            } catch (IOException e) {
                LOG.error("Unable to close output", e);
            }
        }
    }

}
