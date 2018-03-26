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

package org.talend.dataprep.schema;

import java.io.InputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.talend.ServiceBaseTest;
import org.talend.dataprep.api.dataset.DataSetMetadata;
import org.talend.dataprep.api.dataset.Schema;
import org.talend.dataprep.dataset.DataSetMetadataBuilder;

import static org.talend.dataprep.api.dataset.Schema.Builder.parserResult;

/**
 *
 */
public abstract class AbstractSchemaTestUtils extends ServiceBaseTest {

    @Autowired
    protected IoTestUtils ioTestUtils;

    @Autowired
    protected DataSetMetadataBuilder metadataBuilder;

    protected static Schema toSchema(List<SheetContent> contents) {
        return parserResult().sheetContents(contents)
                .draft(contents.size() > 1)
                .sheetName(contents.size() > 0 ? contents.iterator().next().getName() : null)
                .build();
    }

    /**
     * Return the SchemaParser.Request for the given parameters.
     *
     * @param content the dataset con.ent.
     * @param dataSetId the dataset id.
     * @return the SchemaParser.Request for the given parameters.
     */
    protected MetadataBasedFormatAnalysisRequest getRequest(InputStream content, String dataSetId) {
        DataSetMetadata dataSetMetadata = metadataBuilder.metadata().id(dataSetId).build();
        return new MetadataBasedFormatAnalysisRequest(content, dataSetMetadata);
    }

}
