package com.burp.extension.unireq;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.logging.Logging;
import com.burp.extension.unireq.model.RequestResponseEntry;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.List;
import java.util.ArrayList;

/**
 * RequestDeduplicator - Core Engine for HTTP Request Deduplication
 * 
 * This class implements the main logic for identifying and filtering duplicate HTTP requests.
 * It computes unique fingerprints for each request and maintains a thread-safe collection
 * of seen fingerprints to detect duplicates.
 * 
 * Fingerprint Format: METHOD | NORMALIZED_PATH | HASH(CONTENT)
 * - METHOD: HTTP method (GET, POST, etc.)
 * - NORMALIZED_PATH: URL path with trailing slashes removed and normalized
 * - HASH: SHA-256 hash of request body (for POST/PUT) or query string (for GET)
 * 
 * Thread Safety:
 * - Uses ConcurrentSkipListSet for thread-safe fingerprint storage
 * - AtomicBoolean for filtering state
 * - AtomicLong for counters
 */
public class RequestDeduplicator {
    
    // Constants for fingerprint computation
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String FINGERPRINT_SEPARATOR = " | ";
    private static final String EMPTY_HASH = "EMPTY";
    
    // Memory management constants
    private static final int MAX_STORED_REQUESTS = 1000; // Cap to prevent memory issues
    
    // Thread-safe storage for seen request fingerprints
    // ConcurrentSkipListSet provides thread-safe operations and maintains sorted order
    private final ConcurrentSkipListSet<String> seenFingerprints;
    
    // Thread-safe storage for unique request/response pairs (FIFO queue for memory management)
    private final ConcurrentLinkedQueue<RequestResponseEntry> storedRequests;
    
    // Configuration and state
    private final AtomicBoolean filteringEnabled;
    private final Logging logging;
    
    // Statistics tracking
    private final AtomicLong totalRequests;
    private final AtomicLong uniqueRequests;
    private final AtomicLong duplicateRequests;
    
    // Note: RequestResponseEntry has been moved to com.burp.extension.unireq.model.RequestResponseEntry
    
    /**
     * Constructor initializes the deduplication engine with logging support.
     * 
     * @param logging Burp's logging interface for debug output and error reporting
     */
    public RequestDeduplicator(Logging logging) {
        this.logging = logging;
        this.seenFingerprints = new ConcurrentSkipListSet<>();
        this.storedRequests = new ConcurrentLinkedQueue<>();
        this.filteringEnabled = new AtomicBoolean(true); // Default to enabled
        this.totalRequests = new AtomicLong(0);
        this.uniqueRequests = new AtomicLong(0);
        this.duplicateRequests = new AtomicLong(0);
        
        logging.logToOutput("RequestDeduplicator initialized with filtering enabled");
    }
    
    /**
     * Processes an HTTP request to determine if it's a duplicate.
     * 
     * This method:
     * 1. Increments the total request counter
     * 2. Computes a unique fingerprint for the request
     * 3. Checks if the fingerprint has been seen before
     * 4. Updates statistics and returns the result
     * 5. Stores unique requests for GUI display
     * 
     * @param request The HTTP request to analyze
     * @return true if this is a unique request, false if it's a duplicate
     */
    public boolean isUniqueRequest(HttpRequest request) {
        totalRequests.incrementAndGet();
        
        try {
            // Compute fingerprint for the request
            String fingerprint = computeFingerprint(request);
            
            // Check if filtering is enabled
            if (!filteringEnabled.get()) {
                // If filtering is disabled, consider all requests as unique
                // but still track the fingerprint for statistics
                seenFingerprints.add(fingerprint);
                storeUniqueRequest(request, fingerprint);
                uniqueRequests.incrementAndGet();
                return true;
            }
            
            // Check if we've seen this fingerprint before
            boolean isUnique = seenFingerprints.add(fingerprint);
            
            if (isUnique) {
                uniqueRequests.incrementAndGet();
                storeUniqueRequest(request, fingerprint);
                logging.logToOutput("Unique request: " + fingerprint);
            } else {
                duplicateRequests.incrementAndGet();
                logging.logToOutput("Duplicate request blocked: " + fingerprint);
            }
            
            return isUnique;
            
        } catch (Exception e) {
            // If fingerprint computation fails, err on the side of caution
            // and allow the request through while logging the error
            logging.logToError("Error processing request fingerprint: " + e.getMessage());
            uniqueRequests.incrementAndGet();
            return true;
        }
    }
    
