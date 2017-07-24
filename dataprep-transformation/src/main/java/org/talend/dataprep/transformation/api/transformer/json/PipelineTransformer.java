// ============================================================================
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

package org.talend.dataprep.transformation.api.transformer.json;

import static org.talend.dataprep.cache.ContentCache.TimeToLive.DEFAULT;
import static org.talend.dataprep.transformation.api.transformer.configuration.Configuration.Volume.SMALL;

import java.io.Serializable;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.dataset.DataSet;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.preparation.PreparationMessage;
import org.talend.dataprep.api.preparation.Step;
import org.talend.dataprep.cache.ContentCacheKey;
import org.talend.dataprep.dataset.StatisticsAdapter;
import org.talend.dataprep.quality.AnalyzerService;
import org.talend.dataprep.transformation.ExecutorService;
import org.talend.dataprep.transformation.actions.ActionParser;
import org.talend.dataprep.transformation.actions.ActionRegistry;
import org.talend.dataprep.transformation.api.transformer.ConfiguredCacheWriter;
import org.talend.dataprep.transformation.api.transformer.Transformer;
import org.talend.dataprep.transformation.api.transformer.configuration.Configuration;
import org.talend.dataprep.cache.CacheKeyGenerator;
import org.talend.dataprep.cache.TransformationMetadataCacheKey;
import org.talend.dataprep.transformation.format.WriterRegistrationService;
import org.talend.dataprep.transformation.pipeline.node.Pipeline;
import org.talend.dataprep.transformation.pipeline.node.TransformerWriter;
import org.talend.dataprep.transformation.pipeline.node.WriterNode;
import org.talend.dataprep.transformation.pipeline.runtime.ExecutorRunnable;
import org.talend.dataprep.transformation.pipeline.runtime.ExecutorVisitor;
import org.talend.dataprep.transformation.service.DefaultStepMetadataRepository;

@Component
public class PipelineTransformer implements Transformer {

    @Autowired
    private ActionParser actionParser;

    @Autowired
    private ActionRegistry actionRegistry;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private WriterRegistrationService writerRegistrationService;

    @Autowired
    private StatisticsAdapter adapter;

    @Autowired
    private CacheKeyGenerator cacheKeyGenerator;

    @Autowired
    private DefaultStepMetadataRepository preparationUpdater;

    @Autowired
    private ExecutorService executorService;

    @Override
    public ExecutorRunnable buildExecutable(DataSet input, Configuration configuration) {
        final RowMetadata rowMetadata = input.getMetadata().getRowMetadata();

        // prepare the fallback row metadata
        final TransformerWriter writer = writerRegistrationService.getWriter(configuration.formatId(), //
                configuration.getArguments() //
        );
        final ConfiguredCacheWriter metadataWriter = new ConfiguredCacheWriter(DEFAULT);
        final TransformationMetadataCacheKey metadataKey = cacheKeyGenerator.generateMetadataKey(configuration.getPreparationId(),
                configuration.stepId(), configuration.getSourceType());
        final PreparationMessage preparation = configuration.getPreparation();
        final Function<Step, RowMetadata> rowMetadataSupplier = s -> Optional.ofNullable(s.getRowMetadata()) //
                .map(id -> preparationUpdater.get(id)) //
                .orElse(null);
        final Pipeline pipeline = Pipeline.Builder.builder() //
                .withInput(input) //
                .withAnalyzerService(analyzerService) //
                .withActionRegistry(actionRegistry) //
                .withPreparation(preparation) //
                .withActions(actionParser.parse(configuration.getActions())) //
                .withInitialMetadata(rowMetadata, configuration.volume() == SMALL) //
                .withMonitor(configuration.getMonitor()) //
                .withFilter(configuration.getFilter()) //
                .withLimit(configuration.getLimit()) //
                .withFilterOut(configuration.getOutFilter()) //
                .withOutputMetadataCache(new OutputMetadataCache(metadataKey, metadataWriter)) //
                .withOutput(() -> new WriterNode(writer)) //
                .withStatisticsAdapter(adapter) //
                .withStepMetadataSupplier(rowMetadataSupplier) //
                .withGlobalStatistics(configuration.isGlobalStatistics()) //
                .allowMetadataChange(configuration.isAllowMetadataChange()) //
                .build();

        // wrap this transformer into an executable transformer
        ExecutorVisitor<?> visitor = executorService.getExecutor();
        visitor.setOutputStream(configuration.output());
        pipeline.accept(visitor);
        return visitor.toRunnable();
    }

    @Override
    public boolean accept(Configuration configuration) {
        return Configuration.class.equals(configuration.getClass());
    }

    private static class OutputMetadataCache implements Consumer<RowMetadata>, Serializable {

        private final ContentCacheKey metadataKey;

        private final ConfiguredCacheWriter metadataWriter;

        private OutputMetadataCache(ContentCacheKey metadataKey, ConfiguredCacheWriter metadataWriter) {
            this.metadataKey = metadataKey;
            this.metadataWriter = metadataWriter;
        }

        @Override
        public void accept(RowMetadata metadata) {
            metadataWriter.write(metadataKey, metadata);
        }
    }

}
