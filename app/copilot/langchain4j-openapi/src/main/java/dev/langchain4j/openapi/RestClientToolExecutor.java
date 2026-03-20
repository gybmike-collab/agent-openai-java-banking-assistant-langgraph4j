// Copyright (c) Microsoft. All rights reserved.
package dev.langchain4j.openapi;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpMethod;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.SuccessfulHttpResponse;
import dev.langchain4j.service.tool.ToolExecutor;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Plugin for making HTTP requests specifically to endpoints discovered via OpenAPI.
 */
public class RestClientToolExecutor implements ToolExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestClientToolExecutor.class);

    private final String serverUrl;
    private final String path;
    private final PathItem pathItem;
    private final HttpClient client;
    private final HttpMethod method;
    private final Operation operation;
    private final Map<String, List<String>>  httpHeaders;

    public RestClientToolExecutor(
        HttpMethod method,
        String serverUrl,
        String path,
        PathItem pathItem,
        HttpClient client,
        Map<String, List<String>>  httpHeaders,
        Operation operation) {
        this.method = method;
        this.serverUrl = serverUrl;
        this.path = path;
        this.pathItem = pathItem;
        this.client = client;
        this.operation = operation;

        this.httpHeaders = Objects.requireNonNullElseGet(httpHeaders, HashMap::new);
    }

    public String execute(ToolExecutionRequest toolExecutionRequest, Object memoryId) {
        return executeFake( toolExecutionRequest, memoryId );
    }

    /**
     * 执行 HTTP 请求，并返回响应体。
     */
    private String executeFake(ToolExecutionRequest toolExecutionRequest, Object memoryId) {

        if( Objects.equals( "account-api-getAccountsByUserName", toolExecutionRequest.name() ) ) {
            return """
                    {
                    "id": "1234567"
                    "name": "Bob"
                    }
                    """;
        }
        if( Objects.equals( "account-api-getAccountDetails", toolExecutionRequest.name() )) {
            return """
                    {
                    "balance": "1000"
                    "movement": [ "payment1": -100, "payment2": -200 ]
                    "currency": "EUR"
                    }
                    """;
        }
        if( Objects.equals( "account-api-getPaymentMethodDetails", toolExecutionRequest.name() )) {
            return """
                    {
                    "name": "visa"
                    "expire": "12/2025"
                    }
                    """;
        }
        return "{}";

    }

    private String executeOriginal(ToolExecutionRequest toolExecutionRequest, Object memoryId) {


        //使用 Jackson 将 JSON 字符串转换为 Map<string,Object>
        Map<String,Object> arguments = ToolExecutionRequestUtil.argumentsAsMap(toolExecutionRequest.arguments());


        String body = getBody(arguments);
        String query = buildQueryString(arguments);
        String path = buildQueryPath(arguments);

        String url;
        if (!query.isEmpty()) {
            url = serverUrl + path + "?" + query;
        } else {
            url = serverUrl + path;
        }


        HttpRequest request = null;

        if (body != null) {
            httpHeaders.put("Content-Type", Collections.singletonList("application/json"));
            request = HttpRequest.builder()
                    .method(method)
                    .url(url)
                    .headers(httpHeaders)
                    .body(body)
                    .build();
        } else {
            request = HttpRequest.builder()
                    .method(method)
                    .url(url)
                    .headers(httpHeaders)
                    .build();
        }


        LOGGER.debug("Executing {} {}", method.name(), url);
        if (body != null) {
            LOGGER.debug("Body: {}", body);
        }

        SuccessfulHttpResponse response = null;

        try {
            response = client.execute(request);
        } catch (HttpException e){
            throw new RuntimeException("Http request failed. Server returned code ["+e.statusCode()+"] with error : " + e.getMessage());

        } catch (RuntimeException e) {
            throw new RuntimeException("Http request failed with generic error : " + e.getMessage());
        }
        return response.body();

    }

    protected String getBody(Map<String,Object> arguments) {
        String body = null;
        if (arguments.containsKey("requestBody")) {
            Object requestBody = arguments.get("requestBody");
            if (requestBody != null) body = requestBody.toString();
            try {
                body = new ObjectMapper().writeValueAsString(requestBody);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to serialize to JSON the request body map:"+requestBody, e);
            }
            arguments.remove("requestBody");
        }
        return body;
    }

    /**
     * 为 HTTP 请求构建查询字符串。
     * @param arguments HTTP 请求参数。
     * @return 查询字符串。
     */
    protected String buildQueryString(Map<String,Object> arguments) {
        return getParameterStreamOfArguments(arguments)
                .filter(p -> p instanceof QueryParameter)
                .map(parameter -> {
                    String name = parameter.getName();
                    String rendered = encodeParameter(arguments, name);
                    return name + "=" + rendered;
                })
                .collect(Collectors.joining("&"));
    }

    /**
     * 为 HTTP 请求构建路径字符串。
     * @param arguments HTTP 请求参数。
     * @return 路径字符串。
     */
    protected String buildQueryPath(Map<String,Object> arguments) {
        return getParameterStreamOfArguments(arguments)
                .filter(p -> p instanceof PathParameter)
                .reduce(path, (path, parameter) -> {
                    String name = parameter.getName();
                    String rendered = encodeParameter(arguments, name);

                    return path.replaceAll("\\{" + name + "}", rendered);
                }, (a, b) -> a + b);
    }

/**
 * 对 arguments 映射中包含的所有参数，返回在 OpenAPI 规范中与参数名匹配的参数。
 *
 * @param arguments 用于与 OpenAPI 参数进行匹配的参数映射。
 * @return 与参数名匹配的参数流。
 */
protected Stream<Parameter> getParameterStreamOfArguments(
            Map<String,Object> arguments) {
        if (operation.getParameters() == null) {
            return Stream.empty();
        }
        return arguments
                .keySet()
                .stream()
                .map(toolArgumentName -> operation
                        .getParameters()
                        .stream()
                        .filter(param -> param.getName().equalsIgnoreCase(toolArgumentName)).findFirst())
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    protected static String encodeParameter(
            Map<String,Object> arguments, String name) {
        Object value = arguments.get(name);

        if (value == null) {
            throw new IllegalArgumentException("Missing value for path parameter: " + name);
        }
        String rendered = value.toString();

        if (rendered == null) {
            throw new IllegalArgumentException("Path parameter value is null: " + name);
        }
        return URLEncoder.encode(rendered, StandardCharsets.US_ASCII);
    }


}