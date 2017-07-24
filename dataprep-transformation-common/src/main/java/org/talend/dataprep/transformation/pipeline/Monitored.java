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

package org.talend.dataprep.transformation.pipeline;

import org.talend.dataprep.api.dataset.row.DataSetRow;

/**
 * To be implemented to indicate pipeline element can monitor performance.
 */
public interface Monitored {

    /**
     * @return The total time spent in the pipeline element (in milliseconds).
     */
    long getTotalTime();

    /**
     * @return The number of {@link DataSetRow row} processed by this pipeline element.
     */
    long getCount();
}
