package com.burp.unireq.core;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.logging.Logging;
import com.burp.unireq.model.RequestResponseEntry;

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
 * It uses a FingerprintGenerator to compute unique fingerprints for each request and maintains 
 * a thread-safe collection of seen fingerprints to detect duplicates.
 * 
 * Thread Safety:
 * - Uses ConcurrentSkipListSet for thread-safe fingerprint storage
 * - AtomicBoolean for filtering state
 * - AtomicLong for counters
 * 
 * @author Harshit Shah
 */
public class RequestDeduplicator {
    
    // Memory management constants
    private static final int MAX_STORED_REQUESTS = 1000; // Cap to prevent memory issues
    
    // Core dependencies
    private final FingerprintGenerator fingerprintGenerator;
    private final Logging logging;
    
    // Thread-safe storage for seen request fingerprints
    // ConcurrentSkipListSet provides thread-safe operations and maintains sorted order
    private final ConcurrentSkipListSet<String> seenFingerprints;
    
    // Thread-safe storage for unique request/response pairs (FIFO queue for memory management)
    private final ConcurrentLinkedQueue<RequestResponseEntry> storedRequests;
    
    // Configuration and state
    private final AtomicBoolean filteringEnabled;
    
    // Statistics tracking
    private final AtomicLong totalRequests;
    private final AtomicLong uniqueRequests;
    private final AtomicLong duplicateRequests;
    
    /**
     * Constructor initializes the deduplication engine with logging support.
     * 
     * @param logging Burp's logging interface for debug output and error reporting
     */
    public RequestDeduplicator(Logging logging) {
        this.logging = logging;
        this.fingerprintGenerator = new FingerprintGenerator(logging);
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
        
        // Debug output - remove for production
        // System.out.println(">>> UniReq: RequestDeduplicator.isUniqueRequest() called - Total: " + totalRequests.get());
        
        // Check if filtering is enabled
        if (!filteringEnabled.get()) {
            // Debug output - remove for production
            // System.out.println(">>> UniReq: Filtering DISABLED - treating as unique");
            return true; // Treat all requests as unique when filtering is disabled
        }
        
        try {
            // Compute fingerprint for the request
            String fingerprint = fingerprintGenerator.computeFingerprint(request);
            
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
            String fingerprint = fingerprintGenerator.computeFingerprint(request);
            
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
     * Enables or disables request filtering.
     * When disabled, all requests are considered unique.
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
     * Clears all stored fingerprints and requests.
     * This effectively resets the deduplication state.
     */
    public void clearFingerprints() {
        int fingerprintCount = seenFingerprints.size();
        int requestCount = storedRequests.size();
        
        seenFingerprints.clear();
        storedRequests.clear();
        
        // Reset statistics
        totalRequests.set(0);
        uniqueRequests.set(0);
        duplicateRequests.set(0);
        
        logging.logToOutput(String.format("Cleared %d fingerprints and %d stored requests", 
                                         fingerprintCount, requestCount));
    }
    
    // ==================== Statistics Methods ====================
    
    /**
     * @return Total number of requests processed
     */
    public long getTotalRequests() {
        return totalRequests.get();
    }
    
    /**
     * @return Number of unique requests identified
     */
    public long getUniqueRequests() {
        return uniqueRequests.get();
    }
    
    /**
     * @return Number of duplicate requests blocked
     */
    public long getDuplicateRequests() {
        return duplicateRequests.get();
    }
    
    /**
     * @return Number of stored fingerprints
     */
    public int getStoredFingerprintCount() {
        return seenFingerprints.size();
    }
    
    /**
     * @return Number of stored request/response entries
     */
    public int getStoredRequestCount() {
        return storedRequests.size();
    }
    
    /**
     * @return Duplication rate as a percentage (0.0 to 100.0)
     */
    public double getDuplicationRate() {
        long total = getTotalRequests();
        if (total == 0) return 0.0;
        return (getDuplicateRequests() * 100.0) / total;
    }
    
    /**
     * @return The fingerprint generator instance
     */
    public FingerprintGenerator getFingerprintGenerator() {
        return fingerprintGenerator;
    }
} 