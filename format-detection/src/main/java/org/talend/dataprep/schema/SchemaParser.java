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

package org.talend.dataprep.schema;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.talend.dataprep.schema.SheetContent.ColumnMetadata;

/**
 * Represents a class that is able to parse a data set content, possibly guess or update it schema, and returns a list of
 * {@link ColumnMetadata metadata} out of it.
 */
public interface SchemaParser {

    /**
     * <p>
     * Parses the provided content and extract {@link ColumnMetadata column} information. Implementations are encouraged
     * to return as fast as possible from this method (possibly without processing the whole <code>content</code>
     * parameter).
     * </p>
     *
     * @param request container with information needed to parse the raw data
     * @return a list od {@link SheetContent} containing a list of {@link ColumnMetadata metadata}. When no column name/type can be
     * created, implementations are expected to generate names and select
     * {@link org.talend.dataprep.api.type.Type#STRING string} as type.
     */
    List<SheetContent> parse(Request request);

    /**
     * Schema parser request.
     * Must be immutable.
     */
    interface Request {

        /** Raw content to analyse. recommended to be an InputStream that supports {@link InputStream#} */
        InputStream getContent();

        /** Detector parameters as hints. */
        // TODO: should a detector have parameters? This is not a parser.
        Map<String, String> getParameters();

        @Deprecated
        void setParameters(Map<String, String> parameters);

        /** The data stream encoding if available. */
        Charset getEncoding();
    }

}
