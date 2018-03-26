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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.talend.daikon.exception.TalendRuntimeException;
import org.talend.daikon.exception.error.CommonErrorCodes;

import static org.talend.dataprep.schema.csv.CsvFormatFamily.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for CSV format handling.
 */
public class CsvFormatUtils {

    /**
     * Mapper to store objects in parameters map.
     */
    private ObjectMapper mapper = new ObjectMapper();

    /** The default enclosure character. */
    private String defaultTextEnclosure;

    /** The default escape character. */
    private String defaultEscapeChar;

    public CsvFormatUtils() {
    }

    public CsvFormatUtils(String defaultTextEnclosure, String defaultEscapeChar) {
        this.defaultTextEnclosure = defaultTextEnclosure;
        this.defaultEscapeChar = defaultEscapeChar;
    }

    /**
     * @param parameters the dataset format parameters.
     * @return the list of the dataset header or an empty list of an error occurs while computing the headers.
     */
    List<String> retrieveHeader(Map<String, String> parameters) {
        List<String> header;
        try {
            String jsonMap = parameters.get(HEADER_COLUMNS_PARAMETER);
            header = Arrays.asList(mapper.readValue(jsonMap, String[].class));
        } catch (Exception e) { // NOSONAR no need to log or throw the exception here
            return Collections.emptyList();
        }
        return header;
    }

    /**
     * Retrieve properties associated with a dataset content and put them in a
     * map with their corresponding key.
     *
     * @param separator the specified separator
     */
    Map<String, String> compileParameterProperties(Separator separator) {
        Map<String, String> updatedParameters = new HashMap<>();
        List<String> header = separator.getHeaders();
        // header
        String jsonHeader;
        try {
            jsonHeader = mapper.writeValueAsString(header);
        } catch (Exception e) {
            throw new TalendRuntimeException(CommonErrorCodes.UNABLE_TO_SERIALIZE_TO_JSON, e);
        }
        updatedParameters.put(HEADER_COLUMNS_PARAMETER, jsonHeader);

        // separator
        updatedParameters.put(SEPARATOR_PARAMETER, String.valueOf(separator.getSeparator()));

        // if no parameter set set take the default one
        updatedParameters.putIfAbsent(TEXT_ENCLOSURE_CHAR, defaultTextEnclosure);
        updatedParameters.putIfAbsent(ESCAPE_CHAR, defaultEscapeChar);

        return updatedParameters;
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
