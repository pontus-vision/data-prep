// ============================================================================
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.talend.dataprep.command.Defaults.toJson;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiFunction;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DefaultsTest {

    @Test
    public void shouldParseJsonArray() throws Exception {
        // Given
        final BiFunction<HttpRequestBase, HttpResponse, Set<String>> extractStatuses = toJson(new ObjectMapper()).andThen(jsonNode -> {
            final Iterator<JsonNode> elements = jsonNode.elements();
            Set<String> statuses = new HashSet<>();
            while (elements.hasNext()) {
                final JsonNode element = elements.next();
                statuses.add(element.get("status").asText());
            }
            return statuses;
        });
        final BasicHttpResponse response = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), 200, "");
        response.setEntity(new StringEntity("[{\"status\":\"OK\"}, {\"status\":\"OK\"}, {\"status\":\"CANCEL\"}]"));
        final HttpGet request = new HttpGet("http://fake_url");

        // When
        final Set<String> statuses = extractStatuses.apply(request, response);

        // Then
        assertEquals(2, statuses.size());
        assertTrue(statuses.contains("OK"));
        assertTrue(statuses.contains("CANCEL"));
    }
}
