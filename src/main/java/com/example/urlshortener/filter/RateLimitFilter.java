
package com.example.urlshortener.filter;

import com.example.urlshortener.exception.RateLimitExceededException;
import com.example.urlshortener.service.RateLimitService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitFilter implements Filter {

    private final RateLimitService rateLimitService;

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest httpRequest =
                (HttpServletRequest) request;

        String path = httpRequest.getRequestURI();

        // Skip health/actuator endpoints
        if (path.startsWith("/actuator")) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = httpRequest.getRemoteAddr();

        try {
            rateLimitService.validateRateLimit(clientIp);

            chain.doFilter(request, response);

        } catch (RateLimitExceededException ex) {

            HttpServletResponse httpResponse =
                    (HttpServletResponse) response;

            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");

            httpResponse.getWriter().write("""
                {
                  "status": 429,
                  "error": "Too Many Requests",
                  "message": "Rate limit exceeded. Try again later."
                }
                """);
        }
    }
}

