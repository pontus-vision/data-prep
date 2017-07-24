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

package org.talend.dataprep.transformation.pipeline.runtime;

import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Signal;

/**
 * A node is a processing unit inside the transformation pipeline.
 */
public interface RuntimeNode {

    /**
     * Called for each incoming {@link DataSetRow} when a new row is submitted to the pipeline.
     *  @param row A {@link DataSetRow row} to be processed by this node.
     */
    void receive(DataSetRow row);

    /**
     * Sends a {@link Signal event} to the node. Signals are data-independent events to indicate external events (such
     * as end of the stream).
     *
     * @param signal A {@link Signal signal} to be sent to the pipeline.
     */
    void signal(Signal signal);

}
