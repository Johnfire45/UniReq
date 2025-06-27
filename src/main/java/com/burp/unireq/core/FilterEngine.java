package com.burp.unireq.core;

import com.burp.unireq.model.FilterCriteria;
import com.burp.unireq.model.RequestResponseEntry;
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
     * @param logging Burp's logging interface for debug output and error reporting
     * @param montoyaApi Burp's Montoya API for scope checking and advanced features (can be null initially)
     */
    public FilterEngine(Logging logging, MontoyaApi montoyaApi) {
        this.logging = logging;
        this.montoyaApi = montoyaApi; // Can be null initially, will be set later via setApi()
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
     * @param entries The list of entries to filter
     * @param criteria The filter criteria to apply
     * @return A new list containing only entries that match the criteria
     */
    public List<RequestResponseEntry> filterRequests(List<RequestResponseEntry> entries, FilterCriteria criteria) {
        if (entries == null || entries.isEmpty()) {
            return new ArrayList<>();
        }
        
        if (criteria == null || !criteria.hasActiveFilters()) {
            // No filters active, return all entries
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
     * @param entry The entry to check
     * @param criteria The filter criteria
     * @return true if the entry matches all active filters
     */
    public boolean matchesFilters(RequestResponseEntry entry, FilterCriteria criteria) {
        if (entry == null || criteria == null) {
            return true; // No filtering if entry or criteria is null
        }
        
        try {
            // Method filter
            if (!matchesMethodFilter(entry, criteria)) {
                return false;
            }
            
            // Status code filter
            if (!matchesStatusFilter(entry, criteria)) {
                return false;
            }
            
            // Host filter
            if (!matchesHostFilter(entry, criteria)) {
                return false;
            }
            
            // Path filter
            if (!matchesPathFilter(entry, criteria)) {
                return false;
            }
            
            // Response presence filter
            if (!matchesResponseFilter(entry, criteria)) {
                return false;
            }
            
            // Highlighted filter (placeholder for future implementation)
            if (!matchesHighlightedFilter(entry, criteria)) {
                return false;
            }
            
            // Advanced method filter
            if (!matchesAdvancedMethodFilter(entry, criteria)) {
                return false;
            }
            
            // Advanced status filter
            if (!matchesAdvancedStatusFilter(entry, criteria)) {
                return false;
            }
            
            // MIME type filter
            if (!matchesMimeTypeFilter(entry, criteria)) {
                return false;
            }
            
            // Extension filter
            if (!matchesExtensionFilter(entry, criteria)) {
                return false;
            }
            
            // Scope filter
            if (!matchesScopeFilter(entry, criteria)) {
                return false;
            }
            
            // Advanced response filter
            if (!matchesAdvancedResponseFilter(entry, criteria)) {
                return false;
            }
            
            return true; // All filters passed
            
        } catch (Exception e) {
            logging.logToError("Error applying filters: " + e.getMessage());
            return true; // In case of error, include the entry
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
        
        String entryMethod = entry.getMethod();
        return methodFilter.equalsIgnoreCase(entryMethod);
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
            return false; // Pending responses don't match any status filter
        }
        
        try {
            int statusCode = Integer.parseInt(entryStatus);
            
            // Check for range filters (2xx, 3xx, 4xx, 5xx)
            if (statusFilter.endsWith("xx")) {
                int rangeStart = Integer.parseInt(statusFilter.substring(0, 1)) * 100;
                return statusCode >= rangeStart && statusCode < rangeStart + 100;
            }
            
            // Check for exact status code match
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
        
        // Apply inversion if requested
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
        
        String path = entry.getPath();
        return matchesTextPattern(path, pathPattern, criteria.isCaseSensitive(), criteria.isRegexMode());
    }
    
    /**
     * Checks if the entry matches the response presence filter.
     */
    private boolean matchesResponseFilter(RequestResponseEntry entry, FilterCriteria criteria) {
        if (!criteria.isOnlyWithResponses()) {
            return true; // Filter not active
        }
        
        return entry.getResponse() != null && !"Pending".equals(entry.getStatusCode());
    }
    
    /**
     * Checks if the entry matches the highlighted filter.
     * This is a placeholder for future implementation.
     */
    private boolean matchesHighlightedFilter(RequestResponseEntry entry, FilterCriteria criteria) {
        if (!criteria.isOnlyHighlighted()) {
            return true; // Filter not active
        }
        
        // TODO: Implement highlighting system
        // For now, return true to not filter out any entries
        return true;
    }
    
    /**
     * Checks if the entry matches the advanced method filter.
     */
    private boolean matchesAdvancedMethodFilter(RequestResponseEntry entry, FilterCriteria criteria) {
        Set<String> allowedMethods = criteria.getAllowedMethods();
        if (allowedMethods == null || allowedMethods.isEmpty()) {
            return true; // No filter active
        }
        
        String method = entry.getMethod();
        return allowedMethods.contains(method);
    }
    
    /**
     * Checks if the entry matches the advanced status filter.
     */
    private boolean matchesAdvancedStatusFilter(RequestResponseEntry entry, FilterCriteria criteria) {
        Set<Integer> allowedStatusPrefixes = criteria.getAllowedStatusPrefixes();
        if (allowedStatusPrefixes == null || allowedStatusPrefixes.isEmpty()) {
            return true; // No filter active
        }
        
        String statusCode = entry.getStatusCode();
        if ("Pending".equals(statusCode)) {
            return false; // Pending responses don't match any status filter
        }
        
        try {
            int status = Integer.parseInt(statusCode);
            int prefix = status / 100; // Extract prefix (2xx -> 2, 4xx -> 4, etc.)
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
            return true; // No filter active
        }
        
        // Extract MIME type from response (if available)
        if (entry.getResponse() == null) {
            return false; // No response, can't determine MIME type
        }
        
        try {
            // Extract MIME type from Content-Type header
            String mimeType = extractMimeTypeFromResponse(entry.getResponse());
            if (mimeType == null || mimeType.isEmpty()) {
                return false; // No valid MIME type found
            }
            
            // Check if the extracted MIME type matches any allowed types
            return allowedMimeTypes.contains(mimeType);
            
        } catch (Exception e) {
            logging.logToError("Error extracting MIME type: " + e.getMessage());
            return false; // Fail safely - exclude entry if MIME type can't be determined
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
            return true; // No filter active
        }
        
        // Extract file extension from path
        String path = entry.getPath();
        String extension = getFileExtension(path);
        
        // Check exclusions first
        if (excludedExtensions != null && !excludedExtensions.isEmpty()) {
            if (excludedExtensions.contains(extension)) {
                return false;
            }
        }
        
        // Check inclusions
        if (includedExtensions != null && !includedExtensions.isEmpty()) {
            return includedExtensions.contains(extension);
        }
        
        return true; // No inclusion filter, just exclusion passed
    }
    
    /**
     * Checks if the entry matches the scope filter.
     */
    private boolean matchesScopeFilter(RequestResponseEntry entry, FilterCriteria criteria) {
        if (!criteria.isOnlyInScope()) {
            return true; // Filter not active
        }
        
        // If API is not available yet, fail-open (include all entries)
        if (montoyaApi == null) {
            logging.logToError("Scope filter requested but MontoyaApi not available - failing open");
            return true;
        }
        
        try {
            HttpRequest request = entry.getRequest();
            if (request == null) {
                return false; // No request, can't check scope
            }
            
            // Use Montoya API to check if request URL is in Burp's scope
            boolean inScope = montoyaApi.scope().isInScope(request.url());
            return inScope;
            
        } catch (Exception e) {
            logging.logToError("Error checking Burp scope: " + e.getMessage());
            return true; // Fail-open: include entry if scope check fails
        }
    }
    
    /**
     * Checks if the entry matches the advanced response filter.
     */
    private boolean matchesAdvancedResponseFilter(RequestResponseEntry entry, FilterCriteria criteria) {
        if (!criteria.isRequireResponse()) {
            return true; // Filter not active
        }
        
        return entry.getResponse() != null && !"Pending".equals(entry.getStatusCode());
    }
    
    /**
     * Extracts MIME type from HTTP response Content-Type header.
     * 
     * This method parses the Content-Type header to extract the primary MIME type,
     * removing any additional parameters like charset or boundary.
     * 
     * Examples:
     * - "text/html; charset=utf-8" → "text/html"
     * - "application/json" → "application/json"
     * - "image/png" → "image/png"
     * - "multipart/form-data; boundary=something" → "multipart/form-data"
     * 
     * @param response The HTTP response to extract MIME type from
     * @return The MIME type or null if not found or invalid
     */
    private String extractMimeTypeFromResponse(HttpResponse response) {
        if (response == null) {
            return null;
        }
        
        try {
            // Get Content-Type header (case-insensitive lookup)
            String contentType = response.headerValue("Content-Type");
            if (contentType == null || contentType.trim().isEmpty()) {
                return null; // No Content-Type header found
            }
            
            // Extract MIME type (part before semicolon)
            // Remove any parameters like charset, boundary, etc.
            int semicolonIndex = contentType.indexOf(';');
            String mimeType = semicolonIndex != -1 ? 
                contentType.substring(0, semicolonIndex).trim() : 
                contentType.trim();
                
            // Normalize to lowercase for consistent matching
            return mimeType.toLowerCase();
            
        } catch (Exception e) {
            logging.logToError("Error parsing Content-Type header: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Extracts file extension from a URL path.
     * 
     * @param path The URL path
     * @return The file extension (without dot) or empty string if none
     */
    private String getFileExtension(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        
        // Remove query parameters and fragments
        int queryIndex = path.indexOf('?');
        if (queryIndex != -1) {
            path = path.substring(0, queryIndex);
        }
        
        int fragmentIndex = path.indexOf('#');
        if (fragmentIndex != -1) {
            path = path.substring(0, fragmentIndex);
        }
        
        // Extract extension
        int lastDotIndex = path.lastIndexOf('.');
        int lastSlashIndex = path.lastIndexOf('/');
        
        if (lastDotIndex > lastSlashIndex && lastDotIndex < path.length() - 1) {
            return path.substring(lastDotIndex + 1).toLowerCase();
        }
        
        return "";
    }
    
    /**
     * Performs text pattern matching with support for case sensitivity and regex.
     * 
     * @param text The text to match against
     * @param pattern The pattern to match
     * @param caseSensitive Whether matching should be case sensitive
     * @param regexMode Whether to use regex matching
     * @return true if the text matches the pattern
     */
    private boolean matchesTextPattern(String text, String pattern, boolean caseSensitive, boolean regexMode) {
        if (text == null || pattern == null) {
            return true;
        }
        
        try {
            if (regexMode) {
                // Use regex matching
                int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                Pattern regexPattern = Pattern.compile(pattern, flags);
                return regexPattern.matcher(text).find();
            } else {
                // Use simple substring matching
                String searchText = caseSensitive ? text : text.toLowerCase();
                String searchPattern = caseSensitive ? pattern : pattern.toLowerCase();
                return searchText.contains(searchPattern);
            }
        } catch (PatternSyntaxException e) {
            logging.logToError("Invalid regex pattern: " + pattern + " - " + e.getMessage());
            // Fall back to simple substring matching
            String searchText = caseSensitive ? text : text.toLowerCase();
            String searchPattern = caseSensitive ? pattern : pattern.toLowerCase();
            return searchText.contains(searchPattern);
        }
    }
    
    /**
     * Validates filter criteria for correctness.
     * 
     * @param criteria The filter criteria to validate
     * @return true if the criteria is valid, false otherwise
     */
    public boolean validateCriteria(FilterCriteria criteria) {
        if (criteria == null) {
            return true; // Null criteria is valid (no filtering)
        }
        
        try {
            // Validate regex patterns if regex mode is enabled
            if (criteria.isRegexMode()) {
                if (!criteria.getHostPattern().isEmpty()) {
                    Pattern.compile(criteria.getHostPattern());
                }
                if (!criteria.getPathPattern().isEmpty()) {
                    Pattern.compile(criteria.getPathPattern());
                }
            }
            
            return true;
        } catch (PatternSyntaxException e) {
            logging.logToError("Invalid filter criteria: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Creates a summary of active filters for display purposes.
     * 
     * @param criteria The filter criteria
     * @return A human-readable summary of active filters
     */
    public String getFilterSummary(FilterCriteria criteria) {
        if (criteria == null || !criteria.hasActiveFilters()) {
            return "No filters active";
        }
        
        StringBuilder summary = new StringBuilder();
        
        if (!"All".equals(criteria.getMethod())) {
            summary.append("Method: ").append(criteria.getMethod()).append("; ");
        }
        
        if (!"All".equals(criteria.getStatusCode())) {
            summary.append("Status: ").append(criteria.getStatusCode()).append("; ");
        }
        
        if (!criteria.getHostPattern().isEmpty()) {
            summary.append("Host: ").append(criteria.getHostPattern()).append("; ");
        }
        
        if (!criteria.getPathPattern().isEmpty()) {
            summary.append("Path: ").append(criteria.getPathPattern()).append("; ");
        }
        
        if (criteria.isOnlyWithResponses()) {
            summary.append("With responses only; ");
        }
        
        if (criteria.isOnlyHighlighted()) {
            summary.append("Highlighted only; ");
        }
        
        // Remove trailing "; "
        if (summary.length() > 2) {
            summary.setLength(summary.length() - 2);
        }
        
        return summary.toString();
    }
    
    /**
     * Applies highlighting to matching entries based on filter criteria.
     * This feature is planned for future implementation.
     * 
     * @param entries The list of entries to highlight
     * @param criteria The filter criteria
     * @return The list of entries with highlighting applied (currently returns original list)
     */
    public List<RequestResponseEntry> applyHighlighting(List<RequestResponseEntry> entries, FilterCriteria criteria) {
        // Highlighting system planned for future implementation
        // Would mark matching entries with visual indicators
        return entries;
    }
} 