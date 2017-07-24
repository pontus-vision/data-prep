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

package org.talend.dataprep.transformation.pipeline.builder;

import static org.talend.dataprep.transformation.actions.ActionDefinition.Behavior.NEED_STATISTICS_INVALID;
import static org.talend.dataprep.transformation.actions.ActionDefinition.Behavior.NEED_STATISTICS_PATTERN;
import static org.talend.dataprep.transformation.actions.common.ImplicitParameters.FILTER;

import java.util.List;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.lang.StringUtils;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.preparation.Action;
import org.talend.dataprep.dataset.StatisticsAdapter;
import org.talend.dataprep.quality.AnalyzerService;
import org.talend.dataprep.transformation.actions.ActionDefinition;
import org.talend.dataprep.transformation.actions.ActionRegistry;
import org.talend.dataprep.transformation.actions.common.RunnableAction;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.node.InvalidDetectionNode;
import org.talend.dataprep.transformation.pipeline.node.NoOpNode;
import org.talend.dataprep.transformation.pipeline.node.StatisticsNode;
import org.talend.dataprep.transformation.pipeline.node.TypeDetectionNode;
import org.talend.dataquality.common.inference.Analyzer;
import org.talend.dataquality.common.inference.Analyzers;

public class StatisticsNodesBuilder {

    private static final Predicate<ColumnMetadata> ALL_COLUMNS = c -> true;

    private AnalyzerService analyzerService;

    private ActionRegistry actionRegistry;

    private StatisticsAdapter statisticsAdapter;

    private List<RunnableAction> actions;

    private List<ColumnMetadata> columns;

    private boolean allowSchemaAnalysis = true;

    private ActionsProfile actionsProfile;

    private StatisticsNodesBuilder() {
    }

    public static StatisticsNodesBuilder builder() {
        return new StatisticsNodesBuilder();
    }

    public StatisticsNodesBuilder analyzerService(final AnalyzerService analyzerService) {
        this.analyzerService = analyzerService;
        return this;
    }

    public StatisticsNodesBuilder actionRegistry(final ActionRegistry actionRegistry) {
        this.actionRegistry = actionRegistry;
        return this;
    }

    public StatisticsNodesBuilder statisticsAdapter(final StatisticsAdapter statisticsAdapter) {
        this.statisticsAdapter = statisticsAdapter;
        return this;
    }

    public StatisticsNodesBuilder allowSchemaAnalysis(final boolean allowSchemaAnalysis) {
        this.allowSchemaAnalysis = allowSchemaAnalysis;
        return this;
    }

    public StatisticsNodesBuilder actions(final List<RunnableAction> actions) {
        this.actions = actions;
        return this;
    }

    public StatisticsNodesBuilder columns(final List<ColumnMetadata> columns) {
        this.columns = columns;
        return this;
    }

    public Node buildPreStatistics() {
        performActionsProfiling();
        return getTypeDetectionNode(ALL_COLUMNS);
    }

    public Node buildPostStatistics() {
        performActionsProfiling();
        if (actionsProfile.needFullAnalysis()) {
            return NodeBuilder
                    .from(getTypeDetectionNode(actionsProfile.getFilterForFullAnalysis()))
                    .to(getInvalidDetectionNode(actionsProfile.getFilterForInvalidAnalysis()))
                    .to(getFullStatisticsNode(actionsProfile.getFilterForInvalidAnalysis()))
                    .build();
        }

        if (actionsProfile.needOnlyInvalidAnalysis()) {
            return NodeBuilder
                    .from(getInvalidDetectionNode(actionsProfile.getFilterForInvalidAnalysis()))
                    .to(getQualityStatisticsNode(actionsProfile.getFilterForInvalidAnalysis()))
                    .build();
        }
        return new NoOpNode();
    }

