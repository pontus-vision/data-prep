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

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.daikon.exception.TalendRuntimeException;
import org.talend.daikon.exception.error.CommonErrorCodes;
import org.talend.dataprep.schema.SchemaParser;
import org.talend.dataprep.schema.SheetContent;

/**
 * This class is in charge of parsing html file to discover schema.
 *
 */
public class HtmlSchemaParser implements SchemaParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlSchemaParser.class);

    @Override
    public List<SheetContent> parse(Request request) {

        try {
            SimpleHeadersContentHandler headersContentHandler = new SimpleHeadersContentHandler();

            InputStream inputStream = request.getContent();
            HtmlParser htmlParser = new HtmlParser();

            Metadata metadata = new Metadata();

            htmlParser.parse(inputStream, headersContentHandler, metadata, new ParseContext());

            List<SheetContent.ColumnMetadata> columns = new ArrayList<>(headersContentHandler.getHeaderValues().size());

            for (String headerValue : headersContentHandler.getHeaderValues()) {
                columns.add(SheetContent.ColumnMetadata.Builder.column() //
                        .name(headerValue) //
                        .id(columns.size()) //
                        .build());
            }

            SheetContent sheetContent = new SheetContent();
            sheetContent.setColumnMetadatas(columns);

            return Collections.singletonList(sheetContent);
        } catch (Exception e) {
            LOGGER.debug("Exception during parsing html request :" + e.getMessage(), e);
            throw new TalendRuntimeException(CommonErrorCodes.UNEXPECTED_EXCEPTION, e);
        }

    }

}
