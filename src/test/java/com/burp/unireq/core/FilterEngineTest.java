package com.burp.unireq.core;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.logging.Logging;
import com.burp.unireq.model.FilterCriteria;
import com.burp.unireq.model.RequestResponseEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FilterEngineTest {

    private FilterEngine engine;
    private Logging logging;
    private MontoyaApi api;

    @BeforeEach
    void setUp() {
        logging = mock(Logging.class);
        api = mock(MontoyaApi.class);
        engine = new FilterEngine(logging, api);
    }

    private RequestResponseEntry mockEntry(String method, String host, String path) {
        HttpRequest req = mock(HttpRequest.class);
        HttpService svc = mock(HttpService.class);
        when(svc.host()).thenReturn(host);
        when(req.httpService()).thenReturn(svc);
        when(req.method()).thenReturn(method);
        when(req.path()).thenReturn(path);
        when(req.toString()).thenReturn(method + " " + path + " HTTP/1.1\r\nHost: " + host + "\r\n\r\n");
        return new RequestResponseEntry(req, "fp-" + method + host + path, 1);
    }

    @Test
    void nullEntryReturnsFalse() {
        assertFalse(engine.matchesFilters(null, new FilterCriteria()));
    }

    @Test
    void nullCriteriaReturnsFalse() {
        RequestResponseEntry entry = mockEntry("GET", "example.com", "/api");
        assertFalse(engine.matchesFilters(entry, null));
    }

    @Test
    void defaultCriteriaMatchesAll() {
        RequestResponseEntry entry = mockEntry("GET", "example.com", "/api");
        assertTrue(engine.matchesFilters(entry, new FilterCriteria()));
    }

    @Test
    void methodFilterMatchesCorrectMethod() {
        RequestResponseEntry entry = mockEntry("POST", "example.com", "/login");
        FilterCriteria criteria = new FilterCriteria();
        criteria.setMethod("POST");
        assertTrue(engine.matchesFilters(entry, criteria));
    }

    @Test
    void methodFilterRejectsWrongMethod() {
        RequestResponseEntry entry = mockEntry("GET", "example.com", "/api");
        FilterCriteria criteria = new FilterCriteria();
        criteria.setMethod("POST");
        assertFalse(engine.matchesFilters(entry, criteria));
    }

    @Test
    void methodFilterAllPassesAnyMethod() {
        RequestResponseEntry entry = mockEntry("DELETE", "example.com", "/resource/1");
        FilterCriteria criteria = new FilterCriteria();
        criteria.setMethod("All");
        assertTrue(engine.matchesFilters(entry, criteria));
    }

    @Test
    void hostPatternMatchesSubstring() {
        RequestResponseEntry entry = mockEntry("GET", "api.example.com", "/users");
        FilterCriteria criteria = new FilterCriteria();
        criteria.setHostPattern("example.com");
        assertTrue(engine.matchesFilters(entry, criteria));
    }

    @Test
    void hostPatternRejectsNonMatch() {
        RequestResponseEntry entry = mockEntry("GET", "other.com", "/users");
        FilterCriteria criteria = new FilterCriteria();
        criteria.setHostPattern("example.com");
        assertFalse(engine.matchesFilters(entry, criteria));
    }

    @Test
    void pathPatternMatchesSubstring() {
        RequestResponseEntry entry = mockEntry("GET", "example.com", "/api/v1/users");
        FilterCriteria criteria = new FilterCriteria();
        criteria.setPathPattern("/api/");
        assertTrue(engine.matchesFilters(entry, criteria));
    }

    @Test
    void pathPatternRejectsNonMatch() {
        RequestResponseEntry entry = mockEntry("GET", "example.com", "/static/logo.png");
        FilterCriteria criteria = new FilterCriteria();
        criteria.setPathPattern("/api/");
        assertFalse(engine.matchesFilters(entry, criteria));
    }

    @Test
    void invertHostFilterNegatesMatch() {
        RequestResponseEntry entry = mockEntry("GET", "example.com", "/api");
        FilterCriteria criteria = new FilterCriteria();
        criteria.setHostPattern("example.com");
        criteria.setInvertHostFilter(true);
        assertFalse(engine.matchesFilters(entry, criteria));
    }

    @Test
    void invertHostFilterPassesNonMatch() {
        RequestResponseEntry entry = mockEntry("GET", "other.com", "/api");
        FilterCriteria criteria = new FilterCriteria();
        criteria.setHostPattern("example.com");
        criteria.setInvertHostFilter(true);
        assertTrue(engine.matchesFilters(entry, criteria));
    }

    @Test
    void emptyHostPatternMatchesAll() {
        RequestResponseEntry entry = mockEntry("GET", "anything.io", "/");
        FilterCriteria criteria = new FilterCriteria();
        criteria.setHostPattern("");
        assertTrue(engine.matchesFilters(entry, criteria));
    }
}
