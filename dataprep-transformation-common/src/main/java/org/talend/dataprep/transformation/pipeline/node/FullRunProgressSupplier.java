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

package org.talend.dataprep.transformation.pipeline.node;

import java.util.function.Function;

import org.talend.dataprep.transformation.pipeline.Monitored;

/**
 * Supply FullRunProgress object.
 */
public class FullRunProgressSupplier implements Function<Monitored, ExecutionProgress> {

    /** Determine the precision of published progress (1000f means 4 digits precision). */
    private static final float PROGRESS_PRECISION = 1000f;

    private final Long previousElapsedTime;

    /** The current progress. */
    private float latestProgress;

    public FullRunProgressSupplier(AsyncExecution previousTransformation) {
        if (previousTransformation != null) {
            // Compute percentage based on previous execution time
            previousElapsedTime =
                    previousTransformation.getTime().getEndDate() - previousTransformation.getTime().getStartDate();
        } else {
            previousElapsedTime = null;
        }
    }

    @Override
    public ExecutionProgress apply(Monitored monitored) {
        if (previousElapsedTime != null) {
            // Compute percentage based on previous execution time
            if (monitored.getTotalTime() < previousElapsedTime) {
                float totalTime = monitored.getTotalTime();
                float preciseAdvance = totalTime / previousElapsedTime;
                latestProgress = roundAdvance(preciseAdvance);
            } else {
                latestProgress = .95f; // This execution is taking longer than estimated, blocks at 95%.
            }
        } else {
            // Do a lim(progress) -> 1 (tend towards completion but don't reach 100%).
            latestProgress = computeNextEmpiricalProgress(latestProgress);
        }
        ExecutionProgress executionProgress = new ExecutionProgress(latestProgress);
        executionProgress.setProcessedRows(monitored.getCount());
        return executionProgress;
    }

    private static float computeNextEmpiricalProgress(float progress) {
        float nextProgress = progress + ((1 - progress) / 4);
        nextProgress = roundAdvance(nextProgress);
        return nextProgress;
    }

    private static float roundAdvance(float preciseAdvance) {
        return Math.round(preciseAdvance * PROGRESS_PRECISION) / PROGRESS_PRECISION;
    }

}
