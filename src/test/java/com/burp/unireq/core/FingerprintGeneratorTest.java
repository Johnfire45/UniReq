package com.burp.unireq.core;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.logging.Logging;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FingerprintGeneratorTest {

    private FingerprintGenerator generator;
    private Logging logging;

    @BeforeEach
    void setUp() {
        logging = mock(Logging.class);
        generator = new FingerprintGenerator(logging);
    }

    private HttpRequest mockGet(String host, String path, String query) {
        HttpRequest req = mock(HttpRequest.class);
        HttpService svc = mock(HttpService.class);
        when(svc.host()).thenReturn(host);
        when(req.httpService()).thenReturn(svc);
        when(req.method()).thenReturn("GET");
        when(req.path()).thenReturn(path);
        when(req.query()).thenReturn(query);
        // body() returns null by default — hasRequestBody() short-circuits safely
        return req;
    }

    @Test
    void sameGetRequestProducesSameFingerprint() {
        HttpRequest r1 = mockGet("example.com", "/api/users", "page=1");
        HttpRequest r2 = mockGet("example.com", "/api/users", "page=1");
        assertEquals(generator.computeFingerprint(r1), generator.computeFingerprint(r2));
    }

    @Test
    void differentHostProducesDifferentFingerprint() {
        HttpRequest r1 = mockGet("example.com", "/api", "");
        HttpRequest r2 = mockGet("other.com", "/api", "");
        assertNotEquals(generator.computeFingerprint(r1), generator.computeFingerprint(r2));
    }

    @Test
    void differentPathProducesDifferentFingerprint() {
        HttpRequest r1 = mockGet("example.com", "/api/users", "");
        HttpRequest r2 = mockGet("example.com", "/api/items", "");
        assertNotEquals(generator.computeFingerprint(r1), generator.computeFingerprint(r2));
    }

    @Test
    void differentQueryProducesDifferentFingerprint() {
        HttpRequest r1 = mockGet("example.com", "/search", "q=foo");
        HttpRequest r2 = mockGet("example.com", "/search", "q=bar");
        assertNotEquals(generator.computeFingerprint(r1), generator.computeFingerprint(r2));
    }

    @Test
    void sameQueryProducesSameFingerprint() {
        HttpRequest r1 = mockGet("example.com", "/search", "q=foo&page=2");
        HttpRequest r2 = mockGet("example.com", "/search", "q=foo&page=2");
        assertEquals(generator.computeFingerprint(r1), generator.computeFingerprint(r2));
    }

    @Test
    void trailingSlashNormalized() {
        HttpRequest r1 = mockGet("example.com", "/api/", "");
        HttpRequest r2 = mockGet("example.com", "/api", "");
        assertEquals(generator.computeFingerprint(r1), generator.computeFingerprint(r2));
    }

    @Test
    void uppercasePathNormalized() {
        HttpRequest r1 = mockGet("example.com", "/API/Users", "");
        HttpRequest r2 = mockGet("example.com", "/api/users", "");
        assertEquals(generator.computeFingerprint(r1), generator.computeFingerprint(r2));
    }

    @Test
    void fingerprintIsNotNullOrEmpty() {
        HttpRequest req = mockGet("example.com", "/", "");
        String fp = generator.computeFingerprint(req);
        assertNotNull(fp);
        assertFalse(fp.trim().isEmpty());
    }

    @Test
    void fingerprintContainsMethodAndHost() {
        HttpRequest req = mockGet("example.com", "/api", "");
        String fp = generator.computeFingerprint(req);
        assertTrue(fp.contains("GET"), "Fingerprint should contain method");
        assertTrue(fp.contains("example.com"), "Fingerprint should contain host");
    }
}
