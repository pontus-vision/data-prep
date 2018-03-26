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

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.Validate;
import org.apache.tika.io.TemporaryResources;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.talend.dataprep.schema.AbstractSchemaTestUtils;
import org.talend.dataprep.schema.Detector;
import org.talend.dataprep.schema.Format;

public class CsvDetectorTest extends AbstractSchemaTestUtils {

    private CsvDetector csvDetector = new CsvDetector();

    /**
     * Reads an input stream and detects its format. It creates a new metadata TIKA {@link Metadata} object.
     *
     * Note that the the specified input stream will be closed before returning.
     *
     * @param detector
     * @param inputStream the specified input stream
     * @return either null or the detected format
     * @throws IOException In case of input stream related errors.
     */
    public static Format detect(Detector detector, InputStream inputStream) throws IOException {
        Validate.isTrue(inputStream != null, "The specified input stream for a format detection must not be null!");
        // all the depending resources will be closed
        try (TemporaryResources tmp = new TemporaryResources()) {
            TikaInputStream tis = TikaInputStream.get(inputStream, tmp);
            return detector.detect(new Metadata(), tis);
        }
    }

    /**
     * Standard csv file.
     */
    @Test
    public void should_detect_CSV_format_and_encoding() throws IOException {
        try (InputStream inputStream = this.getClass().getResourceAsStream("standard.csv")) {
            Format actual = detect(csvDetector, inputStream);

            Assert.assertNotNull(actual);
            assertTrue(actual.getFormatFamily() instanceof CsvFormatFamily);
            assertEquals(ISO_8859_1, actual.getEncoding());
        }
    }

    /**
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void should_not_read_null_input_stream() throws Exception {
        detect(csvDetector, null);
    }

}
