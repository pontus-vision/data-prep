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

package org.talend.dataprep.transformation.pipeline.runtime;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.daikon.exception.TalendRuntimeException;
import org.talend.dataprep.BaseErrorCodes;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.DataSet;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.api.dataset.row.FlagNames;
import org.talend.dataprep.transformation.pipeline.Signal;
import org.talend.dataprep.transformation.pipeline.node.TypeDetectionNode;
import org.talend.dataprep.util.FilesHelper;
import org.talend.dataquality.common.inference.Analyzer;
import org.talend.dataquality.common.inference.Analyzers;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TypeDetectionRuntime implements RuntimeNode {

    private static final Logger LOGGER = LoggerFactory.getLogger(TypeDetectionRuntime.class);

    private final RuntimeNode nextNode;

    private final File reservoir;

    private final JsonGenerator generator;

    private final TypeDetectionNode typeDetectionNode;

    private RowMetadata metadata;

    private List<ColumnMetadata> filteredColumns;

    private Analyzer<Analyzers.Result> resultAnalyzer;

    private Set<String> filteredColumnNames = Collections.emptySet();

    TypeDetectionRuntime(TypeDetectionNode typeDetectionNode, RuntimeNode nextNode) {
        this.typeDetectionNode = typeDetectionNode;
        try {
            reservoir = File.createTempFile("TypeDetection", ".zip");
            JsonFactory factory = new JsonFactory();
            generator = factory.createGenerator(new GZIPOutputStream(new FileOutputStream(reservoir), true));
            generator.writeStartObject();
            generator.writeFieldName("records");
            generator.writeStartArray();
        } catch (IOException e) {
            throw new TalendRuntimeException(BaseErrorCodes.UNEXPECTED_EXCEPTION, e);
        }

        this.nextNode = nextNode;
    }

    @Override
    public void receive(DataSetRow row) {
        performColumnFilter(row);
        store(row);
        analyze(row);
    }

    private void performColumnFilter(DataSetRow row) {
        final RowMetadata rowMetadata = row.getRowMetadata();
        if (metadata == null || !Objects.equals(metadata, rowMetadata)) {
            List<ColumnMetadata> columns = rowMetadata.getColumns();
            if (filteredColumns == null) {
                filteredColumns = columns.stream().filter(typeDetectionNode.getFilter()).collect(Collectors.toList());
                filteredColumnNames = filteredColumns.stream().map(ColumnMetadata::getId).collect(Collectors.toSet());
            } else {
                throw new IllegalStateException();
            }
            metadata = rowMetadata;
        }
    }

    // Store row in temporary file
    private void store(DataSetRow row) {
        try {
            final List<ColumnMetadata> columns = row.getRowMetadata().getColumns();
            generator.writeStartObject();
            columns.forEach(column -> {
                try {
                    generator.writeStringField(column.getId(), row.get(column.getId()));
                } catch (IOException e) {
                    throw new TalendRuntimeException(BaseErrorCodes.UNEXPECTED_EXCEPTION, e);
                }
            });
            if (row.isDeleted()) {
                generator.writeBooleanField("_deleted", true);
            }
            final Optional<Long> tdpId = Optional.ofNullable(row.getTdpId());
            if (tdpId.isPresent()) {
                generator.writeNumberField(FlagNames.TDP_ID, tdpId.get());
            }
            for (Map.Entry<String, String> entry : row.getInternalValues().entrySet()) {
                generator.writeStringField(entry.getKey(), entry.getValue());
            }
            generator.writeEndObject();
        } catch (IOException e) {
            throw new TalendRuntimeException(BaseErrorCodes.UNEXPECTED_EXCEPTION, e);
        }
    }

    private void analyze(DataSetRow row) {
        if (!row.isDeleted()) {
            // Lazy initialization of the result analyzer
            if (resultAnalyzer == null) {
                resultAnalyzer = typeDetectionNode.getAnalyzer().apply(filteredColumns);
            }
            final String[] values = row.filter(filteredColumns) //
                    .order(metadata.getColumns()) //
                    .toArray(DataSetRow.SKIP_TDP_ID.and(e -> filteredColumnNames.contains(e.getKey())));
            try {
                resultAnalyzer.analyze(values);
            } catch (Exception e) {
                LOGGER.debug("Unable to analyze row '{}'.", Arrays.toString(values), e);
            }
        }
    }

    @Override
    public void signal(Signal signal) {
        try {
            if (signal == Signal.END_OF_STREAM || signal == Signal.CANCEL || signal == Signal.STOP) {
                // End temporary output
                generator.writeEndArray();
                generator.writeEndObject();
                generator.flush();
                generator.close();
                // Send stored records to next steps
                final ObjectMapper mapper = new ObjectMapper();
                if (metadata != null && resultAnalyzer != null) {
                    // Adapt row metadata to infer type (adapter takes care of type-forced columns)
                    resultAnalyzer.end();
                    final List<ColumnMetadata> columns = metadata.getColumns();
                    typeDetectionNode.getAdapter().adapt(columns, resultAnalyzer.getResult(), (Predicate<ColumnMetadata>) typeDetectionNode.getFilter());
                    columns.stream().filter(typeDetectionNode.getFilter()).forEach(c -> c.setStatistics(null));
                    resultAnalyzer.close();
                }
                // Continue process
                try (JsonParser parser = mapper.getFactory().createParser(new InputStreamReader(new GZIPInputStream(new FileInputStream(reservoir)), UTF_8))) {
                    final DataSet dataSet = mapper.reader(DataSet.class).readValue(parser);
                    dataSet.getRecords().map(r -> r.setRowMetadata(metadata)).forEach(nextNode::receive);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to perform delayed analysis.", e);
        } finally {
            try {
                generator.close();
            } catch (IOException e) {
                LOGGER.error("Unable to close JSON generator (causing potential temp file delete issues).", e);
            }
            FilesHelper.deleteQuietly(reservoir);
        }
        if (nextNode != null) {
            nextNode.signal(signal);
        }
    }

    @Override
    public RuntimeNode getNext() {
        return nextNode;
    }
}
