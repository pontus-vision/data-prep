package org.talend.dataprep.command;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.cloud.sleuth.Span.SPAN_NAME_NAME;

import java.io.IOException;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.SpanInjector;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.talend.daikon.exception.ExceptionContext;
import org.talend.daikon.exception.error.ErrorCode;
import org.talend.daikon.exception.json.JsonErrorCode;
import org.talend.dataprep.conversions.BeanConversionService;
import org.talend.dataprep.exception.ErrorCodeDto;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.TdpExceptionDto;
import org.talend.dataprep.exception.error.CommonErrorCodes;
import org.talend.dataprep.security.Security;

@Component
public class DataprepHttpClient {

    private static final Logger LOGGER = getLogger(DataprepHttpClient.class);

    /** The http client. */
    @Autowired
    protected HttpClient client;

    /** Jackson object mapper to handle json. */
    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private BeanConversionService conversionService;

    /** Spring application context. */
    @Autowired
    protected ApplicationContext context;

    @Autowired
    private Tracer tracer;

    /**
     * Runs a data prep command with the following steps:
     *
     * @return A instance of <code>T</code>.
     */
    public <T> HttpCallResult<T> execute(HttpCallConfiguration<T> configuration) {
        final HttpUriRequest request = configuration.getHttpRequestBase();
        addCommandHeaders(configuration, request); // insert all the provided headers in the request
        addAuthorizationHeaders(request); // update request header with security token
        addLocaleHeaders(request); // Forward locale to target
        final Span requestSpan = addTrackingHeaders(request, configuration.getRequestZipkinConfiguration()); // Inject tracing stuff

        try {
            final HttpResponse response;
            try {
                LOGGER.trace("Requesting {} {}", request.getMethod(), request.getURI());
                response = client.execute(request);
            } catch (Exception e) {
                return handleUnexpectedError(configuration, e);
            }
            Header[] commandResponseHeaders = response.getAllHeaders();

            HttpStatus status = HttpStatus.valueOf(response.getStatusLine().getStatusCode());

            Header cookies = response.getFirstHeader("Set-Cookie");
            if (cookies != null) {
                LOGGER.warn(
                        "request {} {}: Cookie detected in responseHeaders (check security.oauth2.resource.uri settings)",
                        request.getMethod(), request.getURI());
            }

            BiFunction<HttpUriRequest, HttpResponse, T> handler = //
                    getResponseHandlingFunction(configuration, request, response, status);

            // application of handler must be able to throw exception on purpose without being bothered by onError wrapping
            T result = handler.apply(request, response);
            return new HttpCallResult<>(result, status, commandResponseHeaders);
        } finally {
            tracer.close(requestSpan);
        }
    }

    private <T> HttpCallResult<T> handleUnexpectedError(HttpCallConfiguration<T> configuration, Exception e) {
        Function<Exception, T> onError = configuration.getOnError();
        if (onError != null) {
            return new HttpCallResult<>(onError.apply(e), null, null);
        } else {
            throw new TDPException(CommonErrorCodes.UNEXPECTED_EXCEPTION, e);
        }
    }

    private <T> BiFunction<HttpUriRequest, HttpResponse, T> getResponseHandlingFunction(
            HttpCallConfiguration<T> configuration, HttpUriRequest request, HttpResponse response, HttpStatus status) {
        // do we have a behavior for this status code (even an error) ?
        BiFunction<HttpUriRequest, HttpResponse, T> function = configuration.getBehaviorForStatus(status);
        if (function == null) {
            // handle response's HTTP status
            if (status.is4xxClientError() || status.is5xxServerError()) {
                LOGGER.trace("request {} {} : response on error {}", request.getMethod(), request.getURI(),
                        response.getStatusLine());
                // Http status >= 400 so apply onError behavior

                function = handleRemoteServerHttpError(configuration.getOnError());
            } else {
                // Http status is not error so apply onError behavior
                function = missingBehavior();
            }
        }
        return function;
    }

    /**
     * @return A {@link BiFunction} to handle missing behavior definition for HTTP response's code.
     */
    private static <T> BiFunction<HttpUriRequest, HttpResponse, T> missingBehavior() {
        return (req, res) -> {
            LOGGER.error("Unable to process message for request {} (response code: {}).", req,
                    res.getStatusLine().getStatusCode());
            return null;
        };
    }

    private <T> BiFunction<HttpUriRequest, HttpResponse, T>
            handleRemoteServerHttpError(Function<Exception, T> onError) {
        return (httpRequestBase, httpResponse) -> handleRemoteServerHttpError(onError, httpRequestBase, httpResponse);
    }

