package org.talend.dataprep.transformation.service.export;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.talend.dataprep.api.dataset.DataSet;
import org.talend.dataprep.api.dataset.DataSetMetadata;
import org.talend.dataprep.api.export.ExportParameters;
import org.talend.dataprep.api.export.ExportParametersUtil;
import org.talend.dataprep.api.preparation.PreparationDTO;
import org.talend.dataprep.cache.*;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.TransformationErrorCodes;
import org.talend.dataprep.format.export.ExportFormat;
import org.talend.dataprep.transformation.api.transformer.configuration.Configuration;
import org.talend.dataprep.transformation.format.CSVFormat;
import org.talend.dataprep.transformation.service.ExportUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;
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
                try (final TeeOutputStream tee =
                        new TeeOutputStream(new CloseShieldOutputStream(outputStream), contentCache.put(key, ContentCache.TimeToLive.DEFAULT))) {
                    doExport(internal, format, tee);
                }
            }
        };
    }

    private void doExport(InternalExportParameters parameters, ExportFormat format, OutputStream outputStream) {
        final String datasetId = parameters.getDatasetId();
        final OptimizedPreparationInput optimizedPreparationInput = analyseOptimizationApplicability(parameters);

        // handle source cache. It could be handled just as out cache if out = src
        Configuration.Builder confBuilder = Configuration
                .builder() //
                .args(parameters.getArguments()) //
                .outFilter(rm -> filterService.build(parameters.getFilter(), rm)) //
                .sourceType(parameters.getFrom())
                .format(format.getName()) // why needs input format ?
                .limit(this.limit);

        if (parameters.getPreparation() != null) {
            final PreparationDTO preparation = parameters.getPreparation();
            // TODO : why builder need prep and step ID ? Should only need actions
            confBuilder.preparation(preparation);
            confBuilder.stepId(parameters.getStepId());

            if (optimizedPreparationInput == null) {
                confBuilder.actions(getActions(parameters.getPreparationId(), parameters.getStepId()));
            } else {
                preparation.setSteps(optimizedPreparationInput.getStepsToApply());
            }
        }
        // no need for statistics if it's not JSON output
        if (!Objects.equals(format.getName(), JSON)) {
            confBuilder.globalStatistics(false);
        }

        // output style
        confBuilder.format(format.getName()).args(parameters.getArguments()).output(outputStream);

        if (parameters.getContent() == null) {
            confBuilder.volume(Configuration.Volume.LARGE);
            if (optimizedPreparationInput == null) {
                // get from dataset source
                try (DataSet dataSet = datasetClient.getDataSet(datasetId, parameters.getFrom() == ExportParameters.SourceType.FILTER, true)) {
                    // get the actions to apply (no preparation ==> dataset export ==> no actions)
                    executeWithDataset(dataSet, confBuilder);
                }
            } else {
                // or get cached version
                // And this was really a bad idea to put cache at this level as we may have cached in any format (CSV, JSON...)
                try (InputStreamReader src = new InputStreamReader(
                        contentCache.get(optimizedPreparationInput.getTransformationCacheKey()), UTF_8);
                        DataSet dataSet = mapper.readerFor(DataSet.class).readValue(src)) {
                    dataSet.setMetadata(optimizedPreparationInput.getMetadata());
                    executeWithDataset(dataSet, confBuilder);
                } catch (TDPException e) {
                    throw e;
                } catch (Exception e) {
                    throw new TDPException(TransformationErrorCodes.UNABLE_TO_TRANSFORM_DATASET, e);
                }
            }
        } else {
            confBuilder.volume(Configuration.Volume.SMALL);
            executeWithDataset(parameters.getContent(), confBuilder);
        }
    }

    private void executeWithDataset(DataSet input, Configuration.Builder configBuilder) {
        Configuration configuration = configBuilder.build();
        factory.get(configuration).buildExecutable(input, configuration).execute();
    }

    /**
     * Normalize parameters replacing blanks with null and "head" step by a real id. Also fetch real objects instead of their ids.
     */
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
                String stepId =
                        StringUtils.isBlank(parameters.getStepId()) ? preparation.getHeadId() : parameters.getStepId();
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
    }

    // WARN: must follow the creation process of PreparationCacheCondition
    private TransformationCacheKey getCacheKey(ExportParameters parameters) {
        return cacheKeyGenerator
                .generateContentKey(exportParametersUtil.populateFromPreparationExportParameter(parameters));
    }

    /**
     * Return the steps that are between the from and the to steps IDs.
     *
     * @param steps the steps to start from.
     * @param fromId the from step id.
     * @param toId the to step id.
     * @return the steps that are between the from and the to steps IDs.
     */
    private List<String> getMatchingSteps(List<String> steps, String fromId, String toId) {
        List<String> result = new ArrayList<>();
        boolean addStep = false;
        for (String step : steps) {
            // skip steps before the from
            if (fromId.equals(step)) {
                addStep = true;
            } else if (addStep) { // fromId should not be added, hence the else !
                result.add(step);
            }
            // skip steps after
            if (addStep && toId.equals(step)) {
                break;
            }
        }
        LOGGER.debug("Matching steps from {} to {} are {}", fromId, toId, steps);
        return result;
    }

    /**
     * A utility class to both extract information to run optimized strategy <b>and</b> check if there's enough information
     * to use the strategy.
     */
    private static class OptimizedPreparationInput {

        // not final
        private DataSetMetadata metadata;

        private TransformationCacheKey transformationCacheKey;

        private List<String> stepsToApply;

        private DataSetMetadata getMetadata() {
            return metadata;
        }

        public List<String> getStepsToApply() {
            return stepsToApply;
        }

        public TransformationCacheKey getTransformationCacheKey() {
            return transformationCacheKey;
        }
    }

    // Extract information or returns null is not applicable.
    private OptimizedPreparationInput analyseOptimizationApplicability(InternalExportParameters parameters) {
        OptimizedPreparationInput input = new OptimizedPreparationInput();

        if (parameters.getPreparation() == null) {
            // Not applicable (need preparation to work on).
            return null;
        }
        final List<String> steps = new ArrayList<>(parameters.getPreparation().getSteps());
        if (steps.size() <= 2) {
            LOGGER.debug("Not enough steps ({}) in preparation.", steps.size());
            return null;
        }
        String version = parameters.getStepId();
        String previousVersion = steps.get(parameters.getPreparation().getSteps().indexOf(version) - 1);
        // Get metadata of previous step
        final TransformationMetadataCacheKey transformationMetadataCacheKey = cacheKeyGenerator
                .generateMetadataKey(parameters.getPreparationId(), previousVersion, parameters.getFrom());
        if (!contentCache.has(transformationMetadataCacheKey)) {
            LOGGER.debug("No metadata cached for previous version '{}' (key for lookup: '{}')", previousVersion,
                    transformationMetadataCacheKey.getKey());
            return null;
        }
        try (InputStream metadataStream = contentCache.get(transformationMetadataCacheKey)) {
            input.metadata = mapper.readerFor(DataSetMetadata.class).readValue(metadataStream);
        } catch (IOException e) {
            LOGGER.warn("Error reading cache for key " + transformationMetadataCacheKey, e);
            return null;
        }
        input.transformationCacheKey = cacheKeyGenerator.generateContentKey( //
                parameters.getDatasetId(), //
                parameters.getPreparationId(), //
                previousVersion, //
                parameters.getExportType(), //
                parameters.getFrom(), //
                parameters.getFilter() //
        );
        LOGGER.debug("Previous content cache key: {}", input.transformationCacheKey.getKey());
        LOGGER.debug("Previous content cache key details: {}", input.transformationCacheKey);

        if (!contentCache.has(input.transformationCacheKey)) {
            LOGGER.debug("No content cached for previous version '{}'", previousVersion);
            return null;
        }

        input.stepsToApply = getMatchingSteps(parameters.getPreparation().getSteps(), previousVersion, version);

        return input;
    }

}
