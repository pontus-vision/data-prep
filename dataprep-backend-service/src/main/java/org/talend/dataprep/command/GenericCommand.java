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

package org.talend.dataprep.command;

import static org.talend.dataprep.command.HttpCallConfiguration.RequestZipkinConfiguration.classBasedConfiguration;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpRequestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.talend.dataprep.command.HttpCallConfiguration.BehaviorBuilder;
import org.talend.dataprep.exception.TDPException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

/**
 * Base Hystrix command request for all DataPrep commands.
 *
 * @param <T> Command result type.
 */
public class GenericCommand<T> extends HystrixCommand<T> {

    /** Hystrix group used for dataset related commands. */
    public static final HystrixCommandGroupKey DATASET_GROUP = HystrixCommandGroupKey.Factory.asKey("dataset");

    /** Hystrix group used for preparation related commands. */
    public static final HystrixCommandGroupKey PREPARATION_GROUP = HystrixCommandGroupKey.Factory.asKey("preparation");

    /** Hystrix group used for transformation related commands. */
    public static final HystrixCommandGroupKey TRANSFORM_GROUP = HystrixCommandGroupKey.Factory.asKey("transform");

    /** Hystrix group used for transformation related commands. */
    public static final HystrixCommandGroupKey FULLRUN_GROUP = HystrixCommandGroupKey.Factory.asKey("fullrun");

    /** Hystrix group used for async related commands */
    public static final HystrixCommandGroupKey ASYNC_GROUP = HystrixCommandGroupKey.Factory.asKey("async");

    /** Hystrix group used for user related commands */
    public static final HystrixCommandGroupKey USER_GROUP = HystrixCommandGroupKey.Factory.asKey("user");

    /** Spring application context. */
    @Autowired
    protected ApplicationContext context;

    /** Transformation service URL. */
    @Value("${transformation.service.url:}")
    protected String transformationServiceUrl;

    /** Full run service URL. */
    @Value("${fullrun.service.url:}")
    protected String fullRunServiceUrl;

    /** Dataset service URL. */
    @Value("${dataset.service.url:}")
    protected String datasetServiceUrl;

    /** Preparation service URL. */
    @Value("${preparation.service.url:}")
    protected String preparationServiceUrl;

    // --- the real HTTP client that would be usable outside Hystrix context ---

    @Autowired
    private DataprepHttpClient dataprepHttpClientDelegate;

    // --- stateful Hystrix command fields ---

    // config render the class stateful but it can be easily refactored with IDE tools (#inline)
    protected final HttpCallConfiguration<T> configuration = new HttpCallConfiguration<>();

    // this (and config) is what render commands stateful. If we could stop using it it would be so great!
    private HttpCallResult<T> callResult;

    /** Jackson object mapper to handle json.
     * Should be avoided as much as possible in preference to object mapping using {@link Defaults} */
    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Protected constructor.
     *
     * @param group the command group.
     */
    protected GenericCommand(final HystrixCommandGroupKey group) {
        super(group);
        this.configuration.withRequestZipkinConfiguration(classBasedConfiguration(getClass()));
    }

    protected GenericCommand(final HystrixCommandGroupKey group, final Map<String, String> headers) {
        this(group);
        this.configuration.withHeaders(headers);
    }

    @Override
    protected RuntimeException decomposeException(Exception e) {
        Throwable current = e;
        while (current.getCause() != null) {
            if (current instanceof TDPException) {
                break;
            }
            current = current.getCause();
        }
        if (current instanceof TDPException) {
            return (TDPException) current;
        } else {
            return super.decomposeException(e);
        }
    }

    /**
     * Runs a data prep command with the following steps:
     * <ul>
     * <li>Gets the HTTP command to execute (see {@link #execute(Supplier)}.</li>
     * <li>Gets the behavior to adopt based on returned HTTP code (see {@link #on(HttpStatus...)}).</li>
     * <li>If no behavior was defined for returned code, returns an error as defined in {@link #onError(Function)}</li>
     * <li>If a behavior was defined, invokes defined behavior.</li>
     * </ul>
     *
     * @return A instance of <code>T</code>.
     */
    @Override
    protected T run() {
        callResult = dataprepHttpClientDelegate.execute(configuration);
        return callResult.getResult();
    }

