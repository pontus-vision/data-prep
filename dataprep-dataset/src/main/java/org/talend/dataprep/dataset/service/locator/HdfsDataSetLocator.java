package org.talend.dataprep.dataset.service.locator;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.dataset.DataSetLocation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Dataset locator for remote hdfs datasets.
 */
@Component
public class HdfsDataSetLocator implements DataSetLocator {

    /** DataSet media type for remote hdfs datasets. */
    public static final String MEDIA_TYPE = "application/vnd.remote-ds.hdfs";

    /** Jackson builder. */
    @Autowired
    private Jackson2ObjectMapperBuilder builder;

    /**
     * @see DataSetLocator#accept(String)
     */
    @Override
    public boolean accept(String contentType) {
        return StringUtils.startsWith(contentType, MEDIA_TYPE);
    }

    /**
     * @see DataSetLocator#getLocation(InputStream)
     */
    @Override
    public DataSetLocation getLocation(InputStream connectionParameters) throws IOException {
        ObjectMapper mapper = builder.build();
        JsonParser parser = mapper.getFactory().createParser(connectionParameters);
        return mapper.readerFor(DataSetLocation.class).readValue(parser);
    }

}