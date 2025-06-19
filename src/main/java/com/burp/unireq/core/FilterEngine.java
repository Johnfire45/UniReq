package com.burp.unireq.core;

import com.burp.unireq.model.FilterCriteria;
import com.burp.unireq.model.RequestResponseEntry;
import burp.api.montoya.logging.Logging;

import java.util.List;
import java.util.ArrayList;
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
    
    /**
     * Constructor initializes the filter engine with logging support.
     * 
     * @param logging Burp's logging interface for debug output and error reporting
     */
    public FilterEngine(Logging logging) {
        this.logging = logging;
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
        return matchesTextPattern(host, hostPattern, criteria.isCaseSensitive(), criteria.isRegexMode());
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
} 