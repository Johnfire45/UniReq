package com.burp.extension.unireq.model;

/**
 * Encapsulates filter criteria for HTTP request filtering.
 * 
 * This model class contains all the parameters used to filter
 * HTTP requests in the UniReq extension. It provides a clean
 * interface for passing filter settings between UI components
 * and the filtering service.
 * 
 * Filter Types:
 * - HTTP Method filtering (GET, POST, etc.)
 * - Status code filtering (2xx, 3xx, 4xx, 5xx, specific codes)
 * - Host pattern matching (with regex support)
 * - Path pattern matching (with regex support)
 * - Response presence filtering
 * - Highlighted items filtering
 * - Case sensitivity options
 * - Regex mode toggle
 * 
 * @author Harshit Shah
 */
public class FilterCriteria {
    
    // Method filter
    private String method = "All";
    
    // Status code filter
    private String statusCode = "All";
    
    // Text-based filters
    private String hostPattern = "";
    private String pathPattern = "";
    
    // Boolean filters
    private boolean onlyWithResponses = false;
    private boolean onlyHighlighted = false;
    
    // Filter behavior options
    private boolean caseSensitive = false;
    private boolean regexMode = false;
    
    /**
     * Default constructor creates a filter criteria with no active filters.
     * All requests will pass through when using default settings.
     */
    public FilterCriteria() {
        // Default values already set above
    }
    
    /**
     * Constructor with all parameters for complete filter configuration.
     * 
     * @param method HTTP method filter ("All", "GET", "POST", etc.)
     * @param statusCode Status code filter ("All", "2xx", "404", etc.)
     * @param hostPattern Host pattern for matching
     * @param pathPattern Path pattern for matching
     * @param onlyWithResponses Whether to show only requests with responses
     * @param onlyHighlighted Whether to show only highlighted requests
     * @param caseSensitive Whether text matching is case sensitive
     * @param regexMode Whether to use regex for text matching
     */
    public FilterCriteria(String method, String statusCode, String hostPattern, String pathPattern,
                         boolean onlyWithResponses, boolean onlyHighlighted, 
                         boolean caseSensitive, boolean regexMode) {
        this.method = method != null ? method : "All";
        this.statusCode = statusCode != null ? statusCode : "All";
        this.hostPattern = hostPattern != null ? hostPattern : "";
        this.pathPattern = pathPattern != null ? pathPattern : "";
        this.onlyWithResponses = onlyWithResponses;
        this.onlyHighlighted = onlyHighlighted;
        this.caseSensitive = caseSensitive;
        this.regexMode = regexMode;
    }
    
    // ==================== Getters and Setters ====================
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method != null ? method : "All";
    }
    
    public String getStatusCode() {
        return statusCode;
    }
    
    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode != null ? statusCode : "All";
    }
    
    public String getHostPattern() {
        return hostPattern;
    }
    
    public void setHostPattern(String hostPattern) {
        this.hostPattern = hostPattern != null ? hostPattern : "";
    }
    
    public String getPathPattern() {
        return pathPattern;
    }
    
    public void setPathPattern(String pathPattern) {
        this.pathPattern = pathPattern != null ? pathPattern : "";
    }
    
    public boolean isOnlyWithResponses() {
        return onlyWithResponses;
    }
    
    public void setOnlyWithResponses(boolean onlyWithResponses) {
        this.onlyWithResponses = onlyWithResponses;
    }
    
    public boolean isOnlyHighlighted() {
        return onlyHighlighted;
    }
    
    public void setOnlyHighlighted(boolean onlyHighlighted) {
        this.onlyHighlighted = onlyHighlighted;
    }
    
    public boolean isCaseSensitive() {
        return caseSensitive;
    }
    
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }
    
    public boolean isRegexMode() {
        return regexMode;
    }
    
    public void setRegexMode(boolean regexMode) {
        this.regexMode = regexMode;
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Checks if any filters are currently active (non-default values).
     * 
     * @return true if any filter is set to a non-default value
     */
    public boolean hasActiveFilters() {
        return !method.equals("All") ||
               !statusCode.equals("All") ||
               !hostPattern.trim().isEmpty() ||
               !pathPattern.trim().isEmpty() ||
               onlyWithResponses ||
               onlyHighlighted;
    }
    
    /**
     * Resets all filters to their default (inactive) state.
     */
    public void clearAllFilters() {
        this.method = "All";
        this.statusCode = "All";
        this.hostPattern = "";
        this.pathPattern = "";
        this.onlyWithResponses = false;
        this.onlyHighlighted = false;
        this.caseSensitive = false;
        this.regexMode = false;
    }
    
    /**
     * Creates a copy of this FilterCriteria with the same settings.
     * 
     * @return A new FilterCriteria instance with identical settings
     */
    public FilterCriteria copy() {
        return new FilterCriteria(method, statusCode, hostPattern, pathPattern,
                                onlyWithResponses, onlyHighlighted, caseSensitive, regexMode);
    }
    
    // ==================== Object Methods ====================
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        FilterCriteria that = (FilterCriteria) obj;
        
        return onlyWithResponses == that.onlyWithResponses &&
               onlyHighlighted == that.onlyHighlighted &&
               caseSensitive == that.caseSensitive &&
               regexMode == that.regexMode &&
               method.equals(that.method) &&
               statusCode.equals(that.statusCode) &&
               hostPattern.equals(that.hostPattern) &&
               pathPattern.equals(that.pathPattern);
    }
    
    @Override
    public int hashCode() {
        int result = method.hashCode();
        result = 31 * result + statusCode.hashCode();
        result = 31 * result + hostPattern.hashCode();
        result = 31 * result + pathPattern.hashCode();
        result = 31 * result + (onlyWithResponses ? 1 : 0);
        result = 31 * result + (onlyHighlighted ? 1 : 0);
        result = 31 * result + (caseSensitive ? 1 : 0);
        result = 31 * result + (regexMode ? 1 : 0);
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("FilterCriteria{method='%s', status='%s', host='%s', path='%s', " +
                           "withResponses=%s, highlighted=%s, caseSensitive=%s, regex=%s}",
                           method, statusCode, hostPattern, pathPattern,
                           onlyWithResponses, onlyHighlighted, caseSensitive, regexMode);
    }
} 