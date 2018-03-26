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

import java.io.InputStream;
import java.util.*;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.talend.dataprep.api.dataset.Schema;
import org.talend.dataprep.schema.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class HtmlSchemaParserTest extends AbstractSchemaTestUtils {

    private HtmlSchemaParser parser = new HtmlSchemaParser();

    @Test
    public void read_html_TDP_1136() throws Exception {

        try (InputStream inputStream = this.getClass().getResourceAsStream("sales-force.xls")) {
            // We do know the format and therefore we go directly to the HTML schema guessing
            MetadataBasedFormatAnalysisRequest request = getRequest(inputStream, "#1");
            request.getMetadata().setEncoding("UTF-16");

            Schema result = toSchema(parser.parse(request));
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.getSheetContents()).isNotNull().isNotEmpty().hasSize(1);
            List<SheetContent.ColumnMetadata> columnMetadatas = result.metadata();
            Assertions.assertThat(columnMetadatas).isNotNull().isNotEmpty().hasSize(7);

            Assertions.assertThat(columnMetadatas.get(0)) //
                    .isEqualToComparingOnlyGivenFields(
                            SheetContent.ColumnMetadata.Builder.column() //
                                    .id(0).name("UID").build(), //
                            "id", "name");

            Assertions.assertThat(columnMetadatas.get(1)) //
                    .isEqualToComparingOnlyGivenFields(
                            SheetContent.ColumnMetadata.Builder.column() //
                                    .id(1).name("Team Member: Name").build(), //
                            "id", "name");

            Assertions.assertThat(columnMetadatas.get(2)) //
                    .isEqualToComparingOnlyGivenFields(
                            SheetContent.ColumnMetadata.Builder.column() //
                                    .id(2).name("Country").build(), //
                            "id", "name");
        }
    }

}
