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

import static org.junit.Assert.assertEquals;
import static org.talend.dataprep.schema.csv.CsvFormatFamily.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Unit test for CSVFormatUtils.
 *
 * @see CsvFormatUtils
 */
public class CsvFormatUtilsTest {

    private CsvFormatUtils csvFormatUtils = new CsvFormatUtils();

    @Test
    public void should_compile_parameters() {
        // given
        csvFormatUtils.setDefaultTextEnclosure("\"");
        csvFormatUtils.setDefaultEscapeChar("\u0000");

        Map<String, String> entryParameters = initMapParam();

        final Separator separator = new Separator("|".charAt(0));

        // when
        entryParameters.putAll(csvFormatUtils.compileParameterProperties(separator));

        // then
        assertEquals(5, entryParameters.size());
        assertEquals("\u0000", entryParameters.get(ESCAPE_CHAR));
        assertEquals("\"", entryParameters.get(TEXT_ENCLOSURE_CHAR));
        assertEquals("|", entryParameters.get(SEPARATOR_PARAMETER));
        assertEquals("12", entryParameters.get(HEADER_NB_LINES_PARAMETER));
        assertEquals("[]", entryParameters.get(HEADER_COLUMNS_PARAMETER));
    }

    private Map<String, String> initMapParam() {
        final Map<String, String> entryParameters = new HashMap<>();
        entryParameters.put(HEADER_NB_LINES_PARAMETER, "12");
        entryParameters.put(HEADER_COLUMNS_PARAMETER,
                "[\"nickname|secret\",\"firstname|secret\",\"lastname|date\",\"of\",\"birth|city\"]");
        entryParameters.put(SEPARATOR_PARAMETER, ",");
        return entryParameters;
    }

}
