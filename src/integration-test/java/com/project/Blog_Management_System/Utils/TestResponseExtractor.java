package com.project.Blog_Management_System.Utils;

import com.project.Blog_Management_System.Advice.ApiError;
import com.project.Blog_Management_System.Advice.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@TestComponent
@RequiredArgsConstructor
public class TestResponseExtractor {

    private final ObjectMapper objectMapper;

    /**
     * Extracts and deserializes a successful data response body.
     */
    public <T> T extractPayload(MvcResult result, Class<T> responseDtoClass) throws Exception {
        String content = result.getResponse().getContentAsString();

        JavaType targetType = objectMapper.getTypeFactory()
                .constructParametricType(ApiResponse.class, responseDtoClass);

        ApiResponse<T> envelope = objectMapper.readValue(content, targetType);

        if (envelope == null || envelope.getData() == null) {
            throw new IllegalStateException("The response data block was null. Ensure this was meant to be a success test.");
        }

        return envelope.getData();
    }

    /**
     * Extracts and deserializes a successful data response body that contains a paginated slice of data.
     */
    public <T> TestSliceResponse<T> extractSlicePayload(MvcResult result, Class<T> responseDtoClass) throws Exception {
        String content = result.getResponse().getContentAsString();

        JavaType sliceType = objectMapper.getTypeFactory()
                .constructParametricType(TestSliceResponse.class, responseDtoClass);

        JavaType targetType = objectMapper.getTypeFactory()
                .constructParametricType(ApiResponse.class, sliceType);

        ApiResponse<TestSliceResponse<T>> envelope = objectMapper.readValue(content, targetType);

        if (envelope == null || envelope.getData() == null) {
            throw new IllegalStateException("The response data slice block was null.");
        }

        return envelope.getData();
    }

    /**
     * Extracts and deserializes a successful data response body that contains a paginated page of data.
     */
    public <T> TestPageResponse<T> extractPagePayload(MvcResult result, Class<T> responseDtoClass) throws Exception {
        String content = result.getResponse().getContentAsString();

        JavaType sliceType = objectMapper.getTypeFactory()
                .constructParametricType(TestPageResponse.class, responseDtoClass);

        JavaType targetType = objectMapper.getTypeFactory()
                .constructParametricType(ApiResponse.class, sliceType);

        ApiResponse<TestPageResponse<T>> envelope = objectMapper.readValue(content, targetType);

        if (envelope == null || envelope.getData() == null) {
            throw new IllegalStateException("The response data slice block was null.");
        }

        return envelope.getData();
    }

    /**
     * Extracts and deserializes a successful data response body that contains a List of data.
     */
    public <T> List<T> extractListPayload(MvcResult result, Class<T> responseDtoClass) throws Exception {
        String content = result.getResponse().getContentAsString();

        JavaType sliceType = objectMapper.getTypeFactory()
                .constructParametricType(ArrayList.class, responseDtoClass);

        JavaType targetType = objectMapper.getTypeFactory()
                .constructParametricType(ApiResponse.class, sliceType);

        ApiResponse<List<T>> envelope = objectMapper.readValue(content, targetType);

        if (envelope == null || envelope.getData() == null) {
            throw new IllegalStateException("The response data slice block was null.");
        }

        return envelope.getData();
    }

    /**
     * Extracts and deserializes an error body from a failed API execution.
     */
    public ApiError extractError(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString();

        JavaType targetType = objectMapper.getTypeFactory()
                .constructParametricType(ApiResponse.class, ApiError.class);

        ApiResponse<Void> envelope = objectMapper.readValue(content, targetType);

        if (envelope == null || envelope.getError() == null) {
            throw new IllegalStateException("The response error block was null. Ensure this was meant to be a failure test.");
        }

        return envelope.getError();
    }
}
