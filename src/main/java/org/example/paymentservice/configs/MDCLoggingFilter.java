package org.example.paymentservice.configs;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import java.io.IOException;
import java.util.UUID;

public class MDCLoggingFilter implements Filter {
    private static final String REQUEST_ID = "requestId";
    private static final String USER_ID = "userId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String requestId = UUID.randomUUID().toString();
            String userId = httpRequest.getUserPrincipal() != null
                    ? httpRequest.getUserPrincipal().getName()
                    : "anonymous";

            MDC.put(REQUEST_ID, requestId);
            MDC.put(USER_ID, userId);

            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}