    private <T> T handleRemoteServerHttpError(Function<Exception, T> onError, HttpUriRequest request,
            HttpResponse response) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("request on error {} -> {}", request.toString(), response.getStatusLine());
        }
        final int statusCode = response.getStatusLine().getStatusCode();
        String content = org.apache.commons.lang3.StringUtils.EMPTY;
        TDPException exception;
        try {
            if (response.getEntity() != null) {
                content = IOUtils.toString(response.getEntity().getContent(), UTF_8);
                LOGGER.trace("Error received {}", content);
                TdpExceptionDto exceptionDto = objectMapper.readValue(content, TdpExceptionDto.class);
                try {
                    exception = conversionService.convert(exceptionDto, TDPException.class);
                } catch (RuntimeException e) {
                    exception = new TDPException(CommonErrorCodes.UNEXPECTED_EXCEPTION, null, content,
                            "Remote service returned an unhandled error and response could not be deserialized.",
                            ExceptionContext.build());
                }
                ErrorCode code = exception.getCode();
                if (code instanceof ErrorCodeDto) {
                    ((ErrorCodeDto) code).setHttpStatus(statusCode);
                }
            } else {
                LOGGER.trace("Error received with no payload.");
                exception = new TDPException(new UnexpectedErrorCode(statusCode));
            }
        } catch (JsonProcessingException e) {
            LOGGER.debug("Cannot parse response content as JSON with content '" + content + "'", e);
            // Failed to parse JSON error, returns an unexpected code with returned HTTP code
            exception = new TDPException(new UnexpectedErrorCode(statusCode));
        } catch (IOException e) {
            LOGGER.error("Unexpected error message: {}", buildRequestReport(request, response));
            throw new TDPException(CommonErrorCodes.UNEXPECTED_EXCEPTION, e);
        }
        return onError.apply(exception);
    }

    private static String buildRequestReport(HttpUriRequest req, HttpResponse res) {
        StringBuilder builder = new StringBuilder("{request:{\n");
        builder.append("uri:").append(req.getURI()).append(",\n");
        builder.append("request:").append(req.getRequestLine()).append(",\n");
        builder.append("method:").append(req.getMethod()).append(",\n");
        if (req instanceof HttpEntityEnclosingRequestBase) {
            try {
                builder
                        .append("load:")
                        .append(IOUtils.toString(((HttpEntityEnclosingRequestBase) req).getEntity().getContent(),
                                UTF_8))
                        .append(",\n");
            } catch (IOException e) {
                // We ignore the field
            }
        }
        builder.append("}, response:{\n");
        try {
            builder.append(IOUtils.toString(res.getEntity().getContent(), UTF_8));
        } catch (IOException e) {
            // We ignore the field
        }
        builder.append("}\n}");
        return builder.toString();
    }

    private Span addTrackingHeaders(HttpRequest request,
            HttpCallConfiguration.RequestZipkinConfiguration requestZipkinConfiguration) {
        String spanNameResult = requestZipkinConfiguration.getSpanName();
        final Span requestSpan = tracer.createSpan(spanNameResult, tracer.getCurrentSpan());

        for (Map.Entry<String, String> tagEntry : requestZipkinConfiguration.getTags().entrySet()) {
            requestSpan.tag(tagEntry.getKey(), tagEntry.getValue());
        }

        final SpanInjector<HttpRequest> injector = new HttpRequestBaseSpanInjector(
                requestZipkinConfiguration.getTags().get(Span.SPAN_LOCAL_COMPONENT_TAG_NAME));
        injector.inject(requestSpan, request);
        return requestSpan;
    }

    private void addLocaleHeaders(HttpRequest request) {
        request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, LocaleContextHolder.getLocale().toLanguageTag());
    }

    private void addAuthorizationHeaders(HttpRequest request) {
        String authenticationToken = context.getBean(Security.class).getAuthenticationToken();
        if (StringUtils.isNotBlank(authenticationToken) && request.getHeaders(HttpHeaders.AUTHORIZATION).length == 0) {
            request.addHeader(HttpHeaders.AUTHORIZATION, authenticationToken);
        } else {
            // Intentionally left as debug to prevent log flood in open source edition.
            LOGGER.debug("No current authentication token for {}.", this.getClass());
        }
    }

    private <T> void addCommandHeaders(HttpCallConfiguration<T> conf, HttpRequest request) {
        Map<String, String> headers = conf.getHeaders();
        if (headers.size() > 0) {
            headers.forEach(request::addHeader);
        }
    }

    /**
     * A {@link SpanInjector} implementation dedicated to inject tracing headers for {@link HttpRequestBase} objects.
     */
    private static class HttpRequestBaseSpanInjector implements SpanInjector<HttpRequest> {

        private String localComponentTagName;

        private HttpRequestBaseSpanInjector(String localComponentTagName) {
            this.localComponentTagName = localComponentTagName;
        }

        @Override
        public void inject(Span span, HttpRequest httpRequestBase) {
            this.setIdHeader(httpRequestBase, Span.TRACE_ID_NAME, span.getTraceId());
            this.setIdHeader(httpRequestBase, Span.SPAN_ID_NAME, span.getSpanId());
            this.setHeader(httpRequestBase, Span.SAMPLED_NAME, span.isExportable() ? "1" : "0");
            this.setHeader(httpRequestBase, SPAN_NAME_NAME, span.getName());
            this.setIdHeader(httpRequestBase, Span.PARENT_ID_NAME, this.getParentId(span));
            this.setHeader(httpRequestBase, Span.PROCESS_ID_NAME, span.getProcessId());
            this.setHeader(httpRequestBase, Span.SPAN_LOCAL_COMPONENT_TAG_NAME, localComponentTagName);
        }

        private void setHeader(HttpRequest request, String name, String value) {
            if (StringUtils.isNotBlank(value) && !request.containsHeader(name)) {
                request.addHeader(name, value);
            }
        }

        private void setIdHeader(HttpRequest request, String name, Long value) {
            if (value != null) {
                this.setHeader(request, name, Span.idToHex(value));
            }
        }

        private Long getParentId(Span span) {
            return !span.getParents().isEmpty() ? span.getParents().get(0) : null;
        }
    }

    private static class UnexpectedErrorCode extends JsonErrorCode {

        private final int statusCode;

        UnexpectedErrorCode(int statusCode) {
            this.statusCode = statusCode;
        }

        @Override
        public String getProduct() {
            return CommonErrorCodes.UNEXPECTED_EXCEPTION.getProduct();
        }

        @Override
        public String getCode() {
            return CommonErrorCodes.UNEXPECTED_EXCEPTION.getCode();
        }

        @Override
        public int getHttpStatus() {
            return statusCode;
        }
    }
}
