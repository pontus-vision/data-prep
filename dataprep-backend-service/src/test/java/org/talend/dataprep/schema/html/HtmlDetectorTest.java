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

package org.talend.dataprep.schema.html;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.talend.dataprep.api.dataset.DataSetMetadata;
import org.talend.dataprep.schema.AbstractSchemaTestUtils;
import org.talend.dataprep.schema.Format;
import org.talend.dataprep.schema.csv.CsvDetectorTest;

import static java.nio.charset.StandardCharsets.UTF_16;
import static org.junit.Assert.*;

public class HtmlDetectorTest extends AbstractSchemaTestUtils {

    private HtmlDetector htmlDetector = new HtmlDetector();

    @Test
    public void guess_html_format_success() throws Exception {

        String fileName = "sales-force.xls";

        DataSetMetadata datasetMetadata = ioTestUtils.getSimpleDataSetMetadata();

        datasetMetadata.setEncoding("UTF-16");

        Format actual = CsvDetectorTest.detect(htmlDetector, this.getClass().getResourceAsStream(fileName));

        assertTrue(actual.getFormatFamily() instanceof HtmlFormatFamily);
        assertEquals(UTF_16, actual.getEncoding());
    }

    @Test
    public void guess_html_format_fail() throws Exception {

        String fileName = "foo.html";

        DataSetMetadata datasetMetadata = ioTestUtils.getSimpleDataSetMetadata();

        datasetMetadata.setEncoding("UTF-16");

        Format actual = CsvDetectorTest.detect(htmlDetector, this.getClass().getResourceAsStream(fileName));
        assertNull(actual);
    }

}
