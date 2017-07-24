package org.talend.dataprep.transformation.api.transformer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.dataprep.cache.ContentCache;
import org.talend.dataprep.cache.ContentCacheKey;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.CommonErrorCodes;
import org.talend.dataprep.transformation.actions.Providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class ConfiguredCacheWriter implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfiguredCacheWriter.class);

    private ContentCache.TimeToLive ttl;

    public ConfiguredCacheWriter(final ContentCache.TimeToLive ttl) {
        this.ttl = ttl;
    }

    public void write(final ContentCacheKey key, final Object object) {
        if (object == null) {
            LOGGER.warn("Unable to cache null object at '{}'", key.getKey());
            return;
        }
        try {
            final ObjectMapper mapper = new ObjectMapper();
            final ObjectWriter objectWriter = mapper.writerFor(object.getClass());
            try (final OutputStream output = Providers.get(ContentCache.class).put(key, ttl)) {
                objectWriter.writeValue(output, object);
                LOGGER.debug("New metadata cache entry -> {}.", key.getKey());
            }
        } catch (IOException e) {
            throw new TDPException(CommonErrorCodes.UNEXPECTED_EXCEPTION, e);
        }
    }
}
