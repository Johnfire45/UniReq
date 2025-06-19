package com.burp.unireq.core;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.logging.Logging;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * FingerprintGenerator - Generates unique fingerprints for HTTP requests
 * 
 * This class is responsible for computing unique fingerprints for HTTP requests
 * based on method, normalized path, and content hash. The fingerprinting algorithm
 * ensures that functionally identical requests produce the same fingerprint.
 * 
 * Fingerprint Format: METHOD | NORMALIZED_PATH | HASH(CONTENT)
 * - METHOD: HTTP method (GET, POST, etc.)
 * - NORMALIZED_PATH: URL path with trailing slashes removed and normalized
 * - HASH: SHA-256 hash of request body (for POST/PUT) or query string (for GET)
 * 
 * @author Harshit Shah
 */
public class FingerprintGenerator {
    
    // Constants for fingerprint computation
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String FINGERPRINT_SEPARATOR = " | ";
    private static final String EMPTY_HASH = "EMPTY";
    
    private final Logging logging;
    
    /**
     * Constructor initializes the fingerprint generator with logging support.
     * 
     * @param logging Burp's logging interface for debug output and error reporting
     */
    public FingerprintGenerator(Logging logging) {
        this.logging = logging;
    }
    
    /**
     * Computes a unique fingerprint for an HTTP request.
     * 
     * The fingerprint format is: METHOD | NORMALIZED_PATH | HASH(CONTENT)
     * where:
     * - METHOD is the HTTP method (GET, POST, etc.)
     * - NORMALIZED_PATH is the URL path with trailing slashes removed and lowercased
     * - HASH is SHA-256 of request body (POST/PUT) or query string (GET)
     * 
     * @param request The HTTP request to fingerprint
     * @return A unique fingerprint string for the request
     * @throws RuntimeException if fingerprint computation fails
     */
    public String computeFingerprint(HttpRequest request) {
        try {
            String method = request.method();
            String normalizedPath = normalizePath(request.path());
            String contentHash = computeContentHash(request);
            
            String fingerprint = method + FINGERPRINT_SEPARATOR + normalizedPath + FINGERPRINT_SEPARATOR + contentHash;
            
            logging.logToOutput("Generated fingerprint: " + fingerprint);
            return fingerprint;
            
        } catch (Exception e) {
            // Create a fallback fingerprint to ensure the system continues working
            String fallbackFingerprint = "FALLBACK_" + System.currentTimeMillis();
            logging.logToError("Failed to compute fingerprint, using fallback: " + fallbackFingerprint + " - " + e.getMessage());
            return fallbackFingerprint;
        }
    }
    
    /**
     * Normalizes a URL path for consistent fingerprinting.
     * 
     * Normalization includes:
     * - Converting to lowercase for case-insensitive matching
     * - Removing trailing slashes (except for root path)
     * - Handling null/empty paths
     * 
     * @param path The original URL path
     * @return Normalized path for fingerprinting
     */
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        
        // Convert to lowercase for case-insensitive matching
        String normalized = path.toLowerCase();
        
        // Remove trailing slash unless it's the root path
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        
        // Ensure path starts with /
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        
        return normalized;
    }
    
    /**
     * Computes a hash of the request content for fingerprinting.
     * 
     * Content selection logic:
     * - For requests with bodies (POST, PUT, PATCH): Hash the request body
     * - For GET requests: Hash the query string parameters
     * - For requests with no relevant content: Return "EMPTY"
     * 
     * @param request The HTTP request to hash
     * @return SHA-256 hash of the relevant content, or "EMPTY" if no content
     */
    private String computeContentHash(HttpRequest request) {
        try {
            String contentToHash = null;
            
            // Determine what content to hash based on request method and body presence
            if (hasRequestBody(request)) {
                // For requests with bodies (POST, PUT, PATCH, etc.), hash the body
                contentToHash = request.bodyToString();
                logging.logToOutput("Hashing request body for " + request.method() + " request");
            } else if ("GET".equalsIgnoreCase(request.method()) && request.query() != null && !request.query().isEmpty()) {
                // For GET requests, hash the query string parameters
                contentToHash = request.query();
                logging.logToOutput("Hashing query string for GET request");
            }
            
            // If no relevant content, return empty marker
            if (contentToHash == null || contentToHash.trim().isEmpty()) {
                return EMPTY_HASH;
            }
            
            // Compute SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(contentToHash.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
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
            // This should never happen with SHA-256, but handle gracefully
            logging.logToError("SHA-256 algorithm not available: " + e.getMessage());
            return "HASH_ERROR_" + System.currentTimeMillis();
        } catch (Exception e) {
            logging.logToError("Error computing content hash: " + e.getMessage());
            return "HASH_ERROR_" + System.currentTimeMillis();
        }
    }
    
    /**
     * Determines if an HTTP request has a request body that should be considered for fingerprinting.
     * 
     * @param request The HTTP request to check
     * @return true if the request has a body, false otherwise
     */
    private boolean hasRequestBody(HttpRequest request) {
        // Check if request has a body (content length > 0 or body is not empty)
        return request.body() != null && request.body().length() > 0;
    }
    
    /**
     * Validates that the fingerprint generator is working correctly.
     * This method can be used for testing and diagnostics.
     * 
     * @return true if the generator is functioning properly
     */
    public boolean isWorking() {
        try {
            MessageDigest.getInstance(HASH_ALGORITHM);
            return true;
        } catch (NoSuchAlgorithmException e) {
            logging.logToError("Fingerprint generator validation failed: " + e.getMessage());
            return false;
        }
    }
} 