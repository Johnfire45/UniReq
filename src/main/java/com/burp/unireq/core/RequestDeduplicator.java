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
 * Core engine for HTTP request deduplication.
 *
 * Uses SHA-256 fingerprints to identify duplicate requests. All shared state
 * is thread-safe via concurrent collections and atomic operations.
 *
 * @author Harshit Shah
 */
public class RequestDeduplicator {

    private static final int MAX_STORED_REQUESTS = 1000;

    private final FingerprintGenerator fingerprintGenerator;
    private final Logging logging;

    private final ConcurrentSkipListSet<String> seenFingerprints;
    private final ConcurrentLinkedQueue<RequestResponseEntry> storedRequests;
    private final AtomicBoolean filteringEnabled;

    private final AtomicLong totalRequests;
    private final AtomicLong uniqueRequests;
    private final AtomicLong duplicateRequests;
    private final AtomicLong sequenceCounter;

    public RequestDeduplicator(Logging logging) {
        this.logging = logging;
        this.fingerprintGenerator = new FingerprintGenerator(logging);
        this.seenFingerprints = new ConcurrentSkipListSet<>();
        this.storedRequests = new ConcurrentLinkedQueue<>();
        this.filteringEnabled = new AtomicBoolean(true);
        this.totalRequests = new AtomicLong(0);
        this.uniqueRequests = new AtomicLong(0);
        this.duplicateRequests = new AtomicLong(0);
        this.sequenceCounter = new AtomicLong(0);

        logging.logToOutput("RequestDeduplicator initialized with filtering enabled");
    }

    /**
     * Processes a request and returns true if it is unique, false if it is a duplicate.
     */
    public boolean isUniqueRequest(HttpRequest request) {
        totalRequests.incrementAndGet();

        try {
            String fingerprint = fingerprintGenerator.computeFingerprint(request);

            if (!filteringEnabled.get()) {
                uniqueRequests.incrementAndGet();
                storeUniqueRequest(request, fingerprint);
                return true;
            }

            boolean isUnique = seenFingerprints.add(fingerprint);

            if (isUnique) {
                uniqueRequests.incrementAndGet();
                storeUniqueRequest(request, fingerprint);
            } else {
                duplicateRequests.incrementAndGet();
            }

            return isUnique;

        } catch (Exception e) {
            logging.logToError("Error processing request fingerprint: " + e.getMessage());
            uniqueRequests.incrementAndGet();
            return true;
        }
    }

    private void storeUniqueRequest(HttpRequest request, String fingerprint) {
        try {
            long sequenceNumber = sequenceCounter.incrementAndGet();
            RequestResponseEntry entry = new RequestResponseEntry(request, fingerprint, sequenceNumber);
            storedRequests.offer(entry);

            while (storedRequests.size() > MAX_STORED_REQUESTS) {
                storedRequests.poll();
            }
        } catch (Exception e) {
            logging.logToError("Error storing unique request: " + e.getMessage());
        }
    }

    /**
     * Updates the response for a previously stored request entry.
     */
    public void updateResponse(HttpRequest request, HttpResponse response) {
        try {
            String fingerprint = fingerprintGenerator.computeFingerprint(request);

            for (RequestResponseEntry entry : storedRequests) {
                if (fingerprint.equals(entry.getFingerprint())) {
                    entry.setResponse(response);
                    break;
                }
            }
        } catch (Exception e) {
            logging.logToError("Error updating response: " + e.getMessage());
        }
    }

    public List<RequestResponseEntry> getStoredRequests() {
        return new ArrayList<>(storedRequests);
    }

    public void setFilteringEnabled(boolean enabled) {
        filteringEnabled.set(enabled);
        logging.logToOutput("Request filtering " + (enabled ? "enabled" : "disabled"));
    }

    public boolean isFilteringEnabled() {
        return filteringEnabled.get();
    }

    public void clearFingerprints() {
        int fingerprintCount = seenFingerprints.size();
        int requestCount = storedRequests.size();

        seenFingerprints.clear();
        storedRequests.clear();
        totalRequests.set(0);
        uniqueRequests.set(0);
        duplicateRequests.set(0);
        sequenceCounter.set(0);

        logging.logToOutput(String.format("Cleared %d fingerprints and %d stored requests",
                fingerprintCount, requestCount));
    }

    public long getTotalRequests() {
        return totalRequests.get();
    }

    public long getUniqueRequests() {
        return uniqueRequests.get();
    }

    public long getDuplicateRequests() {
        return duplicateRequests.get();
    }
}
