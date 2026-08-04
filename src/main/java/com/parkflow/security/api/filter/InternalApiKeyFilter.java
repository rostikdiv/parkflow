package com.parkflow.security.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    @Value("${internal.api.key:default-internal-key-change-me}")
    private String internalApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        if (path.startsWith("/api/internal/")) {
            String apiKey = request.getHeader("X-Internal-API-Key");
            if (apiKey == null || !apiKey.equals(internalApiKey)) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid or missing API Key");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
