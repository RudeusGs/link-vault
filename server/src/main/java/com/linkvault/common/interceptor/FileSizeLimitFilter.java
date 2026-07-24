package com.linkvault.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkvault.common.exception.ErrorCode;
import com.linkvault.common.response.ApiResponse;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FileSizeLimitFilter implements Filter {

    @Value("${spring.servlet.multipart.max-request-size:20MB}")
    private String maxRequestSizeStr;
    
    private final ObjectMapper objectMapper;

    public FileSizeLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if ("POST".equalsIgnoreCase(httpRequest.getMethod()) || "PUT".equalsIgnoreCase(httpRequest.getMethod())) {
            String contentType = httpRequest.getContentType();
            if (contentType != null && contentType.toLowerCase().startsWith("multipart/form-data")) {
                long contentLength = httpRequest.getContentLengthLong();
                long maxBytes = parseSizeToBytes(maxRequestSizeStr);
                
                if (contentLength > maxBytes) {
                    httpResponse.setStatus(413);
                    httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    
                    ApiResponse<?> apiResponse = ApiResponse.failure(
                        "Payload too large. Maximum size is " + maxRequestSizeStr,
                        ErrorCode.BAD_REQUEST
                    );
                    objectMapper.writeValue(httpResponse.getWriter(), apiResponse);
                    return;
                }
            }
        }
        
        chain.doFilter(request, response);
    }

    private long parseSizeToBytes(String sizeStr) {
        sizeStr = sizeStr.trim().toUpperCase();
        if (sizeStr.endsWith("MB")) {
            return Long.parseLong(sizeStr.replace("MB", "")) * 1024 * 1024;
        } else if (sizeStr.endsWith("KB")) {
            return Long.parseLong(sizeStr.replace("KB", "")) * 1024;
        } else if (sizeStr.endsWith("GB")) {
            return Long.parseLong(sizeStr.replace("GB", "")) * 1024 * 1024 * 1024;
        } else if (sizeStr.endsWith("B")) {
            return Long.parseLong(sizeStr.replace("B", ""));
        } else {
            // Assume bytes
            return Long.parseLong(sizeStr);
        }
    }
}
