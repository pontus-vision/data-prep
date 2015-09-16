package org.talend.dataprep.api.service.command.dataset;

import static org.talend.dataprep.api.service.command.common.GenericCommand.Defaults.asString;
import static org.talend.dataprep.api.service.command.common.GenericCommand.Defaults.emptyString;

import java.io.InputStream;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.APIErrorCodes;
import org.talend.dataprep.api.service.PreparationAPI;
import org.talend.dataprep.api.service.command.common.GenericCommand;
import org.talend.dataprep.cache.ContentCache;
import org.talend.dataprep.cache.ContentCacheKey;
import org.talend.dataprep.exception.TDPException;

/**
 * Command in charge of mostly updating a dataset content.
 */
@Component
@Scope("request")
public class CreateOrUpdateDataSet extends GenericCommand<String> {

    /** The content cache. */
    @Autowired
    private ContentCache contentCache;

    /**
     * Private constructor.
     *
     * @param client the http client.
     * @param id the dataset id.
     * @param name the dataset name.
     * @param dataSetContent the new dataset content.
     */
    private CreateOrUpdateDataSet(HttpClient client, String id, String name, InputStream dataSetContent) {
        super(PreparationAPI.DATASET_GROUP, client);
        execute(() -> {
            final HttpPut put = new HttpPut(datasetServiceUrl + "/datasets/" + id + "/raw/?name=" + name); //$NON-NLS-1$ //$NON-NLS-2$
            put.setEntity(new InputStreamEntity(dataSetContent));
            return put;
        });
        onError((e) -> new TDPException(APIErrorCodes.UNABLE_TO_CREATE_OR_UPDATE_DATASET, e));
        on(HttpStatus.NO_CONTENT).then(emptyString());
        on(HttpStatus.ACCEPTED).then(emptyString());
        on(HttpStatus.OK).then((req, res) -> {
            contentCache.evict(new ContentCacheKey(id)); // clear the cache (dataset and all its preparations)
            return asString().apply(req, res); // Return response as String
        });
    }
}
