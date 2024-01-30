package com.springlogging.springloggingproj.common;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLogFilter extends OncePerRequestFilter {

    private static String REQUEST_ID = "request_id";
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        ContentCachingRequestWrapper wrapperRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrapperResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();
        filterChain.doFilter(wrapperRequest, wrapperResponse);
        long endTime = System.currentTimeMillis();

        String traceId = UUID.randomUUID().toString().substring(0, 8);


        MDC.put(REQUEST_ID, traceId);

        try {
            log.info("" +
                    "traceId : " + traceId + " , " +
                    "Method : " + wrapperRequest.getMethod()+" , " +
                    "HttpStatus : " + HttpStatus.resolve(wrapperResponse.getStatus())+" , " +
                    "elapsedTime : " + (endTime - startTime) / 1000.0+" , " +
                    "uri : " + wrapperRequest.getRequestURI()
            );
        } catch (Exception e) {
            e.printStackTrace();
            log.error("logging error 발생");
        }

        MDC.remove(REQUEST_ID);

        wrapperResponse.copyBodyToResponse();
    }
}
