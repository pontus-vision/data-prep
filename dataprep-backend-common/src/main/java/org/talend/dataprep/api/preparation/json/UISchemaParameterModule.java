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

package org.talend.dataprep.api.preparation.json;

import java.io.IOException;

import org.talend.dataprep.api.preparation.MixedContentMap;
import org.talend.dataprep.parameters.UISchemaParameter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Jackson module that deals with MixedContentMap.
 *
 * @see MixedContentMap
 */
public class UISchemaParameterModule extends SimpleModule {

    /**
     * Default empty constructor.
     */
    public UISchemaParameterModule() {
        super(UISchemaParameterModule.class.getName(), new Version(1, 0, 0, null, null, null));
        addSerializer(UISchemaParameter.class, new Serializer());
        addDeserializer(UISchemaParameter.class, new Deserializer());
    }

    /**
     * Serialize MixedContentMap to json.
     */
    private class Serializer extends JsonSerializer<UISchemaParameter> {

        /**
         * @see JsonSerializer#serialize(Object, JsonGenerator, SerializerProvider)
         */
        @Override
        public void serialize(UISchemaParameter parameter, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
                throws IOException {
            jsonGenerator.writeRawValue(parameter.getUISchema());
        }
    }

    /**
     * Deserialize MixedContentMap to MixedContentMap.
     */
    private class Deserializer extends JsonDeserializer<UISchemaParameter> {

        /**
         * @see JsonDeserializer#deserialize(JsonParser, DeserializationContext)
         */
        @Override
        public UISchemaParameter deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
            return new UISchemaParameter("");
        }
    }
}
