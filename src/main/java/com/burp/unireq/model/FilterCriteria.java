package com.burp.unireq.model;

import java.util.HashSet;
import java.util.Set;

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
    private boolean invertHostFilter = false;
    
    // Advanced filter options
    private Set<String> allowedMethods = new HashSet<>();
    private Set<String> allowedMimeTypes = new HashSet<>();
    private Set<String> includedExtensions = new HashSet<>();
    private Set<String> excludedExtensions = new HashSet<>();
    private Set<Integer> allowedStatusPrefixes = new HashSet<>();
    private boolean requireResponse = false;
    private boolean onlyInScope = false;
    
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
     * @param invertHostFilter Whether to invert host filter matching
     * @param allowedMethods Set of allowed HTTP methods
     * @param allowedMimeTypes Set of allowed MIME types
     * @param includedExtensions Set of included file extensions
     * @param excludedExtensions Set of excluded file extensions
     * @param allowedStatusPrefixes Set of allowed status code prefixes
     * @param requireResponse Whether to require response presence
     * @param onlyInScope Whether to show only in-scope items
     */
    public FilterCriteria(String method, String statusCode, String hostPattern, String pathPattern,
                         boolean onlyWithResponses, boolean onlyHighlighted, 
                         boolean caseSensitive, boolean regexMode, boolean invertHostFilter,
                         Set<String> allowedMethods, Set<String> allowedMimeTypes,
                         Set<String> includedExtensions, Set<String> excludedExtensions,
                         Set<Integer> allowedStatusPrefixes, boolean requireResponse, boolean onlyInScope) {
        this.method = method != null ? method : "All";
        this.statusCode = statusCode != null ? statusCode : "All";
        this.hostPattern = hostPattern != null ? hostPattern : "";
        this.pathPattern = pathPattern != null ? pathPattern : "";
        this.onlyWithResponses = onlyWithResponses;
        this.onlyHighlighted = onlyHighlighted;
        this.caseSensitive = caseSensitive;
        this.regexMode = regexMode;
        this.invertHostFilter = invertHostFilter;
        this.allowedMethods = allowedMethods != null ? allowedMethods : new HashSet<>();
        this.allowedMimeTypes = allowedMimeTypes != null ? allowedMimeTypes : new HashSet<>();
        this.includedExtensions = includedExtensions != null ? includedExtensions : new HashSet<>();
        this.excludedExtensions = excludedExtensions != null ? excludedExtensions : new HashSet<>();
        this.allowedStatusPrefixes = allowedStatusPrefixes != null ? allowedStatusPrefixes : new HashSet<>();
        this.requireResponse = requireResponse;
        this.onlyInScope = onlyInScope;
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
    
    public boolean isInvertHostFilter() {
        return invertHostFilter;
    }
    
    public void setInvertHostFilter(boolean invertHostFilter) {
        this.invertHostFilter = invertHostFilter;
    }
    
    // ==================== Advanced Filter Getters/Setters ====================
    
    public Set<String> getAllowedMethods() {
        return allowedMethods;
    }
    
    public void setAllowedMethods(Set<String> allowedMethods) {
        this.allowedMethods = allowedMethods != null ? allowedMethods : new HashSet<>();
    }
    
    public Set<String> getAllowedMimeTypes() {
        return allowedMimeTypes;
    }
    
    public void setAllowedMimeTypes(Set<String> allowedMimeTypes) {
        this.allowedMimeTypes = allowedMimeTypes != null ? allowedMimeTypes : new HashSet<>();
    }
    
    public Set<String> getIncludedExtensions() {
        return includedExtensions;
    }
    
    public void setIncludedExtensions(Set<String> includedExtensions) {
        this.includedExtensions = includedExtensions != null ? includedExtensions : new HashSet<>();
    }
    
    public Set<String> getExcludedExtensions() {
        return excludedExtensions;
    }
    
    public void setExcludedExtensions(Set<String> excludedExtensions) {
        this.excludedExtensions = excludedExtensions != null ? excludedExtensions : new HashSet<>();
    }
    
    public Set<Integer> getAllowedStatusPrefixes() {
        return allowedStatusPrefixes;
    }
    
    public void setAllowedStatusPrefixes(Set<Integer> allowedStatusPrefixes) {
        this.allowedStatusPrefixes = allowedStatusPrefixes != null ? allowedStatusPrefixes : new HashSet<>();
    }
    
    public boolean isRequireResponse() {
        return requireResponse;
    }
    
    public void setRequireResponse(boolean requireResponse) {
        this.requireResponse = requireResponse;
    }
    
    public boolean isOnlyInScope() {
        return onlyInScope;
    }
    
    public void setOnlyInScope(boolean onlyInScope) {
        this.onlyInScope = onlyInScope;
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
        this.invertHostFilter = false;
        this.allowedMethods.clear();
        this.allowedMimeTypes.clear();
        this.includedExtensions.clear();
        this.excludedExtensions.clear();
        this.allowedStatusPrefixes.clear();
        this.requireResponse = false;
        this.onlyInScope = false;
    }
    
    /**
     * Creates a copy of this FilterCriteria with the same settings.
     * 
     * @return A new FilterCriteria instance with identical settings
     */
    public FilterCriteria copy() {
        return new FilterCriteria(method, statusCode, hostPattern, pathPattern,
                                onlyWithResponses, onlyHighlighted, caseSensitive, regexMode, invertHostFilter,
                                new HashSet<>(allowedMethods), new HashSet<>(allowedMimeTypes),
                                new HashSet<>(includedExtensions), new HashSet<>(excludedExtensions),
                                new HashSet<>(allowedStatusPrefixes), requireResponse, onlyInScope);
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
               invertHostFilter == that.invertHostFilter &&
               requireResponse == that.requireResponse &&
               onlyInScope == that.onlyInScope &&
               method.equals(that.method) &&
               statusCode.equals(that.statusCode) &&
               hostPattern.equals(that.hostPattern) &&
               pathPattern.equals(that.pathPattern) &&
               allowedMethods.equals(that.allowedMethods) &&
               allowedMimeTypes.equals(that.allowedMimeTypes) &&
               includedExtensions.equals(that.includedExtensions) &&
               excludedExtensions.equals(that.excludedExtensions) &&
               allowedStatusPrefixes.equals(that.allowedStatusPrefixes);
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
        result = 31 * result + (invertHostFilter ? 1 : 0);
        result = 31 * result + (requireResponse ? 1 : 0);
        result = 31 * result + (onlyInScope ? 1 : 0);
        result = 31 * result + allowedMethods.hashCode();
        result = 31 * result + allowedMimeTypes.hashCode();
        result = 31 * result + includedExtensions.hashCode();
        result = 31 * result + excludedExtensions.hashCode();
        result = 31 * result + allowedStatusPrefixes.hashCode();
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("FilterCriteria{method='%s', status='%s', host='%s', path='%s', " +
                           "withResponses=%s, highlighted=%s, caseSensitive=%s, regex=%s, invertHost=%s}",
                           method, statusCode, hostPattern, pathPattern,
                           onlyWithResponses, onlyHighlighted, caseSensitive, regexMode, invertHostFilter);
    }
} 