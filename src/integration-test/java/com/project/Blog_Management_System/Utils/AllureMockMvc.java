package com.project.Blog_Management_System.Utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import io.qameta.allure.attachment.http.HttpRequestAttachment;
import io.qameta.allure.attachment.http.HttpResponseAttachment;
import jakarta.servlet.http.Cookie;
import org.springframework.test.web.servlet.ResultHandler;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Allure logger filter for MockMvc with streamlined masking and truncation.
 */
public class AllureMockMvc {

    private static String requestTemplatePath = "http-request.ftl";
    private static String responseTemplatePath = "http-response.ftl";
    private static String requestAttachmentName = "Request";
    private static String responseAttachmentName = "Response";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Configurations
    private static final int MAX_BODY_CHARACTERS = 10000;
    private static final String MASK_VALUE = "******";

    // Simplified Regex: Catches variations like "password":"val", "secret" : "val", password=val, etc.
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(?i)\"?(authorization|password|secret|refreshtoken|accesstoken|apikey|cookie|set-cookie)\"?\\s*[:=]\\s*\"?([^\",&\\n\\s]+)\"?"
    );

    public AllureMockMvc setRequestTemplate(final String templatePath) {
        requestTemplatePath = templatePath;
        return this;
    }

    public AllureMockMvc setResponseTemplate(final String templatePath) {
        responseTemplatePath = templatePath;
        return this;
    }

    public AllureMockMvc setRequestAttachmentName(final String requestAttachmentName) {
        AllureMockMvc.requestAttachmentName = requestAttachmentName;
        return this;
    }

    public AllureMockMvc setResponseAttachmentName(final String responseAttachmentName) {
        AllureMockMvc.responseAttachmentName = responseAttachmentName;
        return this;
    }

    public static ResultHandler attach() {
        return result -> {
            // --------------------- REQUEST ---------------------
            var request = result.getRequest();

            Map<String, String> requestHeaders = new HashMap<>();
            request.getHeaderNames().asIterator().forEachRemaining(name ->
                    requestHeaders.put(name, maskString(request.getHeader(name))));

            final HttpRequestAttachment.Builder requestAttachmentBuilder = HttpRequestAttachment.Builder
                .create(requestAttachmentName, request.getRequestURI())
                .setMethod(request.getMethod())
                .setHeaders(requestHeaders)
                .setCookies(extractCookies(request.getCookies()));

            byte[] content = request.getContentAsByteArray();
            String requestBody = (content != null && content.length > 0)
                    ? maskAndTruncateBody(new String(content, StandardCharsets.UTF_8))
                    : "(no body)";

            requestAttachmentBuilder.setBody(requestBody);

            new DefaultAttachmentProcessor().addAttachment(
                    requestAttachmentBuilder.build(),
                    new FreemarkerAttachmentRenderer(requestTemplatePath)
            );

            // --------------------- RESPONSE ---------------------
            var response = result.getResponse();

            Map<String, String> responseHeaders = new HashMap<>();
            response.getHeaderNames().forEach(name ->
                    responseHeaders.put(name, maskString(response.getHeader(name))));

            final HttpResponseAttachment responseAttachment = HttpResponseAttachment.Builder
                .create(responseAttachmentName)
                .setResponseCode(response.getStatus())
                .setHeaders(responseHeaders)
                .setCookies(extractCookies(response.getCookies()))
                .setBody(maskAndTruncateBody(response.getContentAsString()))
                .build();

            new DefaultAttachmentProcessor().addAttachment(
                    responseAttachment,
                    new FreemarkerAttachmentRenderer(responseTemplatePath)
            );
        };
    }

    private static Map<String, String> extractCookies(Cookie[] cookies) {
        Map<String, String> cookieMap = new HashMap<>();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                cookieMap.put(cookie.getName(), maskString(cookie.getValue()));
            }
        }
        return cookieMap;
    }

    // Single unified method for masking text headers, cookies, and payloads
    private static String maskString(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        // $1 references the key found by the regex matcher group
        return SENSITIVE_PATTERN.matcher(input).replaceAll(input.indexOf('{') != -1 ? "\"$1\" : " + MASK_VALUE : "$1=" + MASK_VALUE);
    }

    private static String maskAndTruncateBody(String body) {
        if (body == null || body.isBlank()) {
            return body;
        }

        body = tryPrettyPrint(body);

        boolean isTruncated = body.length() > MAX_BODY_CHARACTERS;
        String processedBody = isTruncated ? body.substring(0, MAX_BODY_CHARACTERS) : body;

        String maskedBody = maskString(processedBody);

        return isTruncated ? maskedBody + "\n\n[PAYLOAD TRUNCATED FOR ALLURE REPORTS]" : maskedBody;
    }

    private static String tryPrettyPrint(String rawJson) {
        try {
            Object jsonObject = OBJECT_MAPPER.readValue(rawJson, Object.class);
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
        } catch (Exception e) {
            return rawJson;
        }
    }
}
