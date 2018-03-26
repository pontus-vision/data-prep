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

import org.apache.tika.mime.MediaType;
import org.talend.dataprep.schema.DeSerializer;
import org.talend.dataprep.schema.FormatFamily;
import org.talend.dataprep.schema.SchemaParser;

@SuppressWarnings("InsufficientBranchCoverage")
public class CsvFormatFamily implements FormatFamily {

    /** Name of the separator parameter. */
    public static final String SEPARATOR_PARAMETER = "SEPARATOR";

    /** Name of the text enclosure parameter. */
    public static final String TEXT_ENCLOSURE_CHAR = "TEXT_ENCLOSURE_CHAR";

    /** Name of the escape character parameter. */
    public static final String ESCAPE_CHAR = "ESCAPE_CHAR";

    /** The parameter used to set and retrieve header information. */
    public static final String HEADER_COLUMNS_PARAMETER = "COLUMN_HEADERS";

    /** The parameter used to set and retrieve the number of lines spanned by the header. */
    public static final String HEADER_NB_LINES_PARAMETER = "HEADER_NB_LINES";

    public static final MediaType MEDIA_TYPE = MediaType.text("csv");

    public static final String BEAN_ID = "formatGuess#csv";

    private CsvSchemaParser schemaGuesser = new CsvSchemaParser();

    private CsvSerializer serializer = new CsvSerializer();

    /**
     * CSV
     */
    public CsvFormatFamily() {
        // empty constructor needed for the json de/serialization
    }

    @Override
    public MediaType getMediaType() {
        return MEDIA_TYPE; // $NON-NLS-1$
    }

    @Override
    public SchemaParser getSchemaGuesser() {
        return this.schemaGuesser;
    }

    @Override
    public DeSerializer getDeSerializer() {
        return this.serializer;
    }

    @Override
    public String getBeanId() {
        return BEAN_ID;
    }

    public String getDefaultTextEnclosure() {
        return schemaGuesser.getDefaultTextEnclosure();
    }

    public void setDefaultTextEnclosure(String defaultTextEnclosure) {
        schemaGuesser.setDefaultTextEnclosure(defaultTextEnclosure);
        serializer.setDefaultTextEnclosure(defaultTextEnclosure);
    }

    public String getDefaultEscapeChar() {
        return schemaGuesser.getDefaultEscapeChar();
    }

    public void setDefaultEscapeChar(String defaultEscapeChar) {
        schemaGuesser.setDefaultEscapeChar(defaultEscapeChar);
        serializer.setDefaultEscapeChar(defaultEscapeChar);
    }
}
