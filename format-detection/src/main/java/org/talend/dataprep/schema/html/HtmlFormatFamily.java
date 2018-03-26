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

import org.apache.tika.mime.MediaType;
import org.talend.dataprep.schema.DeSerializer;
import org.talend.dataprep.schema.FormatFamily;
import org.talend.dataprep.schema.SchemaParser;

public class HtmlFormatFamily implements FormatFamily {

    // Html content is not Excel, but currently only HTML content wrapped in Excel is supported, thus this MIME.
    public static final MediaType MEDIA_TYPE = MediaType.application("vnd.ms-excel");

    public static final String BEAN_ID = "formatGuess#html";

    private HtmlSchemaParser schemaParser = new HtmlSchemaParser();

    private HtmlSerializer htmlSerializer = new HtmlSerializer();

    @Override
    public MediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public SchemaParser getSchemaGuesser() {
        return schemaParser;
    }

    @Override
    public DeSerializer getDeSerializer() {
        return this.htmlSerializer;
    }

    @Override
    public String getBeanId() {
        return BEAN_ID;
    }
}
