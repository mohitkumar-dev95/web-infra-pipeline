package com.webinfra.gateway.cdn;

import org.springframework.http.HttpHeaders;

public class CachedResponse {
    private final int statusCode;
    private final HttpHeaders headers;
    private final byte[] body;
    private final long expiryTimeMs;

    public CachedResponse(int statusCode, HttpHeaders headers, byte[] body, long ttlSeconds) {
        this.statusCode = statusCode;
        this.headers = new HttpHeaders();
        if (headers != null) {
            this.headers.putAll(headers);
        }
        this.body = body != null ? body : new byte[0];
        this.expiryTimeMs = System.currentTimeMillis() + (ttlSeconds * 1000L);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTimeMs;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }
}
