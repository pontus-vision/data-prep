package org.talend.dataprep.command;

import org.apache.http.Header;
import org.springframework.http.HttpStatus;

public class HttpCallResult<T> {

    private final T result;

    private final HttpStatus httpStatus;

    /** Headers of the response received by the command. Set in the run command. */
    private final Header[] commandResponseHeaders;

    public HttpCallResult(T result, HttpStatus httpStatus, Header[] commandResponseHeaders) {
        this.result = result;
        this.httpStatus = httpStatus;
        this.commandResponseHeaders = commandResponseHeaders;
    }

    public T getResult() {
        return result;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public Header[] getCommandResponseHeaders() {
        return commandResponseHeaders;
    }

    public Header getHeader(String name) {
        for (Header header : commandResponseHeaders) {
            if (header.getName().equals(name)) {
                return header;
            }
        }
        return null;
    }
}
