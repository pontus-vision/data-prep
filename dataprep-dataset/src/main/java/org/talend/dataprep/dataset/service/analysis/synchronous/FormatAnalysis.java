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

package org.talend.dataprep.dataset.service.analysis.synchronous;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.DataSetContent;
import org.talend.dataprep.api.dataset.DataSetMetadata;
import org.talend.dataprep.api.dataset.Schema;
import org.talend.dataprep.configuration.EncodingSupport;
import org.talend.dataprep.dataset.store.content.ContentStoreRouter;
import org.talend.dataprep.dataset.store.metadata.DataSetMetadataRepository;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.DataSetErrorCodes;
import org.talend.dataprep.lock.DistributedLock;
import org.talend.dataprep.log.Markers;
import org.talend.dataprep.schema.*;
import org.talend.dataprep.schema.csv.CsvSchemaParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static org.talend.dataprep.api.type.Type.STRING;
import static org.talend.dataprep.api.dataset.Schema.Builder.parserResult;

/**
 * <p>
 * Analyzes the raw content of a dataset and determine its format (XLS, CSV...).
 * </p>
 *
 * <p>
 * It also parses column name information. Once analyzed, data prep would know how to access content.
 * </p>
 */
@Component
public class FormatAnalysis implements SynchronousDataSetAnalyzer {

    /** This class' header. */
    private static final Logger LOG = LoggerFactory.getLogger(FormatAnalysis.class);

    /** DataSet Metadata repository. */
    @Autowired
    private DataSetMetadataRepository repository;

    /** DataSet content store. */
    @Autowired
    private ContentStoreRouter store;

    @Autowired
    private DataprepSchema dataprepSchema;

    @Override
    public void analyze(String dataSetId) {

        if (StringUtils.isEmpty(dataSetId)) {
            throw new IllegalArgumentException("Data set id cannot be null or empty.");
        }

        final Marker marker = Markers.dataset(dataSetId);

        DistributedLock datasetLock = repository.createDatasetMetadataLock(dataSetId);
        datasetLock.lock();
        try {
            DataSetMetadata metadata = repository.get(dataSetId);
            if (metadata != null) {

                Format detectedFormat;
                try (InputStream content = store.getAsRaw(metadata, 10)) { // 10 line should be enough to detect format
                    detectedFormat = dataprepSchema.detect(IOUtils.toByteArray(content));
                } catch (IOException e) {
                    throw new TDPException(DataSetErrorCodes.UNABLE_TO_READ_DATASET_CONTENT, e);
                }

                LOG.debug(marker, "using {} to parse the dataset", detectedFormat);

                verifyFormat(detectedFormat);

                internalUpdateMetadata(metadata, detectedFormat);

                LOG.debug(marker, "format analysed for dataset");
            } else {
                LOG.info(marker, "Data set no longer exists.");
            }
        } finally {
            datasetLock.unlock();
        }
    }

    /**
     * Checks for format validity. Clean up and throw exception if the format is null or unsupported.
     *
     * @param detectedFormat the detected format of the dataset
     */
    private void verifyFormat(Format detectedFormat) {

        TDPException hypotheticalException = null;
        Set<Charset> supportedEncodings = EncodingSupport.getSupportedCharsets();
        if (detectedFormat == null) {
            hypotheticalException = new TDPException(DataSetErrorCodes.UNSUPPORTED_CONTENT);
        } else if (!supportedEncodings.contains(detectedFormat.getEncoding())) {
            hypotheticalException = new TDPException(DataSetErrorCodes.UNSUPPORTED_ENCODING);
        }
        if (hypotheticalException != null) {
            // Throw exception to indicate unsupported content
            throw hypotheticalException;
        }
    }

    /**
     * Update the given dataset metadata with the specified format.
     *
     * @param metadata the dataset metadata to update.
     * @param format the specified format used to update the dataset metadata
     */
    private void internalUpdateMetadata(DataSetMetadata metadata, Format format) {
        FormatFamily formatFamily = format.getFormatFamily();
        DataSetContent dataSetContent = metadata.getContent();

        final String mediaType = metadata.getLocation().toMediaType(format.getFormatFamily());
        dataSetContent.setFormatFamilyId(formatFamily.getBeanId());
        dataSetContent.setMediaType(mediaType);
        metadata.setEncoding(format.getEncoding().name());

        parseColumnNameInformation(metadata.getId(), metadata, format);

        repository.save(metadata);
    }

    /**
     * Update the dataset schema information from its metadata.
     *
     * @param original the original dataset metadata.
     * @param updated the dataset to update.
     */
    public void update(DataSetMetadata original, DataSetMetadata updated) {

        final Marker marker = Markers.dataset(updated.getId());

        FormatFamily formatFamily = dataprepSchema.getFormatFamily(original.getContent().getFormatFamilyId());

        if (formatFamily.getSchemaGuesser() instanceof CsvSchemaParser //
                && (updated.getContent() == null //
                        || !updated.getContent().getFormatFamilyId().equals(formatFamily.getBeanId()))) {
            LOG.debug(marker, "the schema cannot be updated");
            return;
        }

        // update the schema
        Format format = new Format(formatFamily, Charset.forName(updated.getEncoding()));
        internalUpdateMetadata(updated, format);

        LOG.debug(marker, "format updated for dataset");
    }

    /**
     * Parse and store column name information.
     *
     * @param dataSetId the dataset id
     * @param metadata the dataset metadata to parse
     * @param format the format
     */
    private void parseColumnNameInformation(String dataSetId, DataSetMetadata metadata, Format format) {

        final Marker marker = Markers.dataset(dataSetId);
        LOG.debug(marker, "Parsing column information...");
        try (InputStream content = store.getAsRaw(metadata, 10)) {
            SchemaParser parser = format.getFormatFamily().getSchemaGuesser();

            List<SheetContent> parseResult = parser.parse(new MetadataBasedFormatAnalysisRequest(content, metadata));

            Schema schema = parserResult().sheetContents(parseResult)
                    .draft(parseResult.size() > 1)
                    .sheetName(parseResult.size() > 1 ? parseResult.iterator().next().getName() : null)
                    .build();
            metadata.setSheetName(schema.getSheetName());
            metadata.setDraft(schema.draft());
            if (schema.draft()) {
                // Must select between available schemas
                metadata.setSchemaParserResult(schema);
                repository.save(metadata);
                LOG.info(Markers.dataset(dataSetId), "format analysed");
            } else if (schema.getSheetContents().isEmpty()) {
                // no schema found
                throw new IOException("Parser could not detect file format for " + metadata.getId());
            } else {
                metadata.getContent().setParameters(schema.getSheetContents().iterator().next().getParameters());
                // one schema found. Store it and proceed.
                metadata.getRowMetadata().setColumns(convertToApiColumns(schema.metadata()));
            }
        } catch (IOException e) {
            throw new TDPException(DataSetErrorCodes.UNABLE_TO_READ_DATASET_CONTENT, e);
        }
        LOG.debug(marker, "Parsed column information.");
    }

    public static List<ColumnMetadata> convertToApiColumns(List<SheetContent.ColumnMetadata> columnMetadata) {
        return columnMetadata.stream().map(FormatAnalysis::convertToApiColumn).collect(toList());
    }

    private static ColumnMetadata convertToApiColumn(SheetContent.ColumnMetadata cm) {
        return ColumnMetadata.Builder.column().id(cm.getId()).name(cm.getName()).type(STRING).headerSize(cm.getHeaderSize()).build();
    }

    @Override
    public int order() {
        return 0;
    }

}
