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

import java.io.Serializable;

/**
 * Links together {@link Node nodes}.
 */
public interface Link extends Serializable {

    /**
     * Visit the implementation of the {@link Link}.
     *
     * @param visitor A {@link Visitor} to visit the whole pipeline structure.
     */
    <T> T accept(Visitor<T> visitor);

    default Node getTarget() {
        return null;
    }
}
