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

package org.talend.dataprep.schema.xls;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.talend.daikon.exception.TalendRuntimeException;
import org.talend.daikon.exception.error.CommonErrorCodes;
import org.talend.dataprep.schema.DeSerializer;
import org.talend.dataprep.schema.Format;
import org.talend.dataprep.schema.SheetContent;
import org.talend.dataprep.schema.xls.serialization.XlsRecordReader;
import org.talend.dataprep.schema.xls.serialization.XlsxRecordReader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.talend.dataprep.schema.xls.XlsFormatFamily.HEADER_NB_LINES_PARAMETER;

public class XlsSerializer implements DeSerializer {

    @Override
    public RecordReader deserialize(InputStream rawContent, Format format, SheetContent content) {

        Map<String, String> parameters = content.getParameters();
        final int numberOfHeaderLines = Integer.parseInt(
                parameters.getOrDefault(HEADER_NB_LINES_PARAMETER, "1"));

        final String[] columnsIds = new String[content.getColumnMetadatas().size()];
        for (int colId = 0; colId < columnsIds.length; colId++) {
            columnsIds[colId] = StringUtils.leftPad(Integer.toString(colId), 4, "0");
        }
        final String sheetName = content.getName();

        // POI need streams that support mark reset and pushback
        // WARN: this will pull the first 8 bytes from the original stream. We must not read again the original
        if (!rawContent.markSupported()) {
            rawContent = new BufferedInputStream(rawContent, 8);
        }

        try {
            return XlsUtils.isNewExcelFormat(rawContent) ? new XlsxRecordReader(rawContent, columnsIds, sheetName, numberOfHeaderLines) :
                    new XlsRecordReader(rawContent, columnsIds, sheetName, numberOfHeaderLines);
        } catch (IOException | InvalidFormatException  e) {
            throw new TalendRuntimeException(CommonErrorCodes.UNABLE_TO_SERIALIZE_TO_JSON, e);
        }
    }

}
