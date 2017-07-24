package org.talend.dataprep.transformation.service;

import org.talend.dataprep.api.dataset.RowMetadata;

public interface StepMetadataRepository {
    RowMetadata get(String stepId);

    void update(String stepId, RowMetadata rowMetadata);
}
