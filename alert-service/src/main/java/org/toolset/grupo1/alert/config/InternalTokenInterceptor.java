package org.toolset.grupo1.alert.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class InternalTokenInterceptor implements HandlerInterceptor {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final String expectedToken;

    public InternalTokenInterceptor(@Value("${internal.token:stark-internal-token}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String providedToken = request.getHeader(INTERNAL_TOKEN_HEADER);
        if (expectedToken.equals(providedToken)) {
            return true;
        }

        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid internal token");
        return false;
    }
}