    /**
     * Stores a unique request for GUI display with memory management.
     * Implements FIFO eviction when storage limit is reached.
     * 
     * @param request The unique HTTP request to store
     * @param fingerprint The computed fingerprint for this request
     */
    private void storeUniqueRequest(HttpRequest request, String fingerprint) {
        try {
            // Create new entry for this unique request
            RequestResponseEntry entry = new RequestResponseEntry(request, fingerprint);
            
            // Add to storage
            storedRequests.offer(entry);
            
            // Implement FIFO memory management - remove oldest entries if we exceed the limit
            while (storedRequests.size() > MAX_STORED_REQUESTS) {
                RequestResponseEntry removed = storedRequests.poll();
                if (removed != null) {
                    logging.logToOutput("Removed oldest stored request to manage memory: " + removed.getFingerprint());
                }
            }
            
        } catch (Exception e) {
            logging.logToError("Error storing unique request: " + e.getMessage());
        }
    }
    
    /**
     * Updates the response for a stored request entry.
     * This is called when a response is received for a previously stored request.
     * 
     * @param request The original request
     * @param response The received response
     */
    public void updateResponse(HttpRequest request, HttpResponse response) {
        try {
            String fingerprint = computeFingerprint(request);
            
            // Find the matching entry and update its response
            for (RequestResponseEntry entry : storedRequests) {
                if (fingerprint.equals(entry.getFingerprint())) {
                    entry.setResponse(response);
                    logging.logToOutput("Updated response for request: " + fingerprint);
                    break;
                }
            }
            
        } catch (Exception e) {
            logging.logToError("Error updating response: " + e.getMessage());
        }
    }
    
    /**
     * Returns a list of all stored unique requests for GUI display.
     * Returns a copy to prevent concurrent modification issues.
     * 
     * @return List of stored request/response entries
     */
    public List<RequestResponseEntry> getStoredRequests() {
        return new ArrayList<>(storedRequests);
    }
    
    /**
     * Computes a unique fingerprint for an HTTP request.
     * 
     * The fingerprint format is: METHOD | NORMALIZED_PATH | HASH(CONTENT)
     * 
     * Examples:
     * - GET /api/users | EMPTY
     * - POST /api/login | a1b2c3d4... (SHA-256 of request body)
     * - GET /search?q=test | e5f6g7h8... (SHA-256 of query string)
     * 
     * @param request The HTTP request to fingerprint
     * @return A unique string fingerprint for the request
     */
    private String computeFingerprint(HttpRequest request) {
        try {
            // Extract HTTP method (GET, POST, etc.)
            String method = request.method();
            
            // Extract and normalize the path
            String path = normalizePath(request.path());
            
            // Compute content hash based on request type
            String contentHash = computeContentHash(request);
            
            // Combine into final fingerprint
            String fingerprint = method + FINGERPRINT_SEPARATOR + path + FINGERPRINT_SEPARATOR + contentHash;
            
            return fingerprint;
            
        } catch (Exception e) {
            logging.logToError("Failed to compute fingerprint: " + e.getMessage());
            // Return a fallback fingerprint to prevent extension crashes
            return "ERROR_FINGERPRINT_" + System.currentTimeMillis();
        }
    }
    
