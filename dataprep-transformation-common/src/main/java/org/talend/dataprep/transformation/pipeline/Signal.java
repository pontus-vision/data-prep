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

/**
 * Signals are data-independent events. They may be used to indicate end of stream or interruption.
 */
public enum Signal {
    /**
     * Signal for end of stream: no more data set row will be submitted to the pipeline.
     */
    END_OF_STREAM,
    /**
     * Signal for stopping pipeline: no more records to be expected and similarly to {@link #END_OF_STREAM}. This is
     * also similar
     * to {@link #CANCEL} but this a different semantic: STOP interrupts processing but records may still be present
     * after STOP.
     *
     * @see #CANCEL
     */
    STOP,
    /**
     * Signal for cancelling pipeline: no more records to be expected and similarly to {@link #END_OF_STREAM} all output
     * must be
     * correctly handled. CANCEL indicates processed records may be discarded and will not be used (if processed records
     * should be kept, use {@link #STOP}.
     *
     * @see #STOP
     */
    CANCEL
}
