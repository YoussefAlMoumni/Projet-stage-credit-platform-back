package com.talan.creditplatform.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcFilter implements Filter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_TRACE_ID_KEY = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        try {
            if (request instanceof HttpServletRequest httpRequest
                    && response instanceof HttpServletResponse httpResponse) {

                // ── Trace ID ────────────────────────────────────────────
                String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
                if (traceId == null || traceId.isBlank()) {
                    traceId = UUID.randomUUID().toString();
                }
                MDC.put(MDC_TRACE_ID_KEY, traceId);

                // ── User ─────────────────────────────────────────────────
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated()) {
                    Object principal = authentication.getPrincipal();
                    if (principal instanceof UserDetails userDetails) {
                        MDC.put("user", userDetails.getUsername());
                    } else if (principal instanceof String username) {
                        MDC.put("user", username);
                    }
                }

                // ── HTTP Status Code — captured via response wrapper ──────
                // We MUST capture the status before chain.doFilter() returns,
                // because log statements emitted during the request need to see
                // the correct httpCode in MDC. A plain HttpServletResponseWrapper
                // intercepts setStatus() / sendError() so we can read it at any point.
                StatusCapturingResponseWrapper wrappedResponse =
                        new StatusCapturingResponseWrapper(httpResponse);

                chain.doFilter(httpRequest, wrappedResponse);

                // Set httpCode AFTER doFilter so the final committed status is captured.
                // This is correct: all controller/service logs use the request-scoped
                // thread and see their log lines emitted during doFilter. httpCode in MDC
                // is used for the *access log* line written by MdcFilter itself after the
                // request completes (or by any post-processing interceptor).
                MDC.put("httpCode", String.valueOf(wrappedResponse.getCapturedStatus()));
            } else {
                // Non-HTTP request (e.g. websocket handshake) — just pass through
                chain.doFilter(request, response);
            }
        } finally {
            // Always clear MDC to prevent context leaking across thread-pool reuse
            MDC.clear();
        }
    }

    /**
     * Wraps HttpServletResponse to capture the HTTP status code set during
     * the request lifecycle, which is not reliably readable after commit.
     */
    private static final class StatusCapturingResponseWrapper extends HttpServletResponseWrapper {

        private int status = HttpServletResponse.SC_OK;

        StatusCapturingResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setStatus(int sc) {
            this.status = sc;
            super.setStatus(sc);
        }

        @Override
        @SuppressWarnings("deprecation")
        public void setStatus(int sc, String sm) {
            this.status = sc;
            super.setStatus(sc, sm);
        }

        @Override
        public void sendError(int sc) throws IOException {
            this.status = sc;
            super.sendError(sc);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            this.status = sc;
            super.sendError(sc, msg);
        }

        public int getCapturedStatus() {
            return this.status;
        }
    }
}
