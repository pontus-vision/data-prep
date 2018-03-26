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

import org.apache.tika.mime.MediaType;
import org.talend.dataprep.schema.SheetContent.ColumnMetadata;

/**
 * Represents a Format family (CSV, XLS, HTML) for a data set content format. Is a {@link MediaType}.
 */
public interface FormatFamily {

    /**
     * @return The MIME type of the format guess.
     */
    MediaType getMediaType();

    /**
     * @return {@link SchemaParser} that allowed data prep to read {@link ColumnMetadata column metadata} information
     * from the data set.
     */
    SchemaParser getSchemaGuesser();

    /**
     * The object able to read the content of a format.
     */
    DeSerializer getDeSerializer();

    /**
     *
     * @return the Spring beanId to be used to get the bean from the used injection container
     * @deprecated We should never us Spring specific bean id. Use the media type) which is a natural identifier.
     * @see #getMediaType()
     */
    @Deprecated
    String getBeanId();

}
