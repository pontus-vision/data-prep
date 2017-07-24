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

package org.talend.dataprep.transformation.pipeline.link;

import org.talend.dataprep.transformation.pipeline.Node;

/**
 * Node that zip multiple stream.
 * It waits to receive all the inputs entry, and emit an array of all the inputs to the output
 */
public class ZipLink extends BasicLink {

    private final Node[] sources;

    public ZipLink(final Node[] sources, final Node target) {
        super(target);
        this.sources = sources;
        for (Node source : sources) {
            source.setLink(new BasicLink(target));
        }
    }

    public static ZipLink zip(final Node[] sources, final Node target) {
        return new ZipLink(sources, target);
    }

    public Node[] getSources() {
        return sources;
    }
}
