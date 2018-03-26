package org.talend.dataprep.dataset.service.analysis.synchronous;

import org.slf4j.Logger;
import org.talend.dataprep.api.dataset.DataSetMetadata;
import org.talend.dataprep.schema.SchemaParser;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Schema parser request.
 */
public class MetadataBasedFormatAnalysisRequest implements SchemaParser.Request {

    private static final Logger LOGGER = getLogger(MetadataBasedFormatAnalysisRequest.class);

    private final InputStream content;

    private final DataSetMetadata metadata;

    /**
     * Constructor.
     *
     * @param content  The data set content. It should never be <code>null</code>.
     * @param metadata The data set metadata, to be used to retrieve parameters needed to understand format in
     *                 <code>content</code>.
     */
    public MetadataBasedFormatAnalysisRequest(InputStream content, DataSetMetadata metadata) {
        this.content = content;
        this.metadata = metadata;
    }

    public InputStream getContent() {
        return content;
    }

    /**
     * The underlying dataset metadata.
     */
    public DataSetMetadata getMetadata() {
        return metadata;
    }

    @Override
    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(metadata.getContent().getParameters());
    }

    @Override
    public void setParameters(Map<String, String> parameters) {
        metadata.getContent().setParameters(parameters);
    }

    @Override
    public Charset getEncoding() {
        String encoding = metadata.getEncoding();
        try {
            return Charset.forName(encoding);
        } catch (UnsupportedCharsetException e) {
            LOGGER.warn("Stored encoding is not supported", e);
            return null;
        }
    }
}
