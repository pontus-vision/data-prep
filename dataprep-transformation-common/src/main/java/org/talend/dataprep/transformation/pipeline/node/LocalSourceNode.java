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

package org.talend.dataprep.transformation.pipeline.node;

import java.util.stream.Stream;

import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Visitor;

public class LocalSourceNode extends BasicNode {

    private final Stream<DataSetRow> source;

    public LocalSourceNode(Stream<DataSetRow> source) {
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null");
        }
        this.source = source;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitLocalSource(this);
    }

    @Override
    public Node copyShallow() {
        return new LocalSourceNode(source);
    }

    public Stream<DataSetRow> getSource() {
        return source;
    }
}
