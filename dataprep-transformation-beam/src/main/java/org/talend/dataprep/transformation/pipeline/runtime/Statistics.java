package org.talend.dataprep.transformation.pipeline.runtime;

import static org.talend.dataprep.api.dataset.row.AvroUtils.toDataSetRow;
import static org.talend.dataprep.api.dataset.row.AvroUtils.toRecord;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.avro.generic.IndexedRecord;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.row.AvroUtils;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.dataset.StatisticsAdapter;
import org.talend.dataprep.transformation.pipeline.node.StatisticsNode;
import org.talend.dataquality.common.inference.Analyzer;
import org.talend.dataquality.common.inference.Analyzers;

class Statistics extends DoFn<KV<IndexedRecord, AvroUtils.Metadata>, KV<IndexedRecord, AvroUtils.Metadata>> {

    private final StatisticsNode statisticsNode;

    private transient Analyzer<Analyzers.Result> configuredAnalyzer;

    public Statistics(StatisticsNode statisticsNode) {
        this.statisticsNode = statisticsNode;
    }

    @ProcessElement
    public void process(ProcessContext c) {
        final KV<IndexedRecord, AvroUtils.Metadata> record = c.element();
        final DataSetRow row = toDataSetRow(record.getKey(), record.getValue());

        if (!row.isDeleted()) {
            final RowMetadata rowMetadata = row.getRowMetadata();
            final List<ColumnMetadata> columns = rowMetadata.getColumns();
            final List<ColumnMetadata> filteredColumns = columns //
                    .stream() //
                    .filter(statisticsNode.getFilter()) //
                    .collect(Collectors.toList());
            if (configuredAnalyzer == null) {
                this.configuredAnalyzer = statisticsNode.getAnalyzer().apply(filteredColumns);
            }
            configuredAnalyzer.analyze(row //
                    .filter(filteredColumns) //
                    .order(filteredColumns) //
                    .toArray(DataSetRow.SKIP_TDP_ID) //
            );

            if (c.pane().isLast()) {
                final StatisticsAdapter adapter = statisticsNode.getAdapter();
                adapter.adapt(filteredColumns, configuredAnalyzer.getResult());
                final AvroUtils.Record enhancedRecord = toRecord(row.setRowMetadata(rowMetadata));
                c.output(KV.of(enhancedRecord.getIndexedRecord(), enhancedRecord.getMetadata()));
            } else {
                c.output(record);
            }
        } else {
            c.output(record);
        }
    }

}
