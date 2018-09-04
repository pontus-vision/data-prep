package org.talend.dataprep.transformation.service.export;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.talend.dataprep.api.dataset.DataSet;
import org.talend.dataprep.api.export.ExportParameters;
import org.talend.dataprep.api.export.ExportParametersUtil;
import org.talend.dataprep.api.preparation.PreparationDTO;
import org.talend.dataprep.cache.CacheKeyGenerator;
import org.talend.dataprep.cache.ContentCache;
import org.talend.dataprep.cache.TransformationCacheKey;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.format.export.ExportFormat;
import org.talend.dataprep.transformation.api.transformer.configuration.Configuration;
import org.talend.dataprep.transformation.format.CSVFormat;
import org.talend.dataprep.transformation.service.ExportUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;
import static org.talend.dataprep.transformation.format.JsonFormat.JSON;

@Component
public class MasterSampleExportStrategy extends BaseSampleExportStrategy implements SampleExportStrategy {

    private static final Logger LOGGER = getLogger(MasterSampleExportStrategy.class);

    @Autowired
    private CacheKeyGenerator cacheKeyGenerator;

    @Autowired
    private ExportParametersUtil exportParametersUtil;

    @Override
    public StreamingResponseBody execute(ExportParameters parameters) {
        ExportFormat format = getFormat(parameters.getExportType());
        ExportUtils.setExportHeaders(parameters.getExportName(), //
                parameters.getArguments().get(ExportFormat.PREFIX + CSVFormat.ParametersCSV.ENCODING), //
                format);

        InternalExportParameters internal = fromParams(parameters);

        final TransformationCacheKey key = getCacheKey(parameters);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Transformation Cache Key : {}", key.getKey());
            LOGGER.debug("Cache key details: {}", key);
        }

        return outputStream -> {
            if (contentCache.has(key)) { // we read from cache !
                LOGGER.error("Reading from cache {}", key);
                try (InputStream cachedContent = contentCache.get(key)) {
                    IOUtils.copy(cachedContent, outputStream);
                }
            } else { // Or we just do the export and cache it
                LOGGER.error("Executing the prep and putting in cache {}", key);
                try (final TeeOutputStream tee = new TeeOutputStream(outputStream,
                        contentCache.put(key, ContentCache.TimeToLive.DEFAULT))) {
                    doExport(internal, format, tee);
                }
            }
        };
    }

    private void doExport(InternalExportParameters parameters, ExportFormat format, OutputStream outputStream) {
        // get the dataset content (in an auto-closable block to make sure it is properly closed)
        final String datasetId = parameters.getDatasetId();
        try (DataSet dataSet = datasetClient.getDataSet(datasetId, parameters.getFrom() == ExportParameters.SourceType.FILTER, true)) {
            // get the actions to apply (no preparation ==> dataset export ==> no actions)

            Configuration.Builder builder = Configuration.builder() //
                    .args(parameters.getArguments()) //
                    .outFilter(rm -> filterService.build(parameters.getFilter(), rm)) //
                    .sourceType(parameters.getFrom()).format(format.getName()) //
                    .limit(this.limit);

            if (parameters.getPreparation() != null) {
                final PreparationDTO preparation = parameters.getPreparation();
                builder.preparation(preparation);
                String stepId = parameters.getStepId() == null ? preparation.getHeadId() : parameters.getStepId();
                builder.stepId(parameters.getStepId());
                builder.actions(getActions(parameters.getPreparationId(), stepId));
            }

            if (parameters.getContent() == null) {
                builder.volume(Configuration.Volume.LARGE);
                // TODO : a bit more
            } else {
                builder.volume(Configuration.Volume.SMALL);
            }

            // no need for statistics if it's not JSON output
            if (!Objects.equals(format.getName(), JSON)) {
                builder.globalStatistics(false);
            }

            // output style
            builder.format(format.getName()).args(parameters.getArguments()).output(outputStream);

            factory.get(builder.build()).buildExecutable(dataSet, builder.build()).execute();
        }
    }

    private InternalExportParameters fromParams(ExportParameters parameters) {
        InternalExportParameters internal = new InternalExportParameters();
        internal.setFormat(getFormat(parameters.getExportType()));
        internal.setExportType(getNonBlankOrNull(parameters.getExportType()));
        String datasetId = parameters.getDatasetId();
        internal.setDatasetId(getNonBlankOrNull(datasetId));
        internal.setPreparationId(getNonBlankOrNull(parameters.getPreparationId()));
        internal.setFrom(parameters.getFrom());
        internal.setContent(parameters.getContent());
        internal.setExportName(getNonBlankOrNull(parameters.getExportName()));
        internal.setFilter(getNonBlankOrNull(parameters.getFilter()));
        internal.setArguments(parameters.getArguments());

        if (StringUtils.isNotBlank(parameters.getPreparationId())) {
            PreparationDTO preparation = getPreparation(parameters.getPreparationId());
            internal.setPreparation(preparation);
            if (internal.getDatasetId() == null) {
                internal.setDatasetId(preparation.getDataSetId());
            }
            // Normalize stepId (replace "head" with a real step ID)
            if (StringUtils.isNotBlank(parameters.getStepId())) {
                String stepId = StringUtils.isBlank(parameters.getStepId()) ? preparation.getHeadId() : parameters.getStepId();
                String version = getCleanStepId(preparation, stepId);
                internal.setStepId(version);
            }
        }
        return internal;
    }

    private String getNonBlankOrNull(String datasetId) {
        return StringUtils.isBlank(datasetId) ? null : datasetId;
    }

    @Override
    public boolean test(ExportParameters exportParameters) {
        return true;
//        return isCached(exportParameters)
//                || isDataset(exportParameters)
//                || isPrepExport(exportParameters);
    }

    public boolean isPrepExport(ExportParameters parameters) {
        // Valid if both data set and preparation are set.
        return parameters.getContent() == null //
                && StringUtils.isNotEmpty(parameters.getDatasetId()) //
                && StringUtils.isNotEmpty(parameters.getPreparationId());
    }

    private boolean isDataset(ExportParameters parameters) {
        return parameters.getContent() == null //
                && StringUtils.isNotEmpty(parameters.getDatasetId()) //
                && StringUtils.isEmpty(parameters.getPreparationId());
    }

    private boolean isCached(ExportParameters parameters) {
        try {
            return parameters.getContent() == null //
                    && StringUtils.isNotEmpty(parameters.getPreparationId())
                    && StringUtils.isEmpty(parameters.getDatasetId())
                    && contentCache.has(getCacheKey(parameters));
        } catch (TDPException e) {
            LOGGER.debug("Unable to use cached export strategy.", e);
            return false;
        }
    }

    // WARN: must follow the creation process of PreparationCacheCondition
    private TransformationCacheKey getCacheKey(ExportParameters parameters) {
        return cacheKeyGenerator.generateContentKey(exportParametersUtil.populateFromPreparationExportParameter(parameters));
    }

}