    /**
     * Normalizes a URL path for consistent fingerprinting.
     * 
     * Normalization includes:
     * - Removing trailing slashes
     * - Converting to lowercase for case-insensitive comparison
     * - Handling empty/null paths
     * 
     * @param path The original URL path
     * @return The normalized path
     */
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        
        // Remove trailing slashes (except for root path)
        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        
        // Convert to lowercase for case-insensitive comparison
        return path.toLowerCase();
    }
    
    /**
     * Computes a hash of the request content for fingerprinting.
     * 
     * The content to hash depends on the request method:
     * - For requests with bodies (POST, PUT, PATCH): Hash the request body
     * - For GET requests: Hash the query string
     * - For requests with no relevant content: Return "EMPTY"
     * 
     * @param request The HTTP request
     * @return SHA-256 hash of the relevant content, or "EMPTY" if no content
     */
    private String computeContentHash(HttpRequest request) {
        try {
            String contentToHash = null;
            
            // Determine what content to hash based on request method
            if (hasRequestBody(request)) {
                // For requests with bodies, hash the body content
                byte[] bodyBytes = request.body().getBytes();
                if (bodyBytes.length > 0) {
                    contentToHash = new String(bodyBytes, StandardCharsets.UTF_8);
                }
            } else if ("GET".equalsIgnoreCase(request.method())) {
                // For GET requests, hash the query string
                String query = request.query();
                if (query != null && !query.isEmpty()) {
                    contentToHash = query;
                }
            }
            
            // If no content to hash, return empty indicator
            if (contentToHash == null || contentToHash.isEmpty()) {
                return EMPTY_HASH;
            }
            
            // Compute SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(contentToHash.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            logging.logToError("SHA-256 algorithm not available: " + e.getMessage());
            return "HASH_ERROR";
        } catch (Exception e) {
            logging.logToError("Error computing content hash: " + e.getMessage());
            return "HASH_ERROR";
        }
    }
    
    /**
     * Determines if an HTTP request has a body that should be hashed.
     * 
     * @param request The HTTP request
     * @return true if the request has a body, false otherwise
     */
    private boolean hasRequestBody(HttpRequest request) {
        String method = request.method().toUpperCase();
        return "POST".equals(method) || "PUT".equals(method) || 
               "PATCH".equals(method) || "DELETE".equals(method);
    }
    
    // Configuration methods
    
    /**
     * Enables or disables request filtering.
     * 
     * @param enabled true to enable filtering, false to disable
     */
    public void setFilteringEnabled(boolean enabled) {
        filteringEnabled.set(enabled);
        logging.logToOutput("Request filtering " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Checks if request filtering is currently enabled.
     * 
     * @return true if filtering is enabled, false otherwise
     */
    public boolean isFilteringEnabled() {
        return filteringEnabled.get();
    }
    
    /**
     * Clears all stored fingerprints, stored requests, and resets statistics.
     * This is useful for starting fresh or freeing memory.
     */
    public void clearFingerprints() {
        seenFingerprints.clear();
        storedRequests.clear();
        totalRequests.set(0);
        uniqueRequests.set(0);
        duplicateRequests.set(0);
        logging.logToOutput("All fingerprints, stored requests cleared and statistics reset");
    }
    
    // Statistics methods
    
    /**
     * Gets the total number of requests processed.
     * 
     * @return Total request count
     */
    public long getTotalRequests() {
        return totalRequests.get();
    }
    
    /**
     * Gets the number of unique requests seen.
     * 
     * @return Unique request count
     */
    public long getUniqueRequests() {
        return uniqueRequests.get();
    }
    
    /**
     * Gets the number of duplicate requests detected.
     * 
     * @return Duplicate request count
     */
    public long getDuplicateRequests() {
        return duplicateRequests.get();
    }
    
    /**
     * Gets the number of unique fingerprints stored.
     * 
     * @return Number of stored fingerprints
     */
    public int getStoredFingerprintCount() {
        return seenFingerprints.size();
    }
} 