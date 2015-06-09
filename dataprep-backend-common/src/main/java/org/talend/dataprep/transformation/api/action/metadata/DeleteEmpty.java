package org.talend.dataprep.transformation.api.action.metadata;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.talend.dataprep.api.dataset.ColumnMetadata;

/**
 * Delete row when value is empty.
 */
@Component(DeleteEmpty.ACTION_BEAN_PREFIX + DeleteEmpty.DELETE_EMPTY_ACTION_NAME)
public class DeleteEmpty extends AbstractDelete {

    /** The action name. */
    public static final String DELETE_EMPTY_ACTION_NAME = "delete_empty"; //$NON-NLS-1$

    /**
     * @see ActionMetadata#getName()
     */
    @Override
    public String getName() {
        return DELETE_EMPTY_ACTION_NAME;
    }

    /**
     * @see AbstractDelete#toDelete(Map, String)
     */
    @Override
    public boolean toDelete(Map<String, String> parsedParameters, String value) {
        return value == null || value.trim().length() == 0;
    }

    /**
     * @see ActionMetadata#accept(ColumnMetadata)
     */
    @Override
    public boolean accept(ColumnMetadata column) {
        // allow on all columns
        return true;
    }

}
