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

import java.util.function.Consumer;

import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Signal;
import org.talend.dataprep.transformation.pipeline.Visitor;

/**
 * Node that execute a map function.
 */
public class ConsumerNode extends BasicNode {

    private final Consumer<DataSetRow> rowConsumer;

    private final Consumer<Signal> signalConsumer;

    public ConsumerNode(final Consumer<DataSetRow> rowConsumer, final Consumer<Signal> signalConsumer) {
        this.rowConsumer = rowConsumer == null ? row -> {
        } : rowConsumer;
        this.signalConsumer = signalConsumer == null ? signal -> {
        } : signalConsumer;
    }

    public Consumer<DataSetRow> getRowConsumer() {
        return rowConsumer;
    }

    public Consumer<Signal> getSignalConsumer() {
        return signalConsumer;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitConsumerNode(this);
    }

    @Override
    public Node copyShallow() {
        return new ConsumerNode(rowConsumer, signalConsumer);
    }
}
