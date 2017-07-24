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

import java.util.function.Function;

import org.talend.dataprep.transformation.pipeline.Monitored;
import org.talend.dataprep.transformation.pipeline.Node;

/**
 * Node in charge of updating progress information for a transformation.
 */
public class ProgressNode extends BasicNode implements Monitored {

    /** How often will this node update transformation progress. */
    private static final int INTERVAL = 1000;

    /** When this transformation started. */
    private long startTime = -1;

    /** When was the last progress updated ? */
    private long lastProgressUpdate = System.currentTimeMillis();

    /** How many rows have been received. */
    private long receivedRows = 0;

    /** Function that supplies the progress object to from this. */
    private Function<Monitored, Object> progressSupplier;

    /**
     * Constructor.
     *
     * @param progressSupplier the function progress object supplier.
     */
    public ProgressNode(Function<Monitored, Object> progressSupplier) {
        this.progressSupplier = progressSupplier;
    }

    /**
     * @see Monitored#getTotalTime()
     */
    @Override
    public long getTotalTime() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * @see Monitored#getCount()
     */
    @Override
    public long getCount() {
        return receivedRows;
    }

    @Override
    public Node copyShallow() {
        return new ProgressNode(progressSupplier);
    }
}
