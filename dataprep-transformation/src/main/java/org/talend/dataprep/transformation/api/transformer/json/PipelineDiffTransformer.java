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

package org.talend.dataprep.transformation.api.transformer.json;

import static org.talend.dataprep.transformation.pipeline.node.FilterNode.Behavior.INTERRUPT;

import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.dataset.DataSet;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.dataset.StatisticsAdapter;
import org.talend.dataprep.quality.AnalyzerService;
import org.talend.dataprep.transformation.ExecutorService;
import org.talend.dataprep.transformation.actions.ActionParser;
import org.talend.dataprep.transformation.actions.ActionRegistry;
import org.talend.dataprep.transformation.api.transformer.Transformer;
import org.talend.dataprep.transformation.api.transformer.configuration.Configuration;
import org.talend.dataprep.transformation.api.transformer.configuration.PreviewConfiguration;
import org.talend.dataprep.transformation.format.WriterRegistrationService;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.builder.ActionNodesBuilder;
import org.talend.dataprep.transformation.pipeline.builder.NodeBuilder;
import org.talend.dataprep.transformation.pipeline.node.TransformerWriter;
import org.talend.dataprep.transformation.pipeline.node.WriterNode;
import org.talend.dataprep.transformation.pipeline.runtime.ExecutorRunnable;
import org.talend.dataprep.transformation.pipeline.runtime.ExecutorVisitor;

/**
 * Transformer that preview the transformation (puts additional json content so that the front can display the
 * difference between current and previous transformation).
 */
@Component
class PipelineDiffTransformer implements Transformer {

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
    private ExecutorService executorService;

    /**
     * Starts the transformation in preview mode.
     *  @param input the dataset content.
     * @param configuration The {@link Configuration configuration} for this transformation.
     */
    @Override
    public ExecutorRunnable buildExecutable(DataSet input, Configuration configuration) {
        Validate.notNull(input, "Input cannot be null.");
        final PreviewConfiguration previewConfiguration = (PreviewConfiguration) configuration;
        final RowMetadata rowMetadata = input.getMetadata().getRowMetadata();
        final TransformerWriter writer = writerRegistrationService.getWriter(
                configuration.formatId(),
                configuration.getArguments()
        );

        // Build diff pipeline
        final String referenceActions = previewConfiguration.getReferenceActions();
        final String previewActions = previewConfiguration.getPreviewActions();
        final Node referencePipeline = buildPipeline(rowMetadata, referenceActions);
        final Node previewPipeline = buildPipeline(rowMetadata, previewActions);

        // Filter source records (extract TDP ids information)
        final List<Long> indexes = previewConfiguration.getIndexes();
        final boolean isIndexLimited = indexes != null && !indexes.isEmpty();
        final Long minIndex = isIndexLimited ? indexes.stream().mapToLong(Long::longValue).min().getAsLong() : 0L;
        final Long maxIndex = isIndexLimited ? indexes.stream().mapToLong(Long::longValue).max().getAsLong() : Long.MAX_VALUE;
        final Predicate<DataSetRow> filter = isWithinWantedIndexes(minIndex, maxIndex);

        // Build diff pipeline
        final Node diffPipeline = NodeBuilder
                .filteredSource(filter, INTERRUPT, input.getRecords()) //
                .dispatch(referencePipeline, previewPipeline) //
                .zip(rows -> rows[1].diff(rows[0])) //
                .to(new WriterNode(writer)) //
                .build();

        ExecutorVisitor<?> visitor = executorService.getExecutor();
        visitor.setOutputStream(configuration.output());
        diffPipeline.accept(visitor);
        return visitor.toRunnable();
    }

    private Node buildPipeline(RowMetadata rowMetadata, String actions) {
        return ActionNodesBuilder.builder() //
                .actions(actionParser.parse(actions)) //
                .initialMetadata(rowMetadata) //
                .actionRegistry(actionRegistry) //
                .analyzerService(analyzerService) //
                .allowSchemaAnalysis(false) //
                .needStatisticsAfter(false) //
                .needStatisticsBefore(false) //
                .statisticsAdapter(adapter) //
                .build();
    }

    @Override
    public boolean accept(Configuration configuration) {
        return PreviewConfiguration.class.isAssignableFrom(configuration.getClass());
    }

    private Predicate<DataSetRow> isWithinWantedIndexes(Long minIndex, Long maxIndex) {
        return row -> row.getTdpId() >= minIndex && row.getTdpId() <= maxIndex;
    }
}
