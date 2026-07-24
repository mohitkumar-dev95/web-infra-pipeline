package com.webinfra.gateway.cdn;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Component
@Order(0)
public class CdnCacheFilter extends OncePerRequestFilter {

    private final CdnCacheService cdnCacheService;

    public CdnCacheFilter(CdnCacheService cdnCacheService) {
        this.cdnCacheService = cdnCacheService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !cdnCacheService.isEnabled()
                || !path.startsWith("/api/")
                || !"GET".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String cacheKey = buildCacheKey(request);
        CachedResponse cached = cdnCacheService.get(cacheKey);

        if (cached != null) {
            response.setHeader("X-Cache", "HIT");
            response.setStatus(cached.getStatusCode());

            cached.getHeaders().forEach((headerName, headerValues) -> {
                if (!headerName.equalsIgnoreCase("Transfer-Encoding") 
                        && !headerName.equalsIgnoreCase("Connection") 
                        && !headerName.equalsIgnoreCase("X-Cache")) {
                    for (String val : headerValues) {
                        response.addHeader(headerName, val);
                    }
                }
            });

            response.getOutputStream().write(cached.getBody());
            return;
        }

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        responseWrapper.setHeader("X-Cache", "MISS");

        filterChain.doFilter(request, responseWrapper);

        int statusCode = responseWrapper.getStatus();
        byte[] bodyBytes = responseWrapper.getContentAsByteArray();

        if (statusCode >= 200 && statusCode < 300 && bodyBytes.length > 0) {
            HttpHeaders headers = new HttpHeaders();
            for (String name : responseWrapper.getHeaderNames()) {
                if (!name.equalsIgnoreCase("X-Cache")) {
                    headers.put(name, responseWrapper.getHeaders(name).stream().toList());
                }
            }
            cdnCacheService.put(cacheKey, statusCode, headers, bodyBytes);
        }

        responseWrapper.copyBodyToResponse();
    }

    private String buildCacheKey(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        return request.getMethod() + ":" + uri + (query != null ? "?" + query : "");
    }
}
