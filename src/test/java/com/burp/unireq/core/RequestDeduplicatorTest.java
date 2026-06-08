package com.burp.unireq.core;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.logging.Logging;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestDeduplicatorTest {

    private RequestDeduplicator deduplicator;
    private Logging logging;

    @BeforeEach
    void setUp() {
        logging = mock(Logging.class);
        deduplicator = new RequestDeduplicator(logging);
    }

    private HttpRequest mockRequest(String method, String host, String path, String query) {
        HttpRequest req = mock(HttpRequest.class);
        HttpService svc = mock(HttpService.class);
        when(svc.host()).thenReturn(host);
        when(req.httpService()).thenReturn(svc);
        when(req.method()).thenReturn(method);
        when(req.path()).thenReturn(path);
        when(req.query()).thenReturn(query);
        when(req.toString()).thenReturn(method + " " + path + " HTTP/1.1\r\nHost: " + host + "\r\n\r\n");
        // body() returns null by default — hasRequestBody() short-circuits safely
        return req;
    }

    @Test
    void firstRequestIsUnique() {
        HttpRequest req = mockRequest("GET", "example.com", "/api", "");
        assertTrue(deduplicator.isUniqueRequest(req));
    }

    @Test
    void secondIdenticalRequestIsNotUnique() {
        HttpRequest r1 = mockRequest("GET", "example.com", "/api", "page=1");
        HttpRequest r2 = mockRequest("GET", "example.com", "/api", "page=1");
        deduplicator.isUniqueRequest(r1);
        assertFalse(deduplicator.isUniqueRequest(r2));
    }

    @Test
    void differentPathIsUnique() {
        HttpRequest r1 = mockRequest("GET", "example.com", "/api/users", "");
        HttpRequest r2 = mockRequest("GET", "example.com", "/api/items", "");
        deduplicator.isUniqueRequest(r1);
        assertTrue(deduplicator.isUniqueRequest(r2));
    }

    @Test
    void differentMethodIsUnique() {
        HttpRequest r1 = mockRequest("GET", "example.com", "/api", "");
        HttpRequest r2 = mockRequest("POST", "example.com", "/api", "");
        deduplicator.isUniqueRequest(r1);
        assertTrue(deduplicator.isUniqueRequest(r2));
    }

    @Test
    void differentHostIsUnique() {
        HttpRequest r1 = mockRequest("GET", "example.com", "/api", "");
        HttpRequest r2 = mockRequest("GET", "other.com", "/api", "");
        deduplicator.isUniqueRequest(r1);
        assertTrue(deduplicator.isUniqueRequest(r2));
    }

    @Test
    void differentQueryIsUnique() {
        HttpRequest r1 = mockRequest("GET", "example.com", "/search", "q=foo");
        HttpRequest r2 = mockRequest("GET", "example.com", "/search", "q=bar");
        deduplicator.isUniqueRequest(r1);
        assertTrue(deduplicator.isUniqueRequest(r2));
    }

    @Test
    void clearResetsDeduplication() {
        HttpRequest r1 = mockRequest("GET", "example.com", "/api", "");
        HttpRequest r2 = mockRequest("GET", "example.com", "/api", "");
        deduplicator.isUniqueRequest(r1);
        deduplicator.clearFingerprints();
        assertTrue(deduplicator.isUniqueRequest(r2));
    }

    @Test
    void countersTrackCorrectly() {
        HttpRequest r1 = mockRequest("GET", "example.com", "/a", "");
        HttpRequest r2 = mockRequest("GET", "example.com", "/a", "");
        HttpRequest r3 = mockRequest("GET", "example.com", "/b", "");
        deduplicator.isUniqueRequest(r1);
        deduplicator.isUniqueRequest(r2);
        deduplicator.isUniqueRequest(r3);
        assertEquals(3, deduplicator.getTotalRequests());
        assertEquals(2, deduplicator.getUniqueRequests());
        assertEquals(1, deduplicator.getDuplicateRequests());
    }

    @Test
    void filteringCanBeDisabled() {
        deduplicator.setFilteringEnabled(false);
        HttpRequest r1 = mockRequest("GET", "example.com", "/api", "");
        HttpRequest r2 = mockRequest("GET", "example.com", "/api", "");
        assertTrue(deduplicator.isUniqueRequest(r1));
        assertTrue(deduplicator.isUniqueRequest(r2));
    }

    @Test
    void storedRequestsAccumulate() {
        HttpRequest r1 = mockRequest("GET", "example.com", "/a", "");
        HttpRequest r2 = mockRequest("GET", "example.com", "/b", "");
        deduplicator.isUniqueRequest(r1);
        deduplicator.isUniqueRequest(r2);
        assertEquals(2, deduplicator.getStoredRequests().size());
    }
}