    /**
     * @return the CommandResponseHeader
     */
    public Header[] getCommandResponseHeaders() {
        return callResult.getCommandResponseHeaders();
    }

    public Header getHeader(String name) {
        return callResult.getHeader(name);
    }

    /**
     * @return The HTTP status of the executed request.
     */
    public HttpStatus getStatus() {
        return callResult.getHttpStatus();
    }

    /**
     * Declares what exception should be thrown in case of error.
     *
     * @param onError A {@link Function function} that returns a {@link RuntimeException}.
     * @see TDPException
     */
    protected void onError(Function<Exception, RuntimeException> onError) {
        configuration.onErrorThrow(onError);
    }

    /**
     * Declares which {@link HttpRequestBase http request} to execute in command.
     *
     * @param call The {@link Supplier} to provide the {@link HttpRequestBase} to execute.
     */
    protected void execute(Supplier<HttpRequestBase> call) {
        configuration.execute(call);
    }

    /**
     * Starts declaration of behavior(s) to adopt when HTTP response has status code <code>status</code>.
     *
     * @param status One of more HTTP {@link HttpStatus status(es)}.
     * @return A {@link BehaviorBuilder builder} to continue behavior declaration for the HTTP status(es).
     * @see BehaviorBuilder#then(BiFunction)
     */
    protected BehaviorBuilder<T> on(HttpStatus... status) {
        return configuration.on(status);
    }

    /**
     * Starts declaration of behavior(s) to adopt when HTTP response has status code of 1xx.
     *
     * @return A {@link BehaviorBuilder builder} to continue behavior declaration for the HTTP status(es).
     * @see BehaviorBuilder#then(BiFunction)
     */
    protected BehaviorBuilder onInfo() {
        return configuration.onInfo();
    }

    /**
     * Starts declaration of behavior(s) to adopt when HTTP response has status code of 2xx.
     *
     * @return A {@link BehaviorBuilder builder} to continue behavior declaration for the HTTP status(es).
     * @see BehaviorBuilder#then(BiFunction)
     */
    protected BehaviorBuilder onSuccess() {
        return configuration.onSuccess();
    }

    /**
     * Starts declaration of behavior(s) to adopt when HTTP response has status code of 3xx.
     *
     * @return A {@link BehaviorBuilder builder} to continue behavior declaration for the HTTP status(es).
     * @see BehaviorBuilder#then(BiFunction)
     */
    protected BehaviorBuilder onRedirect() {
        return configuration.onRedirect();
    }

    /**
     * Starts declaration of behavior(s) to adopt when HTTP response has status code of 4xx.
     *
     * @return A {@link BehaviorBuilder builder} to continue behavior declaration for the HTTP status(es).
     * @see BehaviorBuilder#then(BiFunction)
     */
    protected BehaviorBuilder onUserErrors() {
        return configuration.onUserErrors();
    }

    /**
     * Starts declaration of behavior(s) to adopt when HTTP response has status code of 5xx.
     *
     * @return A {@link BehaviorBuilder builder} to continue behavior declaration for the HTTP status(es).
     * @see BehaviorBuilder#then(BiFunction)
     */
    protected BehaviorBuilder onServerErrors() {
        return configuration.onServerErrors();
    }

    protected String getServiceUrl(ServiceType type) {
        switch (type) {
        case DATASET:
            return datasetServiceUrl;
        case TRANSFORMATION:
        case TRANSFORM:
            return transformationServiceUrl;
        case PREPARATION:
            return preparationServiceUrl;
        case FULLRUN:
            return fullRunServiceUrl;
        default:
            throw new IllegalArgumentException("Type '" + type + "' is not supported.");
        }
    }

    public enum ServiceType {
        DATASET,
        TRANSFORMATION,
        TRANSFORM,
        PREPARATION,
        FULLRUN,

    }

}
