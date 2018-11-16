/*
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * https://github.com/Talend/data-prep/blob/master/LICENSE
 *
 * You should have received a copy of the agreement
 * along with this program; if not, write to Talend SA
 * 9 rue Pages 92150 Suresnes, France
 */

package org.talend.dataprep.command;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.springframework.cloud.sleuth.Span;
import org.springframework.http.HttpStatus;

public class HttpCallConfiguration<T> {

    private static final HttpStatus[] INFO_STATUS = Stream.of(HttpStatus.values()) //
            .filter(HttpStatus::is1xxInformational) //
            .collect(Collectors.toList()) //
            .toArray(new HttpStatus[0]);

    private static final HttpStatus[] SUCCESS_STATUS = Stream.of(HttpStatus.values()) //
            .filter(HttpStatus::is2xxSuccessful) //
            .collect(Collectors.toList()) //
            .toArray(new HttpStatus[0]);

    private static final HttpStatus[] REDIRECT_STATUS = Stream.of(HttpStatus.values()) //
            .filter(HttpStatus::is3xxRedirection) //
            .collect(Collectors.toList()) //
            .toArray(new HttpStatus[0]);

    private static final HttpStatus[] USER_ERROR_STATUS = Stream.of(HttpStatus.values()) //
            .filter(HttpStatus::is4xxClientError) //
            .collect(Collectors.toList()) //
            .toArray(new HttpStatus[0]);

    private static final HttpStatus[] SERVER_ERROR_STATUS = Stream.of(HttpStatus.values()) //
            .filter(HttpStatus::is5xxServerError) //
            .collect(Collectors.toList()) //
            .toArray(new HttpStatus[0]);

    public static <T> HttpCallConfiguration<T> call() {
        return new HttpCallConfiguration<>();
    }

    private Supplier<HttpRequestBase> httpRequestBase;

    private Map<HttpStatus, BiFunction<HttpUriRequest, HttpResponse, T>> behavior = new EnumMap<>(HttpStatus.class);

    private Function<Exception, T> onError = e -> {
        throw Defaults.passthrough().apply(e);
    };

    private final Map<String, String> headers = new HashMap<>();

    /**
     * Class used as tracking element for zipkin tag generation.
     */
    private RequestZipkinConfiguration requestZipkinConfiguration;

    /**
     * Supply a {@link HttpRequestBase} through a java 8 {@link Supplier}. The request creation will be done when the task is executed.
     *
     * @deprecated This allow initialization of supplier in constructors, before Spring autowiring. It is not the preferred way
     * with Hystrix use through AOP. Please use {@link #execute(HttpRequestBase)}.
     */
    // Using supplier allow the creation of the HTTP request AFTER the object containing its code has been initialized with Spring context.
    // Still it is not how it should be done and the other execute method should be privileged.
    // even if it has to be called in a @Postconstruct method
    @Deprecated
    public HttpCallConfiguration<T> execute(Supplier<HttpRequestBase> httpRequestBaseSupplier) {
        httpRequestBase = httpRequestBaseSupplier;
        return this;
    }

    /**
     * Set the request to be executed.
     */
    public HttpCallConfiguration<T> execute(HttpRequestBase httpRequestBase) {
        this.httpRequestBase = () -> httpRequestBase;
        return this;
    }

    /**
     * Handle exceptions which may happen when the request is executed.
     *
     * @param onError the handler
     * @return the configuration object for fluent configuration
     */
    public HttpCallConfiguration<T> onError(Function<Exception, T> onError) {
        this.onError = onError;
        return this;
    }

    /**
     * Handle exceptions which may happen when the request is executed by throwing another exception.
     *
     * @param onError the handler
     * @return the configuration object for fluent configuration
     */
    public HttpCallConfiguration<T> onErrorThrow(Function<Exception, RuntimeException> onError) {
        this.onError = e -> {
            throw onError.apply(e);
        };
        return this;
    }

    /**
     * @param headers
     * @return the configuration object for fluent configuration
     * @deprecated Headers should not be set by user.
     */
    @Deprecated
    public HttpCallConfiguration<T> withHeaders(final Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }

    /**
     * Add a reference class to be used to add a tag for Zipkin tracer.
     *
     * @param requestZipkinConfiguration
     * @return the configuration object for fluent configuration
     */
    public HttpCallConfiguration<T> withRequestZipkinConfiguration(RequestZipkinConfiguration requestZipkinConfiguration) {
        this.requestZipkinConfiguration = requestZipkinConfiguration;
        return this;
    }

    /**
     * Starts declaration of behavior(s) to adopt when HTTP response has status code <code>status</code>.
     *
     * @param status One of more HTTP {@link HttpStatus status(es)}.
     * @return A {@link BehaviorBuilder builder} to continue behavior declaration for the HTTP status(es).
     * @see BehaviorBuilder#then(BiFunction)
     */
    public BehaviorBuilder<T> on(HttpStatus... status) {
        return new BehaviorBuilder<>(status, this);
    }

