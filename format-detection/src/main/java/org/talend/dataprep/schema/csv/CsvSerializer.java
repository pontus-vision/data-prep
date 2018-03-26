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

package org.talend.dataprep.schema.csv;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.daikon.exception.TalendRuntimeException;
import org.talend.daikon.exception.error.CommonErrorCodes;
import org.talend.dataprep.schema.DeSerializer;
import org.talend.dataprep.schema.Format;
import org.talend.dataprep.schema.SheetContent;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

import static org.talend.dataprep.schema.csv.CsvFormatFamily.TEXT_ENCLOSURE_CHAR;

public class CsvSerializer implements DeSerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvSerializer.class);

    /** The default enclosure character. */
    private String defaultTextEnclosure;

    /** The default escape character. */
    private String defaultEscapeChar;

    @Override
    public DeSerializer.RecordReader deserialize(InputStream rawContent, Format format, SheetContent content) {

        final Charset encoding = format.getEncoding();
        final Map<String, String> parameters = content.getParameters();

        final String separator = parameters.get(CsvFormatFamily.SEPARATOR_PARAMETER);
        final char actualSeparator = separator.charAt(0);

        // should always be present since parsed from CsvSchemaParser
        final char textEnclosureChar = getFromParameters(parameters, TEXT_ENCLOSURE_CHAR, defaultTextEnclosure);
        final char escapeChar = getFromParameters(parameters, CsvFormatFamily.ESCAPE_CHAR, defaultEscapeChar);

        final int numberOfHeaderLines = Integer.parseInt(parameters.getOrDefault(CsvFormatFamily.HEADER_NB_LINES_PARAMETER, "1"));

        final String[] columnsIds = content.getColumnMetadatas()
                .stream()
                .map(SheetContent.ColumnMetadata::idAsApiColumnId)
                .toArray(String[]::new);

        try {
            return new CsvRecordReader(rawContent, encoding, actualSeparator, textEnclosureChar, escapeChar,
                    columnsIds, separator, numberOfHeaderLines);
        } catch (IOException e) {
            throw new TalendRuntimeException(CommonErrorCodes.UNABLE_TO_SERIALIZE_TO_JSON, e);
        }
    }

    /**
     * Extract the parameter value from the dataset parameters or return the given default value if not found.
     *
     * @param parameters where to look for the wanted parameter value.
     * @param key the parameter key.
     * @param defaultValue the default value to use if the parameter is not found.
     * @return the parameter value from the dataset parameters or return the given default value if not found.
     */
    private char getFromParameters(Map<String, String> parameters, String key, String defaultValue) {
        final String fromParameters = parameters.get(key);

        // wrong parameter use (empty or more than one character)
        if (fromParameters == null || fromParameters.length() > 1) {
            LOGGER.warn("Parameter \"{}\" not present from CsvSchema detector ! Using {}", key, defaultValue);
            return StringUtils.isEmpty(defaultValue) ? Character.MIN_VALUE : defaultValue.charAt(0);
        }

        return (fromParameters.length() == 0)  ? Character.MIN_VALUE : fromParameters.charAt(0);
    }

    public String getDefaultTextEnclosure() {
        return defaultTextEnclosure;
    }

    public void setDefaultTextEnclosure(String defaultTextEnclosure) {
        this.defaultTextEnclosure = defaultTextEnclosure;
    }

    public String getDefaultEscapeChar() {
        return defaultEscapeChar;
    }

    public void setDefaultEscapeChar(String defaultEscapeChar) {
        this.defaultEscapeChar = defaultEscapeChar;
    }
}
