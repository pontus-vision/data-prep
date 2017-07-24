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

package org.talend.dataprep.transformation.api.action;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.dataset.StatisticsAdapter;
import org.talend.dataprep.quality.AnalyzerService;
import org.talend.dataprep.transformation.actions.common.RunnableAction;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Runtimes;
import org.talend.dataprep.transformation.pipeline.builder.NodeBuilder;
import org.talend.dataprep.transformation.pipeline.node.*;

public class ActionTestWorkbench {

    private ActionTestWorkbench() {
    }

    public static List<DataSetRow> test(DataSetRow input, RunnableAction... actions) {
        return test(Collections.singleton(input), actions);
    }

    public static List<DataSetRow> test(Collection<DataSetRow> input, RunnableAction... actions) {
        NodeBuilder builder = NodeBuilder.source(input.stream());
        for (RunnableAction action : actions) {
            builder = builder.to(new CompileNode(action, action.getParameters()));
        }
        for (RunnableAction action : actions) {
            builder = builder.to(new ActionNode(action, action.getParameters()));
        }
        builder.to(new InvalidDetectionNode(new AllColumns()));
        builder.to(new StatisticsNode(new AnalyzerService(), new AllColumns(), new StatisticsAdapter(40)));
        final Node pipeline = builder.to(new CollectorNode()).build();
        return Runtimes.execute(pipeline);
    }

    private static class AllColumns implements Predicate<ColumnMetadata>, Serializable {

        @Override
        public boolean test(ColumnMetadata columnMetadata) {
            return true;
        }
    }
}
