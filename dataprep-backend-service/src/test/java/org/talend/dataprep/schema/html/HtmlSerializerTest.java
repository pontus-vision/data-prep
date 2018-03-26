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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.MapEntry;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.schema.AbstractSchemaTestUtils;
import org.talend.dataprep.schema.MetadataBasedFormatAnalysisRequest;
import org.talend.dataprep.api.dataset.Schema;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.talend.dataprep.schema.MetadataBasedFormatAnalysisRequest.convertToApiColumns;
import static org.talend.dataprep.schema.csv.CsvSerializerTest.serializeToJson;

public class HtmlSerializerTest extends AbstractSchemaTestUtils {

    private final static Logger logger = LoggerFactory.getLogger(HtmlSchemaParserTest.class);

    private HtmlSchemaParser htmlSchemaGuesser = new HtmlSchemaParser();

    private HtmlSerializer htmlSerializer = new HtmlSerializer();

    private HtmlFormatFamily htmlFormatFamily = new HtmlFormatFamily();

    @Test
    public void html_serializer() throws Exception {

        final MetadataBasedFormatAnalysisRequest request;
        final Schema result;
        try (InputStream inputStream = this.getClass().getResourceAsStream("sales-force.xls")) {
            // We do know the format and therefore we go directly to the HTML schema guessing
            request = getRequest(inputStream, "#2");
            request.getMetadata().setEncoding("UTF-16");

            result = toSchema(htmlSchemaGuesser.parse(request));
        }

        try (InputStream inputStream = this.getClass().getResourceAsStream("sales-force.xls")) {

            request.getMetadata().getRowMetadata().setColumns(convertToApiColumns(result.metadata()));

            InputStream jsonStream = serializeToJson(inputStream, request.getMetadata(), htmlSerializer);

            String json = IOUtils.toString(jsonStream, UTF_8);

            logger.debug("json: {}", json);

            ObjectMapper mapper = new ObjectMapper();

            CollectionType collectionType = mapper.getTypeFactory().constructCollectionType(ArrayList.class, TreeMap.class);

            List<Map<String, String>> values = mapper.readValue(json, collectionType);

            logger.debug("values: {}", values);

            Map<String, String> row0 = values.get(0);

            Assertions.assertThat(row0).contains(MapEntry.entry("0000", "000001"), //
                    MapEntry.entry("0001", "Jennifer BOS"), //
                    MapEntry.entry("0002", "France"), //
                    MapEntry.entry("0003", "jbos@talend.com"));
        }
    }

    @Test
    public void html_serializer_with_jira_export() throws Exception {

        final MetadataBasedFormatAnalysisRequest request;
        final Schema result;
        try (InputStream inputStream = this.getClass().getResourceAsStream("jira_export.xls")) {
            // We do know the format and therefore we go directly to the HTML schema guessing
            request = getRequest(inputStream, "#2");
            request.getMetadata().setEncoding("UTF-16");

            result = toSchema(htmlSchemaGuesser.parse(request));
        }

        try (InputStream inputStream = this.getClass().getResourceAsStream("jira_export.xls")) {

            final List<ColumnMetadata> columns = convertToApiColumns(result.metadata());
            Assert.assertThat(columns.size(), is(98));
            request.getMetadata().getRowMetadata().setColumns(columns);

            InputStream jsonStream = serializeToJson(inputStream, request.getMetadata(), htmlSerializer);

            String json = IOUtils.toString(jsonStream, UTF_8);
            ObjectMapper mapper = new ObjectMapper();
            CollectionType collectionType = mapper.getTypeFactory().constructCollectionType(ArrayList.class, TreeMap.class);
            List<Map<String, String>> values = mapper.readValue(json, collectionType);
            Map<String, String> row0 = values.get(0);
            for (String s : row0.keySet()) {
                row0.put(s, row0.get(s).trim());
            }
            Assertions.assertThat(row0).contains(MapEntry.entry("0001", "TDP-1"));
        }
    }

}
