package org.talend.dataprep.transformation.pipeline.runtime;

import static org.talend.dataprep.api.dataset.row.AvroUtils.toDataSetRow;

import java.io.Serializable;
import java.util.function.Predicate;

import org.apache.avro.generic.IndexedRecord;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.talend.dataprep.api.dataset.row.AvroUtils;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.node.FilterNode;

class Filter extends DoFn<KV<IndexedRecord, AvroUtils.Metadata>, KV<IndexedRecord, AvroUtils.Metadata>> implements Serializable {

    private final BeamRuntime beamRuntime;

    private final FilterNode filterNode;

    Filter(BeamRuntime beamRuntime, FilterNode filterNode) {
        this.beamRuntime = beamRuntime;
        this.filterNode = filterNode;
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        Predicate<DataSetRow> predicate = beamRuntime.handleFilterNode(filterNode);
        KV<IndexedRecord, AvroUtils.Metadata> row = c.element();
        if (predicate.test(toDataSetRow(row.getKey(), row.getValue()))) {
            c.output(row);
        }
    }
}
