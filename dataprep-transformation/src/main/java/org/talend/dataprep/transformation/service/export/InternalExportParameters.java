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

package org.talend.dataprep.transformation.service.export;

import org.talend.dataprep.api.dataset.DataSet;
import org.talend.dataprep.api.export.ExportParameters;
import org.talend.dataprep.api.preparation.PreparationDTO;
import org.talend.dataprep.format.export.ExportFormat;

import java.util.HashMap;
import java.util.Map;

/**
 * Parameter for dataset/preparation format
 */
class InternalExportParameters {

    private String exportType;

    private ExportFormat format;

    private String preparationId;

    private PreparationDTO preparation;

    private String stepId;

    private String datasetId;

    private ExportParameters.SourceType from;

    private String exportName;

    private Map<String, String> arguments = new HashMap<>();

    private String filter;

    private DataSet content;

    public String getExportType() {
        return exportType;
    }

    public void setExportType(String exportType) {
        this.exportType = exportType;
    }

    /**
     * The export format.
     */
    public ExportFormat getFormat() {
        return format;
    }

    public void setFormat(ExportFormat format) {
        this.format = format;
    }

    public String getPreparationId() {
        return preparationId;
    }

    public void setPreparationId(String preparationId) {
        this.preparationId = preparationId;
    }

    /**
     * The preparation id to format. If this is null, datasetId must be set.
     */
    public PreparationDTO getPreparation() {
        return preparation;
    }

    public void setPreparation(PreparationDTO preparation) {
        this.preparation = preparation;
    }

    /**
     * The step id to format at a specific state.
     */
    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    /**
     * The dataset id to format.
     */
    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    /**
     * Where should the data come from.
     */
    public ExportParameters.SourceType getFrom() {
        return from;
    }

    public void setFrom(ExportParameters.SourceType from) {
        this.from = from;
    }

    public String getExportName() {
        return exportName;
    }

    public void setExportName(String exportName) {
        this.exportName = exportName;
    }

    public Map<String, String> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, String> arguments) {
        this.arguments = arguments;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    /**
     * Raw content that may be supplied by user.
     * Maybe should be just a Stream<DatasetRow>
     */
    public DataSet getContent() {
        return content;
    }

    public void setContent(DataSet content) {
        this.content = content;
    }
}
