package com.networknt.openapi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.config.Config;
import com.networknt.jsonoverlay.Overlay;
import com.networknt.oas.model.*;
import com.networknt.oas.model.impl.SchemaImpl;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class ResponseValidator {
    private final SchemaValidator schemaValidator;
    private final SchemaValidatorsConfig config;
    private static final String VALIDATOR_RESPONSE_CONTENT_UNEXPECTED = "ERR11018";
    private static final String JSON_MEDIA_TYPE = "application/json";
    private static final String GOOD_STATUS_CODE = "200";
    private static final String DEFAULT_STATUS_CODE = "default";
    private static final Logger logger = LoggerFactory.getLogger(ResponseValidator.class);

    /**
     * Construct a new request validator with the given schema validator.
     */
    public ResponseValidator(SchemaValidatorsConfig config) {
        this.schemaValidator = new SchemaValidator(OpenApiHelper.openApi3);
        this.config = config;
    }

    public ResponseValidator() {
        this.schemaValidator = new SchemaValidator(OpenApiHelper.openApi3);
        this.config = new SchemaValidatorsConfig();
    }

    /**
     * validate a given response content object with status code "200" and media content type "application/json"
     * uri, httpMethod, JSON_MEDIA_TYPE("200"), DEFAULT_MEDIA_TYPE("application/json") is to locate the schema to validate
     * @param responseContent response content needs to be validated
     * @param uri original uri of the request
     * @param httpMethod eg. "put" or "get"
     * @return Status
     */
    public Status validateResponseContent(Object responseContent, String uri, String httpMethod) {
        return validateResponseContent(responseContent, uri, httpMethod, GOOD_STATUS_CODE);
    }

    /**
     * validate a given response content object with media content type "application/json"
     * uri, httpMethod, statusCode, DEFAULT_MEDIA_TYPE is to locate the schema to validate
     * @param responseContent response content needs to be validated
     * @param uri original uri of the request
     * @param httpMethod eg. "put" or "get"
     * @param statusCode eg. 200, 400
     * @return Status
     */
    public Status validateResponseContent(Object responseContent, String uri, String httpMethod, String statusCode) {
        return validateResponseContent(responseContent, uri, httpMethod, statusCode, JSON_MEDIA_TYPE);
    }

    /**
     * validate a given response content object with schema coordinate (uri, httpMethod, statusCode, mediaTypeName)
     * uri, httpMethod, statusCode, mediaTypeName is to locate the schema to validate
     * @param responseContent response content needs to be validated
     * @param uri original uri of the request
     * @param httpMethod eg. "put" or "get"
     * @param statusCode eg. 200, 400
     * @param mediaTypeName eg. "application/json"
     * @return Status
     */
    public Status validateResponseContent(Object responseContent, String uri, String httpMethod, String statusCode, String mediaTypeName) {
        OpenApiOperation operation = null;
        try {
            operation = getOpenApiOperation(uri, httpMethod);
        } catch (URISyntaxException e) {
            logger.error(e.getMessage());
            return new Status(VALIDATOR_RESPONSE_CONTENT_UNEXPECTED, httpMethod, uri);
        }
        if(operation == null) {
            return new Status(VALIDATOR_RESPONSE_CONTENT_UNEXPECTED, httpMethod, uri);
        }
        return validateResponseContent(responseContent, operation, statusCode, mediaTypeName);
    }

    /**
     * validate a given response content object
     * @param responseContent response content needs to be validated
     * @param openApiOperation OpenApi Operation which is located by uri and httpMethod
     * @param statusCode eg. 200, 400
     * @param mediaTypeName eg. "application/json"
     * @return Status
     */
    public Status validateResponseContent(Object responseContent, OpenApiOperation openApiOperation, String statusCode, String mediaTypeName) {
        //try to convert json string to structured object
        if(responseContent instanceof String) {
            responseContent = convertStrToObjTree((String)responseContent);
        }
        JsonNode schema = getContentSchema(openApiOperation, statusCode, mediaTypeName);
        //if cannot find schema based on status code, try to get from "default"
        if(schema == null || schema.equals("")) {
            // if corresponding response exist but also does not contain any schema, pass validation
            if (openApiOperation.getOperation().getResponses().containsKey(String.valueOf(statusCode))) {
                return null;
            }
            schema = getContentSchema(openApiOperation, DEFAULT_STATUS_CODE, mediaTypeName);
            // if default also does not contain any schema, pass validation
            if (schema == null) return null;
        }
        if ((responseContent != null && schema == null) ||
                (responseContent == null && schema != null)) {
            return new Status(VALIDATOR_RESPONSE_CONTENT_UNEXPECTED, openApiOperation.getMethod(), openApiOperation.getPathString().original());
        }
        config.setTypeLoose(false);
        return schemaValidator.validate(responseContent, schema, config);
    }

    /**
     * try to convert a string with json style to a structured object.
     * @param s
     * @return Object
     */
    private Object convertStrToObjTree(String s) {
        Object contentObj = null;
        if (s != null) {
            s = s.trim();
            try {
                if (s.startsWith("{")) {
                    contentObj = Config.getInstance().getMapper().readValue(s, new TypeReference<HashMap<String, Object>>() {
                    });
                } else if (s.startsWith("[")) {
                    contentObj = Config.getInstance().getMapper().readValue(s, new TypeReference<List<Object>>() {
                    });
                } else {
                    logger.error("cannot deserialize json str: {}", s);
                    return null;
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
        return contentObj;
    }

    /**
     * locate operation based on uri and httpMethod
     * @param uri original uri of the request
     * @param httpMethod http method of the request
     * @return OpenApiOperation the wrapper of an api operation
     */
    private OpenApiOperation getOpenApiOperation(String uri, String httpMethod) throws URISyntaxException {
        String uriWithoutQuery = new URI(uri).getPath();
        NormalisedPath requestPath = new ApiNormalisedPath(uriWithoutQuery);
        Optional<NormalisedPath> maybeApiPath = OpenApiHelper.findMatchingApiPath(requestPath);
        if (!maybeApiPath.isPresent()) {
            return null;
        }

        final NormalisedPath openApiPathString = maybeApiPath.get();
        final Path path = OpenApiHelper.openApi3.getPath(openApiPathString.original());

        final Operation operation = path.getOperation(httpMethod);
        return new OpenApiOperation(openApiPathString, path, httpMethod, operation);
    }

    private JsonNode getContentSchema(OpenApiOperation operation, String statusCode, String mediaTypeStr) {
        Schema schema;
        Optional<Response> response = Optional.ofNullable(operation.getOperation().getResponse(String.valueOf(statusCode)));
        if(response.isPresent()) {
            Optional<MediaType> mediaType = Optional.ofNullable(response.get().getContentMediaType(mediaTypeStr));
            if(mediaType.isPresent()) {
                schema = mediaType.get().getSchema();
                JsonNode schemaNode = schema == null ? null : Overlay.toJson((SchemaImpl)schema);
                return schemaNode;
            }
        }
        return null;
    }
}
