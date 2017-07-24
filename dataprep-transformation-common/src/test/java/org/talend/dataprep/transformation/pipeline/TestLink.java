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

package org.talend.dataprep.transformation.pipeline;

import java.util.ArrayList;
import java.util.List;

import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.link.BasicLink;

public class TestLink extends BasicLink {

    private final List<DataSetRow> emittedRows = new ArrayList<>();

    private final List<RowMetadata> emittedMetadata = new ArrayList<>();

    private final List<Signal> emittedSignals = new ArrayList<>();

    public TestLink(final Node target) {
        super(target);
    }

    public List<DataSetRow> getEmittedRows() {
        return emittedRows;
    }

    public List<RowMetadata> getEmittedMetadata() {
        return emittedMetadata;
    }

    public List<Signal> getEmittedSignals() {
        return emittedSignals;
    }
}
