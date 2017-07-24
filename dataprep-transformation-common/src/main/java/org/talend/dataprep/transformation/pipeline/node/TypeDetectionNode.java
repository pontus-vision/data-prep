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

package org.talend.dataprep.transformation.pipeline.node;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.dataset.StatisticsAdapter;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Visitor;
import org.talend.dataquality.common.inference.Analyzer;
import org.talend.dataquality.common.inference.Analyzers;

public class TypeDetectionNode extends ColumnFilteredNode {

    private final Function<List<ColumnMetadata>, Analyzer<Analyzers.Result>> analyzer;

    private final StatisticsAdapter adapter;

    public TypeDetectionNode(Predicate<? super ColumnMetadata> filter,
                             Function<List<ColumnMetadata>, Analyzer<Analyzers.Result>> analyzer,
                             StatisticsAdapter adapter) {
        super(filter);
        this.analyzer = analyzer;
        this.adapter = adapter;
    }

    public Function<List<ColumnMetadata>, Analyzer<Analyzers.Result>> getAnalyzer() {
        return analyzer;
    }

    public StatisticsAdapter getAdapter() {
        return adapter;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitTypeDetection(this);
    }

    @Override
    public Node copyShallow() {
        return new TypeDetectionNode(filter, analyzer, adapter);
    }
}
