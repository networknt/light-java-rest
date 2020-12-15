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

package com.networknt.openapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * This utility normalize the RESTful API path so that they can be
 * handled identically.
 *
 * @author Steve Hu
 */
public class ApiNormalisedPath implements NormalisedPath {
    Logger logger = LoggerFactory.getLogger(ApiNormalisedPath.class);
    private final List<String> pathParts;
    private final String original;
    private final String normalised;

    public ApiNormalisedPath(final String path) {
        if (logger.isDebugEnabled()) logger.debug("path =" + path);
        this.original = requireNonNull(path, "A path is required");
        this.normalised = normalise(path);
        if (logger.isDebugEnabled()) logger.debug("normalised = " + this.normalised);
        this.pathParts = unmodifiableList(asList(normalised.split("/")));
    }

    @Override
    public List<String> parts() {
        return pathParts;
    }

    @Override
    public String part(int index) {
        return pathParts.get(index);
    }

    @Override
    public boolean isParam(int index) {
        final String part = part(index);
        return part.startsWith("{") && part.endsWith("}");
    }

    @Override
    public String paramName(int index) {
        if (!isParam(index)) {
            return null;
        }
        final String part = part(index);
        return part.substring(1, part.length() - 1);
    }

    @Override
    public String original() {
        return original;
    }

    @Override
    public String normalised() {
        return normalised;
    }

    private String normalise(String requestPath) {
        if(OpenApiHelper.openApi3 != null && OpenApiHelper.basePath.length() > 0) {
            requestPath = requestPath.replaceFirst(OpenApiHelper.basePath, "");
        }
        if (!requestPath.startsWith("/")) {
            return "/" + requestPath;
        }
        return requestPath;
    }
}
