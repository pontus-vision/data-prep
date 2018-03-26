package org.talend.dataprep.schema.xls;

import org.talend.daikon.exception.error.ErrorCode;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public enum XlsErrorCodes implements ErrorCode {
    DATASET_HAS_TOO_MANY_COLUMNS(400, "number-of-columns", "max-allowed");

    /**
     * The http status to use.
     */
    private int httpStatus;

    /**
     * Expected entries to be in the context.
     */
    private List<String> expectedContextEntries;

    /**
     * default constructor.
     *
     * @param httpStatus     the http status to use.
     * @param contextEntries expected context entries.
     */
    XlsErrorCodes(int httpStatus, String... contextEntries) {
        this.httpStatus = httpStatus;
        this.expectedContextEntries = Arrays.asList(contextEntries);
    }

    /**
     * @return the product.
     */
    @Override
    public String getProduct() {
        return "TDP"; //$NON-NLS-1$
    }

    /**
     * @return the group.
     */
    @Override
    public String getGroup() {
        return "PARSER"; //$NON-NLS-1$
    }

    /**
     * @return the http status.
     */
    @Override
    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * @return the expected context entries.
     */
    @Override
    public Collection<String> getExpectedContextEntries() {
        return expectedContextEntries;
    }

    @Override
    public String getCode() {
        return this.toString();
    }
}
