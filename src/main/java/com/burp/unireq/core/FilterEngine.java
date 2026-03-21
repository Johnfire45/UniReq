package com.burp.unireq.core;

import com.burp.unireq.model.FilterCriteria;
import com.burp.unireq.model.RequestResponseEntry;
import com.burp.unireq.utils.HttpUtils;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.logging.Logging;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * FilterEngine - Handles filtering of HTTP requests based on criteria
 *
 * This class provides filtering functionality for HTTP requests based on
 * various criteria such as method, status code, host, path, and other
 * attributes. It supports both simple string matching and regex patterns.
 *
 * @author Harshit Shah
 */
public class FilterEngine {

    private final Logging logging;
    private MontoyaApi montoyaApi;

    /**
     * Constructor initializes the filter engine with logging and API support.
     *
     * @param logging    Burp's logging interface for debug output and error reporting
     * @param montoyaApi Burp's Montoya API for scope checking and advanced features (can be null initially)
     */
    public FilterEngine(Logging logging, MontoyaApi montoyaApi) {
        this.logging = logging;
        this.montoyaApi = montoyaApi;
    }

    /**
     * Updates the MontoyaApi reference. This allows the FilterEngine to be created
     * before the API is available and updated later.
     *
     * @param montoyaApi The MontoyaApi instance
     */
    public void setApi(MontoyaApi montoyaApi) {
        this.montoyaApi = montoyaApi;
        if (montoyaApi != null) {
            logging.logToOutput("FilterEngine API updated successfully");
        }
    }

    /**
     * Filters a list of request/response entries based on the provided criteria.
     *
     * @param entries  The list of entries to filter
     * @param criteria The filter criteria to apply
     * @return A new list containing only entries that match the criteria
     */
    public List<RequestResponseEntry> filterRequests(List<RequestResponseEntry> entries, FilterCriteria criteria) {
        if (entries == null || entries.isEmpty()) {
            return new ArrayList<>();
        }

        if (criteria == null || !criteria.hasActiveFilters()) {
            return new ArrayList<>(entries);
        }

        List<RequestResponseEntry> filteredEntries = new ArrayList<>();

        for (RequestResponseEntry entry : entries) {
            if (matchesFilters(entry, criteria)) {
                filteredEntries.add(entry);
            }
        }

        logging.logToOutput(String.format("Filtered %d entries to %d based on criteria",
                entries.size(), filteredEntries.size()));

        return filteredEntries;
    }

    /**
     * Checks if a single request/response entry matches the filter criteria.
     *
     * @param entry    The entry to check
     * @param criteria The filter criteria
     * @return true if the entry matches all active filters
     */
    public boolean matchesFilters(RequestResponseEntry entry, FilterCriteria criteria) {
        if (entry == null || criteria == null) {
            return true;
        }

        try {
            return matchesMethodFilter(entry, criteria)
                && matchesStatusFilter(entry, criteria)
                && matchesHostFilter(entry, criteria)
                && matchesPathFilter(entry, criteria)
                && matchesResponseFilter(entry, criteria)
                && matchesAdvancedMethodFilter(entry, criteria)
                && matchesAdvancedStatusFilter(entry, criteria)
                && matchesMimeTypeFilter(entry, criteria)
                && matchesExtensionFilter(entry, criteria)
                && matchesScopeFilter(entry, criteria);
        } catch (Exception e) {
            logging.logToError("Error applying filters: " + e.getMessage());
            return true;
        }
    }

    /**
     * Checks if the entry matches the HTTP method filter.
     */
    private boolean matchesMethodFilter(RequestResponseEntry entry, FilterCriteria criteria) {
        String methodFilter = criteria.getMethod();
        if (methodFilter == null || "All".equals(methodFilter)) {
            return true;
        }

        return methodFilter.equalsIgnoreCase(entry.getMethod());
    }

    /**
     * Checks if the entry matches the status code filter.
     */
    private boolean matchesStatusFilter(RequestResponseEntry entry, FilterCriteria criteria) {
        String statusFilter = criteria.getStatusCode();
        if (statusFilter == null || "All".equals(statusFilter)) {
            return true;
        }

        String entryStatus = entry.getStatusCode();
        if ("Pending".equals(entryStatus)) {
            return false;
        }

        try {
            int statusCode = Integer.parseInt(entryStatus);

            if (statusFilter.endsWith("xx")) {
                int rangeStart = Integer.parseInt(statusFilter.substring(0, 1)) * 100;
                return statusCode >= rangeStart && statusCode < rangeStart + 100;
            }

            return statusFilter.equals(entryStatus);

        } catch (NumberFormatException e) {
            logging.logToError("Invalid status code format: " + entryStatus);
            return false;
        }
    }

    /**
     * Checks if the entry matches the host filter.
     */
    private boolean matchesHostFilter(RequestResponseEntry entry, FilterCriteria criteria) {
        String hostPattern = criteria.getHostPattern();
        if (hostPattern == null || hostPattern.trim().isEmpty()) {
            return true;
        }

        String host = entry.getRequest().httpService().host();
        boolean matches = matchesTextPattern(host, hostPattern, criteria.isCaseSensitive(), criteria.isRegexMode());

        return criteria.isInvertHostFilter() ? !matches : matches;
    }

    /**
     * Checks if the entry matches the path filter.
     */
    private boolean matchesPathFilter(RequestResponseEntry entry, FilterCriteria criteria) {
        String pathPattern = criteria.getPathPattern();
        if (pathPattern == null || pathPattern.trim().isEmpty()) {
            return true;
        }

        return matchesTextPattern(entry.getPath(), pathPattern, criteria.isCaseSensitive(), criteria.isRegexMode());
    }

