// ============================================================================
//
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

package org.talend.dataprep.dataset.store.content;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.DataSetMetadata;
import org.talend.dataprep.api.dataset.json.DataSetRowIterator;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.api.dataset.row.InvalidMarker;
import org.talend.dataprep.conversions.BeanConversionService;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.CommonErrorCodes;
import org.talend.dataprep.quality.AnalyzerService;
import org.talend.dataprep.schema.*;
import org.talend.dataquality.common.inference.Analyzer;
import org.talend.dataquality.common.inference.Analyzers;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.Resource;

import static org.slf4j.LoggerFactory.getLogger;
import static org.talend.dataprep.exception.error.CommonErrorCodes.UNABLE_TO_SERIALIZE_TO_JSON;

/**
 * Base class for DataSet content stores.
 */
public abstract class DataSetContentStore {

    @Value("${dataset.records.limit:10000}")
    private long sampleSize;

    @Autowired
    private AnalyzerService service;

    /** DataPrep ready jackson builder. */
    @Autowired
    protected ObjectMapper mapper;

    /** Task executor used to serialize CSV dataset into JSON. */
    @Resource(name = "serializer#csv#executor")
    private TaskExecutor executor;

    @Autowired
    private BeanConversionService beanConversionService;

    @Autowired
    private DataprepSchema dataprepSchema;

    private static final Logger LOGGER = getLogger(DataSetContentStore.class);

    /**
     * Stores (persists) a data set raw content to a storage. The only expectation is for {@link #get(DataSetMetadata)}
     * to return content after this method ends.
     *
     * @param dataSetMetadata The data set metadata attached to the {@link DataSetMetadata data set}.
     * @param dataSetContent Content of the data set.
     * @see #get(DataSetMetadata)
     * @see #delete(DataSetMetadata)
     */
    public abstract void storeAsRaw(DataSetMetadata dataSetMetadata, InputStream dataSetContent);

    /**
     * Returns the {@link DataSetMetadata data set} content as <b>JSON</b> format. Whether data set content was JSON or
     * not, method is expected to provide a JSON output. It's up to the implementation to:
     * <ul>
     * <li>Convert data content to JSON.</li>
     * <li>Throw an exception if data set is not ready for read (content type missing).</li>
     * </ul>
     * Implementations are also encouraged to implement method with no blocking code.
     *
     * @param dataSetMetadata The {@link DataSetMetadata data set} to read content from.
     * @return A valid <b>JSON</b> stream. It is a JSON array where each element in the array contains a single data set
     * row (it does not mean there's a line in input stream per data set row, a data set row might be split on multiple
     * rows in stream).
     */
    protected InputStream get(DataSetMetadata dataSetMetadata) {
        return get(dataSetMetadata, sampleSize);
    }

    /**
     * Returns the {@link DataSetMetadata data set} content as <b>JSON</b> format. Whether data set content was JSON or
     * not, method is expected to provide a JSON output. It's up to the implementation to:
     * <ul>
     * <li>Convert data content to JSON.</li>
     * <li>Throw an exception if data set is not ready for read (content type missing).</li>
     * </ul>
     * Implementations are also encouraged to implement method with no blocking code.
     *
     * @param dataSetMetadata The {@link DataSetMetadata data set} to read content from.
     * @param limit A limit to pass to content supplier (use -1 for "no limit). Used as parameter for both raw content supplier
     * and JSON serializer.
     * @return A valid <b>JSON</b> stream. It is a JSON array where each element in the array contains a single data set
     * row (it does not mean there's a line in input stream per data set row, a data set row might be split on multiple
     * rows in stream).
     */
    protected InputStream get(DataSetMetadata dataSetMetadata, long limit) {
        FormatFamily formatFamily = dataprepSchema.getFormatFamily(dataSetMetadata.getContent().getFormatFamilyId());
        DeSerializer deSerializer = dataprepSchema.getDeserializer(formatFamily.getMediaType());
        InputStream rawContent = getAsRaw(dataSetMetadata, limit);

        try {
            PipedInputStream pipe = new PipedInputStream();
            PipedOutputStream jsonOutput = new PipedOutputStream(pipe);
            // Serialize asynchronously for better performance (especially if caller doesn't consume all, see sampling).
            executor.execute(() -> {
                Format format = new Format(formatFamily, Charset.forName(dataSetMetadata.getEncoding()));
                SheetContent content = beanConversionService.convert(dataSetMetadata, SheetContent.class);

                try (DeSerializer.RecordReader reader = deSerializer.deserialize(rawContent, format, content);
                        JsonRecordWriter writer = new JsonRecordWriter(jsonOutput)) {
                    int recordsRead = 0;
                    DeSerializer.Record record = reader.read();
                    while (record != null && recordsRead++ < limit) {
                        writer.writeRecord(record);
                        record = reader.read();
                    }
                } catch (IOException e) {
                    // if the consumer closed the stream, it's OK
                    LOGGER.debug("stream closed for serialization, this may by normal if the consumer closed it", e.getMessage());
                } catch (Exception e) {
                    throw new TDPException(UNABLE_TO_SERIALIZE_TO_JSON, e);
                }
            });
            return pipe;
        } catch (IOException e) {
            throw new TDPException(UNABLE_TO_SERIALIZE_TO_JSON, e);
        }
    }

