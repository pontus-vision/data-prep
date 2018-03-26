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

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.talend.dataprep.schema.DeSerializer;
import org.talend.dataprep.schema.Format;
import org.talend.dataprep.schema.SheetContent;
import org.talend.dataprep.schema.SheetContent.ColumnMetadata;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HtmlSerializer implements DeSerializer {

    @Override
    public RecordReader deserialize(InputStream rawContent, Format format, SheetContent content) {
        return new HtmlRecordReader(rawContent, content);
    }

    private static class HtmlRecordReader implements RecordReader {

        private String[] columnsIds;

        private Iterator<List<String>> values;

        private boolean closed;

        private HtmlRecordReader(InputStream rawContent, SheetContent content) {
            closed = false;
            List<ColumnMetadata> columns = content.getColumnMetadatas();
            columnsIds = content.getColumnMetadatas().stream().map(ColumnMetadata::idAsApiColumnId).toArray(String[]::new);

            SimpleValuesContentHandler valuesContentHandler = new SimpleValuesContentHandler(columns.size());

            HtmlParser htmlParser = new HtmlParser();

            try {
                htmlParser.parse(rawContent, valuesContentHandler, new Metadata(), new ParseContext());
                values = valuesContentHandler.getValues().iterator();
            } catch (IOException | SAXException | TikaException e) {
                e.printStackTrace();
            }
        }

        @Override
        public Record read() {
            Record record;
            if (closed) {
                record = null;
            } else if (!values.hasNext()) {
                close();
                record = null;
            } else {
                int idx = 0;
                Map<String, String> result = new HashMap<>();
                List<String> rowValues = values.next();
                for (String value : rowValues) {
                    if (idx < columnsIds.length) {
                        result.put(columnsIds[idx], value);
                    }
                    idx++;
                }
                record = new Record(result);
            }
            return record;
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