    /**
     * Checks if the entry matches the response presence filter.
     */
    private boolean matchesResponseFilter(RequestResponseEntry entry, FilterCriteria criteria) {
        if (!criteria.isOnlyWithResponses() && !criteria.isRequireResponse()) {
            return true;
        }

        return entry.getResponse() != null && !"Pending".equals(entry.getStatusCode());
    }

    /**
     * Checks if the entry matches the advanced method filter.
     */
    private boolean matchesAdvancedMethodFilter(RequestResponseEntry entry, FilterCriteria criteria) {
        Set<String> allowedMethods = criteria.getAllowedMethods();
        if (allowedMethods == null || allowedMethods.isEmpty()) {
            return true;
        }

        return allowedMethods.contains(entry.getMethod());
    }

    /**
     * Checks if the entry matches the advanced status filter.
     */
    private boolean matchesAdvancedStatusFilter(RequestResponseEntry entry, FilterCriteria criteria) {
        Set<Integer> allowedStatusPrefixes = criteria.getAllowedStatusPrefixes();
        if (allowedStatusPrefixes == null || allowedStatusPrefixes.isEmpty()) {
            return true;
        }

        String statusCode = entry.getStatusCode();
        if ("Pending".equals(statusCode)) {
            return false;
        }

        try {
            int prefix = Integer.parseInt(statusCode) / 100;
            return allowedStatusPrefixes.contains(prefix);
        } catch (NumberFormatException e) {
            logging.logToError("Invalid status code format: " + statusCode);
            return false;
        }
    }

    /**
     * Checks if the entry matches the MIME type filter.
     */
    private boolean matchesMimeTypeFilter(RequestResponseEntry entry, FilterCriteria criteria) {
        Set<String> allowedMimeTypes = criteria.getAllowedMimeTypes();
        if (allowedMimeTypes == null || allowedMimeTypes.isEmpty()) {
            return true;
        }

        if (entry.getResponse() == null) {
            return false;
        }

        try {
            String mimeType = extractMimeTypeFromResponse(entry.getResponse());
            if (mimeType == null || mimeType.isEmpty()) {
                return false;
            }

            return allowedMimeTypes.contains(mimeType);

        } catch (Exception e) {
            logging.logToError("Error extracting MIME type: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if the entry matches the extension filter.
     */
    private boolean matchesExtensionFilter(RequestResponseEntry entry, FilterCriteria criteria) {
        Set<String> includedExtensions = criteria.getIncludedExtensions();
        Set<String> excludedExtensions = criteria.getExcludedExtensions();

        if ((includedExtensions == null || includedExtensions.isEmpty()) &&
            (excludedExtensions == null || excludedExtensions.isEmpty())) {
            return true;
        }

        String extension = HttpUtils.getFileExtension(entry.getPath());

        if (excludedExtensions != null && !excludedExtensions.isEmpty()) {
            if (excludedExtensions.contains(extension)) {
                return false;
            }
        }

        if (includedExtensions != null && !includedExtensions.isEmpty()) {
            return includedExtensions.contains(extension);
        }

        return true;
    }

    /**
     * Checks if the entry matches the scope filter.
     */
    private boolean matchesScopeFilter(RequestResponseEntry entry, FilterCriteria criteria) {
        if (!criteria.isOnlyInScope()) {
            return true;
        }

        if (montoyaApi == null) {
            logging.logToError("Scope filter requested but MontoyaApi not available - failing open");
            return true;
        }

        try {
            HttpRequest request = entry.getRequest();
            if (request == null) {
                return false;
            }

            return montoyaApi.scope().isInScope(request.url());

        } catch (Exception e) {
            logging.logToError("Error checking Burp scope: " + e.getMessage());
            return true;
        }
    }

    /**
     * Extracts MIME type from HTTP response Content-Type header.
     *
     * @param response The HTTP response to extract MIME type from
     * @return The MIME type or null if not found or invalid
     */
    private String extractMimeTypeFromResponse(HttpResponse response) {
        if (response == null) {
            return null;
        }

        try {
            String contentType = response.headerValue("Content-Type");
            if (contentType == null || contentType.trim().isEmpty()) {
                return null;
            }

            int semicolonIndex = contentType.indexOf(';');
            String mimeType = semicolonIndex != -1 ?
                    contentType.substring(0, semicolonIndex).trim() :
                    contentType.trim();

            return mimeType.toLowerCase();

        } catch (Exception e) {
            logging.logToError("Error parsing Content-Type header: " + e.getMessage());
            return null;
        }
    }

    /**
     * Performs text pattern matching with support for case sensitivity and regex.
     *
     * @param text          The text to match against
     * @param pattern       The pattern to match
     * @param caseSensitive Whether matching should be case sensitive
     * @param regexMode     Whether to use regex matching
     * @return true if the text matches the pattern
     */
    private boolean matchesTextPattern(String text, String pattern, boolean caseSensitive, boolean regexMode) {
        if (text == null || pattern == null) {
            return true;
        }

        try {
            if (regexMode) {
                int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                return Pattern.compile(pattern, flags).matcher(text).find();
            } else {
                String searchText = caseSensitive ? text : text.toLowerCase();
                String searchPattern = caseSensitive ? pattern : pattern.toLowerCase();
                return searchText.contains(searchPattern);
            }
        } catch (PatternSyntaxException e) {
            logging.logToError("Invalid regex pattern: " + pattern + " - " + e.getMessage());
            String searchText = caseSensitive ? text : text.toLowerCase();
            String searchPattern = caseSensitive ? pattern : pattern.toLowerCase();
            return searchText.contains(searchPattern);
        }
    }
}