    /**
     * Similarly to {@link #get(DataSetMetadata)} returns the content of the data set but as a {@link Stream stream} of
     * {@link DataSetRow rows} instead of JSON content. Same as calling {@link #get(DataSetMetadata)} (DataSetMetadata, long)}
     * with limit = -1.
     *
     * @param dataSetMetadata The {@link DataSetMetadata data set} to read rows from.
     * @return A valid <b>{@link DataSetRow}</b> stream.
     */
    public Stream<DataSetRow> stream(DataSetMetadata dataSetMetadata) {
        return stream(dataSetMetadata, sampleSize);
    }

    /**
     * Similarly to {@link #get(DataSetMetadata)} returns the content of the data set but as a {@link Stream stream} of
     * {@link DataSetRow rows} instead of JSON content.
     *
     * @param dataSetMetadata The {@link DataSetMetadata data set} to read rows from.
     * @param limit A limit to pass to raw content supplier (use -1 for "no limit). Used as parameter to call
     * {@link #get(DataSetMetadata, long)}.
     * @return A valid <b>{@link DataSetRow}</b> stream.
     */
    public Stream<DataSetRow> stream(DataSetMetadata dataSetMetadata, long limit) {
        final InputStream inputStream = get(dataSetMetadata, limit);
        final DataSetRowIterator iterator = new DataSetRowIterator(inputStream);
        final Iterable<DataSetRow> rowIterable = () -> iterator;
        Stream<DataSetRow> dataSetRowStream = StreamSupport.stream(rowIterable.spliterator(), false);

        // make sure to close the original input stream when closing this one
        AtomicLong tdpId = new AtomicLong(1);
        final List<ColumnMetadata> columns = dataSetMetadata.getRowMetadata().getColumns();
        final Analyzer<Analyzers.Result> analyzer = service.build(columns, AnalyzerService.Analysis.QUALITY);

        dataSetRowStream = dataSetRowStream //
                .filter(r -> !r.isEmpty()) //
                .peek(r -> {
                    final String[] values = r.order(columns).toArray(DataSetRow.SKIP_TDP_ID);
                    analyzer.analyze(values);
                }) //
                .map(new InvalidMarker(columns, analyzer)) // Mark invalid columns as detected by provided analyzer.
                .peek(r -> r.setTdpId(tdpId.getAndIncrement())) //
                .onClose(() -> { //
                    try {
                        inputStream.close();
                    } catch (Exception e) {
                        throw new TDPException(CommonErrorCodes.UNEXPECTED_EXCEPTION, e);
                    }
                });

        return dataSetRowStream;
    }

    /**
     * Returns the {@link DataSetMetadata data set} content as "raw" (i.e. the content supplied by user upon data set
     * creation). Same as calling {@link #getAsRaw(DataSetMetadata, long)}} (DataSetMetadata, long)} with limit = -1.
     *
     * @param dataSetMetadata The {@link DataSetMetadata data set} to read content from.
     * @return The content associated with <code>dataSetMetadata</code>.
     */
    public InputStream getAsRaw(DataSetMetadata dataSetMetadata) {
        return getAsRaw(dataSetMetadata, sampleSize);
    }

    /**
     * Returns the {@link DataSetMetadata data set} content as "raw" (i.e. the content supplied by user upon data set
     * creation).
     *
     * @param dataSetMetadata The {@link DataSetMetadata data set} to read content from.
     * @param limit A limit to pass to raw content supplier (use -1 for "no limit).
     * @return The content associated with <code>dataSetMetadata</code>.
     */
    public abstract InputStream getAsRaw(DataSetMetadata dataSetMetadata, long limit);

    /**
     * Deletes the {@link DataSetMetadata data set}. No recovery operation is expected.
     *
     * @param dataSetMetadata The data set to delete.
     */
    public abstract void delete(DataSetMetadata dataSetMetadata);

    /**
     * Removes all stored content. No recovery operation is expected.
     */
    public abstract void clear();
}
