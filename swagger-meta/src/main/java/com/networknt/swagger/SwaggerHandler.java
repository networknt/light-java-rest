/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.swagger;

import com.networknt.audit.AuditHandler;
import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.status.Status;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This is the handler that parses the swagger object based on uri and method
 * of the request and attached the operation to the request so that security
 * and validator can use it without parsing it.
 *
 * @author Steve Hu
 */
public class SwaggerHandler implements MiddlewareHandler {
    static final Logger logger = LoggerFactory.getLogger(SwaggerHandler.class);

    public static final String CONFIG_NAME = "swagger";

    static final String STATUS_INVALID_REQUEST_PATH = "ERR10007";
    static final String STATUS_METHOD_NOT_ALLOWED = "ERR10008";

    private volatile HttpHandler next;

    public SwaggerHandler() {

    }


    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {

        final NormalisedPath requestPath = new ApiNormalisedPath(exchange.getRequestURI());
        final Optional<NormalisedPath> maybeApiPath = SwaggerHelper.findMatchingApiPath(requestPath);
        if (!maybeApiPath.isPresent()) {
            setExchangeStatus(exchange, STATUS_INVALID_REQUEST_PATH, requestPath.normalised());
            return;
        }

        final NormalisedPath swaggerPathString = maybeApiPath.get();
        final Path swaggerPath = SwaggerHelper.swagger.getPath(swaggerPathString.original());

        final HttpMethod httpMethod = HttpMethod.valueOf(exchange.getRequestMethod().toString());
        final Operation operation = swaggerPath.getOperationMap().get(httpMethod);

        if (operation == null) {
            setExchangeStatus(exchange, STATUS_METHOD_NOT_ALLOWED);
            return;
        }

        // This handler can identify the swaggerOperation and endpoint only. Other info will be added by JwtVerifyHandler.
        final SwaggerOperation swaggerOperation = new SwaggerOperation(swaggerPathString, swaggerPath, httpMethod, operation);
        String endpoint = swaggerPathString.normalised() + "@" + httpMethod.toString().toLowerCase();
        Map<String, Object> auditInfo = new HashMap<>();
        auditInfo.put(Constants.ENDPOINT_STRING, endpoint);
        auditInfo.put(Constants.SWAGGER_OPERATION_STRING, swaggerOperation);
        exchange.putAttachment(AuditHandler.AUDIT_INFO, auditInfo);

        Handler.next(exchange, next);
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(final HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        // just check if swagger.json exists or not.
        return (SwaggerHelper.swagger != null);
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(SwaggerHandler.class.getName(), Config.getInstance().getJsonMapConfig(CONFIG_NAME), null);
    }

}
