/*
 * (c) Copyright 2020 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.tracing.undertow;

import com.palantir.logsafe.Preconditions;
import com.palantir.tracing.DetachedSpan;
import com.palantir.tracing.TagTranslator;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * Extracts Zipkin-style trace information from the given HTTP request and sets up a corresponding {@link DetachedSpan}
 * to span the entire request. See <a href="https://github.com/openzipkin/b3-propagation">b3-propagation</a>.
 *
 * <p>This handler should be registered as early as possible in the request lifecycle to fully encapsulate all work.
 *
 * <p>If this handler is registered multiple times in the handler chain, subsequent executions are ignored to preserve
 * the first, most accurate span.
 */
public final class TracedRequestHandler implements HttpHandler {

    private static final String DEFAULT_OPERATION_NAME = "Undertow Request";

    private final HttpHandler delegate;
    private final String operationName;
    private final TagTranslator<? super HttpServerExchange> translator;

    public TracedRequestHandler(
            HttpHandler delegate, String operationName, TagTranslator<? super HttpServerExchange> translator) {
        this.delegate = Preconditions.checkNotNull(delegate, "HttpHandler is required");
        this.operationName = Preconditions.checkNotNull(operationName, "Operation name is required");
        this.translator = Preconditions.checkNotNull(translator, "TagTranslator is required");
    }

    public TracedRequestHandler(HttpHandler delegate, TagTranslator<? super HttpServerExchange> translator) {
        this(delegate, DEFAULT_OPERATION_NAME, translator);
    }

    public TracedRequestHandler(HttpHandler delegate) {
        this(delegate, StatusCodeTagTranslator.INSTANCE);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        UndertowTracing.getOrInitializeRequestTrace(exchange, operationName, translator);
        delegate.handleRequest(exchange);
    }

    @Override
    public String toString() {
        return "TracedRequestHandler{delegate=" + delegate + ", operationName='" + operationName + "'}";
    }
}
