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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.talend.dataprep.schema.AbstractSchemaTestUtils;
import org.talend.dataprep.schema.Format;
import org.talend.dataprep.schema.csv.CsvDetectorTest;

public class XlsDetectorTest extends AbstractSchemaTestUtils {

    /** The format guesser to test. */
    private XlsDetector xlsDetector = new XlsDetector();

    /**
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void should_not_read_null_input_stream() throws Exception {
        CsvDetectorTest.detect(xlsDetector, null);
    }

    @Test
    public void should_detect_old_xls_format() throws Exception {
        try (InputStream inputStream = this.getClass().getResourceAsStream("test.xls")) {
            Format actual = CsvDetectorTest.detect(xlsDetector, inputStream);
            assertTrue(actual.getFormatFamily() instanceof XlsFormatFamily);
            assertTrue(UTF_8.equals(actual.getEncoding()));
        }
    }

    @Test
    public void should_detect_new_xls_format() throws Exception {
        try (InputStream inputStream = this.getClass().getResourceAsStream("test_new.xlsx")) {
            Format actual = CsvDetectorTest.detect(xlsDetector, inputStream);
            assertTrue(actual.getFormatFamily() instanceof XlsFormatFamily);
            assertTrue(UTF_8.equals(actual.getEncoding()));
        }
    }

    @Test
    public void read_xls_that_can_be_parsed_as_csv_TDP_375() throws Exception {

        String fileName = "TDP-375_xsl_read_as_csv.xls";

        try (InputStream inputStream = this.getClass().getResourceAsStream(fileName)) {
            Format actual = CsvDetectorTest.detect(xlsDetector, inputStream);
            Assert.assertNotNull(actual);
            assertTrue(actual.getFormatFamily() instanceof XlsFormatFamily);
            assertEquals(XlsFormatFamily.MEDIA_TYPE, actual.getFormatFamily().getMediaType());
            assertTrue(UTF_8.equals(actual.getEncoding()));
        }

    }

}