    /**
     * Starts declaration of behavior(s) to adopt when HTTP response has status code of 1xx.
     *
     * @return A {@link BehaviorBuilder builder} to continue behavior declaration for the HTTP status(es).
     * @see BehaviorBuilder#then(BiFunction)
     */
    protected BehaviorBuilder onInfo() {
        return on(INFO_STATUS);
    }

    /**
     * Starts declaration of behavior(s) to adopt when HTTP response has status code of 2xx.
     *
     * @return A {@link BehaviorBuilder builder} to continue behavior declaration for the HTTP status(es).
     * @see BehaviorBuilder#then(BiFunction)
     */
    protected BehaviorBuilder onSuccess() {
        return on(SUCCESS_STATUS);
    }

    /**
     * Starts declaration of behavior(s) to adopt when HTTP response has status code of 3xx.
     *
     * @return A {@link BehaviorBuilder builder} to continue behavior declaration for the HTTP status(es).
     * @see BehaviorBuilder#then(BiFunction)
     */
    protected BehaviorBuilder onRedirect() {
        return on(REDIRECT_STATUS);
    }

    /**
     * Starts declaration of behavior(s) to adopt when HTTP response has status code of 4xx.
     *
     * @return A {@link BehaviorBuilder builder} to continue behavior declaration for the HTTP status(es).
     * @see BehaviorBuilder#then(BiFunction)
     */
    protected BehaviorBuilder onUserErrors() {
        return on(USER_ERROR_STATUS);
    }

    /**
     * Starts declaration of behavior(s) to adopt when HTTP response has status code of 5xx.
     *
     * @return A {@link BehaviorBuilder builder} to continue behavior declaration for the HTTP status(es).
     * @see BehaviorBuilder#then(BiFunction)
     */
    protected BehaviorBuilder onServerErrors() {
        return on(SERVER_ERROR_STATUS);
    }

    public HttpRequestBase getHttpRequestBase() {
        return httpRequestBase.get();
    }

    /**
     * The headers to add on the HTTP request.
     *
     * @return
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * The Map of all behaviors. Never null.
     */
    public Map<HttpStatus, BiFunction<HttpUriRequest, HttpResponse, T>> getBehavior() {
        return behavior;
    }

    /**
     * Directly get the behavior matching this status.
     *
     * @see #getBehavior()
     */
    public BiFunction<HttpUriRequest, HttpResponse, T> getBehaviorForStatus(HttpStatus status) {
        return behavior.get(status);
    }

    public Function<Exception, T> getOnError() {
        return onError;
    }

    public RequestZipkinConfiguration getRequestZipkinConfiguration() {
        return requestZipkinConfiguration;
    }

    public static final class BehaviorBuilder<T> {

        private final HttpStatus[] status;

        private final HttpCallConfiguration<T> configuration;

        public BehaviorBuilder(HttpStatus[] status, HttpCallConfiguration<T> configuration) {
            this.status = status;
            this.configuration = configuration;
        }

        /**
         * Declares what action should be performed for the given HTTP status(es).
         *
         * @param action A {@link BiFunction function} to be executed for given HTTP status(es).
         * @see Defaults
         */
        public HttpCallConfiguration<T> then(BiFunction<HttpUriRequest, HttpResponse, T> action) {
            for (HttpStatus currentStatus : status) {
                configuration.behavior.put(currentStatus, action);
            }
            return configuration;
        }
    }

    public static class RequestZipkinConfiguration {

        private final String spanName;

        private final Map<String, String> tags;

        private RequestZipkinConfiguration(String spanName, Map<String, String> tags) {
            this.spanName = spanName;
            this.tags = tags;
        }

        public static RequestZipkinConfiguration classBasedConfiguration(Class<?> clazz) {
            return new RequestZipkinConfiguration(classBasedSpanTagName(clazz),
                    Collections.singletonMap(Span.SPAN_LOCAL_COMPONENT_TAG_NAME, clazz.getName()));
        }

        private static String classBasedSpanTagName(Class<?> referenceClass) {
            final Package commandPackage = referenceClass.getPackage();
            final StringTokenizer tokenizer = new StringTokenizer(commandPackage.getName(), ".");
            final StringBuilder spanName = new StringBuilder();
            while (tokenizer.hasMoreTokens()) {
                spanName.append(String.valueOf(tokenizer.nextToken().charAt(0) + "."));
            }
            spanName.append(referenceClass.getSimpleName());

            return spanName.toString();
        }

        public String getSpanName() {
            return spanName;
        }

        public Map<String, String> getTags() {
            return tags;
        }
    }

}
