package com.epam.sonar.web.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that populates MDC with request context on every
 * incoming HTTP request, so all logs within that request automatically
 * carry traceId, requestId, method, and URI.
 *
 * Integrates with logback-spring.xml JSON appender — every log line
 * in the JSON log file will contain these fields for easy correlation
 * in Kibana, Grafana Loki, or Splunk.
 */
@Component
@Order(1)
@Slf4j
public class MdcLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID  = "requestId";
    private static final String TRACE_ID    = "traceId";
    private static final String METHOD      = "method";
    private static final String URI         = "uri";
    private static final String CLIENT_IP   = "clientIp";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        try {
            // Populate MDC — will appear in every log line during this request
            String requestId = UUID.randomUUID().toString();
            // Check for upstream traceId (e.g. from API gateway or load balancer)
            String traceId = request.getHeader("X-Trace-Id");
            if (traceId == null || traceId.isBlank()) {
                traceId = requestId;
            }

            MDC.put(REQUEST_ID, requestId);
            MDC.put(TRACE_ID,   traceId);
            MDC.put(METHOD,     request.getMethod());
            MDC.put(URI,        request.getRequestURI());
            MDC.put(CLIENT_IP,  getClientIp(request));

            // Add traceId to response header for client-side correlation
            response.setHeader("X-Trace-Id", traceId);
            response.setHeader("X-Request-Id", requestId);

            log.debug("Incoming request: {} {}", request.getMethod(), request.getRequestURI());

            filterChain.doFilter(request, response);

        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Request completed in {}ms — status: {}", duration, response.getStatus());
            MDC.clear(); // ✅ Always clear — critical for thread pool reuse
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
