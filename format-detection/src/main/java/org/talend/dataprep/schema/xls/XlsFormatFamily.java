//  ============================================================================
//
//  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
//
//  This source code is available under agreement available at
//  https://github.com/Talend/data-prep/blob/master/LICENSE
//
//  You should have received a copy of the agreement
//  along with this program; if not, write to Talend SA
//  9 rue Pages 92150 Suresnes, France
//
//  ============================================================================

package org.talend.dataprep.schema.xls;

import org.apache.tika.mime.MediaType;
import org.talend.dataprep.schema.FormatFamily;

public class XlsFormatFamily implements FormatFamily {

    /**
     * The media type returned for XLS format
     */
    public static final MediaType MEDIA_TYPE = MediaType.application("vnd.ms-excel");

    /**
     * the bean identifier for the XLS format family
     */
    public static final String BEAN_ID = "formatGuess#xls";

    public static final String HEADER_NB_LINES_PARAMETER = "HEADER_NB_LINES";

    private XlsSchemaParser schemaParser = new XlsSchemaParser();

    private XlsSerializer serializer = new XlsSerializer();

    @Override
    public MediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public XlsSchemaParser getSchemaGuesser() {
        return schemaParser;
    }

    @Override
    public XlsSerializer getDeSerializer() {
        return this.serializer;
    }


    @Override
    public String getBeanId() {
        return BEAN_ID;
    }
}