    /**
     * Insert statistics computing nodes before the supplied action node if needed.
     *
     * @param nextAction action needing
     * @return The node that performs statistics in the middle of the pipeline
     */
    public Node buildIntermediateStatistics(final Action nextAction) {
        Node node = null;
        performActionsProfiling();
        if (needIntermediateStatistics(nextAction)) {
            final Set<ActionDefinition.Behavior> behavior = actionsProfile.getBehavior(nextAction);
            if (behavior.contains(NEED_STATISTICS_PATTERN)) {
                node = NodeBuilder.from(getPatternDetectionNode(actionsProfile.getFilterForPatternAnalysis())).build();
            } else {
                // 2 cases remain as this point: action needs invalid values or filter attached to action does
                node = NodeBuilder
                        .from(getTypeDetectionNode(actionsProfile.getFilterForFullAnalysis()))
                        .to(getInvalidDetectionNode(actionsProfile.getFilterForInvalidAnalysis()))
                        .build();
            }
        }
        return node;
    }

    private boolean needIntermediateStatistics(final Action nextAction) {
        // next action indicates that it need fresh statistics
        final Set<ActionDefinition.Behavior> behavior = actionsProfile.getBehavior(nextAction);
        if (behavior.contains(NEED_STATISTICS_PATTERN) || behavior.contains(NEED_STATISTICS_INVALID)) {
            return true;
        }

        // action has filter that is on valid/invalid
        if (nextAction.getParameters().containsKey(FILTER.getKey())) {
            // action has a filterForFullAnalysis, to cover cases where filters are on invalid values
            final String filterAsString = nextAction.getParameters().get(FILTER.getKey());
            return StringUtils.contains(filterAsString, "valid") || StringUtils.contains(filterAsString, "invalid");
        }

        return false;
    }

    private void performActionsProfiling() {
        if (actionsProfile != null) {
            return;
        }
        checkInputs();

        final ActionsStaticProfiler profiler = new ActionsStaticProfiler(actionRegistry);
        actionsProfile = profiler.profile(columns, actions);
    }

    private void checkInputs() {
        if (actionRegistry == null) {
            throw new MissingResourceException("You need to provide an actionRegistry", "ActionRegistry", null);
        }
        if (statisticsAdapter == null) {
            throw new MissingResourceException("You need to provide a statistics adapter", "StatisticsAdapter", null);
        }
        if (actions == null) {
            throw new MissingResourceException("You need to provide the whole list of actions", "List", null);
        }
        if (columns == null) {
            throw new MissingResourceException("You need to provide the whole list of columns", "List", null);
        }
    }

    /**
     * Create a full analyzer
     */
    private Function<List<ColumnMetadata>, Analyzer<Analyzers.Result>> getQualityAnalyzer() {
        return c -> analyzerService.build(c, AnalyzerService.Analysis.QUALITY);
    }

    /**
     * Create a full analyzer
     */
    private Function<List<ColumnMetadata>, Analyzer<Analyzers.Result>> getFullAnalyzer() {
        return StatisticsNode.getDefaultAnalyzer(analyzerService);
    }

    private Node getTypeDetectionNode(final Predicate<ColumnMetadata> columnFilter) {
        return allowSchemaAnalysis ? new TypeDetectionNode(columnFilter, analyzerService::schemaAnalysis, statisticsAdapter)
                : new NoOpNode();
    }

    private Node getPatternDetectionNode(final Predicate<ColumnMetadata> columnFilter) {
        return allowSchemaAnalysis
                ? new TypeDetectionNode(columnFilter, c -> analyzerService.build(c, AnalyzerService.Analysis.PATTERNS), statisticsAdapter)
                : new NoOpNode();
    }

    private Node getInvalidDetectionNode(final Predicate<ColumnMetadata> columnFilter) {
        return new InvalidDetectionNode(columnFilter);
    }

    private Node getQualityStatisticsNode(final Predicate<ColumnMetadata> columnFilter) {
        return new StatisticsNode(getQualityAnalyzer(), columnFilter, statisticsAdapter);
    }

    private Node getFullStatisticsNode(final Predicate<ColumnMetadata> columnFilter) {
        return new StatisticsNode(getFullAnalyzer(), columnFilter, statisticsAdapter);
    }
}
