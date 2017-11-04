/*
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * https://github.com/Talend/data-prep/blob/master/LICENSE
 *
 * You should have received a copy of the agreement
 * along with this program; if not, write to Talend SA
 * 9 rue Pages 92150 Suresnes, France
 */

package org.talend.dataprep.transformation;

import java.util.List;

import org.talend.dataprep.api.dataset.statistics.*;

public class ImmutableStatistics {

    private final Statistics delegate;

    public ImmutableStatistics(Statistics statistics) {
        this.delegate = statistics;
    }

    public long getCount() {
        return delegate.getCount();
    }

    public long getValid() {
        return delegate.getValid();
    }

    public long getInvalid() {
        return delegate.getInvalid();
    }

    public long getEmpty() {
        return delegate.getEmpty();
    }

    public double getMax() {
        return delegate.getMax();
    }

    public double getMin() {
        return delegate.getMin();
    }

    public double getMean() {
        return delegate.getMean();
    }

    public double getVariance() {
        return delegate.getVariance();
    }

    public long getDuplicateCount() {
        return delegate.getDuplicateCount();
    }

    public long getDistinctCount() {
        return delegate.getDistinctCount();
    }

    public List<DataFrequency> getDataFrequencies() {
        return delegate.getDataFrequencies();
    }

    public List<PatternFrequency> getPatternFrequencies() {
        return delegate.getPatternFrequencies();
    }

    public Quantiles getQuantiles() {
        return delegate.getQuantiles();
    }

    public Histogram getHistogram() {
        return delegate.getHistogram();
    }

    public TextLengthSummary getTextLengthSummary() {
        return delegate.getTextLengthSummary();
    }
}
