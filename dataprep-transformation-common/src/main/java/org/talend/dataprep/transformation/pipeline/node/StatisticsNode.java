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

package org.talend.dataprep.transformation.pipeline.node;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.dataset.StatisticsAdapter;
import org.talend.dataprep.quality.AnalyzerService;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Visitor;
import org.talend.dataquality.common.inference.Analyzer;
import org.talend.dataquality.common.inference.Analyzers;

/**
 * <p>
 * This node performs statistical analysis.
 * </p>
 * <p>
 * Please note this class does not perform invalid values detection (see {@link InvalidDetectionNode} for this).
 * </p>
 */
public class StatisticsNode extends ColumnFilteredNode {

    private final Function<List<ColumnMetadata>, Analyzer<Analyzers.Result>> analyzer;

    private final StatisticsAdapter adapter;

    public StatisticsNode(Function<List<ColumnMetadata>, Analyzer<Analyzers.Result>> analyzer,
            Predicate<? super ColumnMetadata> filter, StatisticsAdapter adapter) {
        super(filter);
        this.analyzer = analyzer;
        this.adapter = adapter;
    }

    /**
     * Construct a statisticsNode performing default analysis which are :
     * quality, cardinality, frequency, patterns, the length, quantiles, summary and histogram analysis.
     *
     * @param analyzerService the analyzer service to use
     * @param filter the filter to apply on values of a column
     * @param adapter the adapter used to retrieve statistical information
     */
    public StatisticsNode(AnalyzerService analyzerService, Predicate<ColumnMetadata> filter,
            StatisticsAdapter adapter) {
        this(getDefaultAnalyzer(analyzerService), filter, adapter);
    }

    /**
     * Creates a default analyzer with te specified analyzer service.
     * This analyzer performs quality, cardinality, frequency, patterns, the length, quantiles, summary and histogram
     * analysis.
     *
     * @param analyzerService the provided analyzer service
     */
    public static Function<List<ColumnMetadata>, Analyzer<Analyzers.Result>>
            getDefaultAnalyzer(AnalyzerService analyzerService) {
        return new DefaultAnalyzerFunction(analyzerService);
    }

    public Function<List<ColumnMetadata>, Analyzer<Analyzers.Result>> getAnalyzer() {
        return analyzer;
    }

    public StatisticsAdapter getAdapter() {
        return adapter;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitStatistics(this);
    }

    @Override
    public Node copyShallow() {
        return new StatisticsNode(analyzer, filter, adapter);
    }

    private static class DefaultAnalyzerFunction
            implements Function<List<ColumnMetadata>, Analyzer<Analyzers.Result>>, Serializable {

        private final AnalyzerService analyzerService;

        private DefaultAnalyzerFunction(AnalyzerService analyzerService) {
            this.analyzerService = analyzerService;
        }

        @Override
        public Analyzer<Analyzers.Result> apply(List<ColumnMetadata> c) {
            return analyzerService.build(c, //
                    AnalyzerService.Analysis.QUALITY, //
                    AnalyzerService.Analysis.CARDINALITY, //
                    AnalyzerService.Analysis.FREQUENCY, //
                    AnalyzerService.Analysis.PATTERNS, //
                    AnalyzerService.Analysis.LENGTH, //
                    AnalyzerService.Analysis.QUANTILES, //
                    AnalyzerService.Analysis.SUMMARY, //
                    AnalyzerService.Analysis.HISTOGRAM);
        }
    }
}
