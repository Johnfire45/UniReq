package com.burp.unireq.core;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.logging.Logging;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Generates unique SHA-256 fingerprints for HTTP requests.
 *
 * Fingerprint format: METHOD | HOST | NORMALIZED_PATH | HASH(CONTENT)
 * - NORMALIZED_PATH: lowercase, trailing slashes removed
 * - HASH: SHA-256 of request body (POST/PUT/PATCH) or query string (GET)
 *
 * @author Harshit Shah
 */
public class FingerprintGenerator {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String FINGERPRINT_SEPARATOR = " | ";
    private static final String EMPTY_HASH = "EMPTY";
    private static final int MAX_BODY_BYTES = 1_048_576; // 1 MB

    private static final ThreadLocal<MessageDigest> DIGEST_TL = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance(HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    });

    private final Logging logging;

    public FingerprintGenerator(Logging logging) {
        this.logging = logging;
    }

    public String computeFingerprint(HttpRequest request) {
        try {
            String method = request.method();
            String host = request.httpService().host();
            String normalizedPath = normalizePath(request.path());
            String contentHash = computeContentHash(request);

            return method + FINGERPRINT_SEPARATOR + host + FINGERPRINT_SEPARATOR
                    + normalizedPath + FINGERPRINT_SEPARATOR + contentHash;

        } catch (Exception e) {
            String fallback = "FALLBACK_" + System.currentTimeMillis();
            logging.logToError("Failed to compute fingerprint, using fallback: " + fallback + " - " + e.getMessage());
            return fallback;
        }
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }

        String normalized = path.toLowerCase();

        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        return normalized;
    }

    private String computeContentHash(HttpRequest request) {
        try {
            String contentToHash = null;

            if (hasRequestBody(request)) {
                if (request.body().length() > MAX_BODY_BYTES) {
                    return EMPTY_HASH;
                }
                String contentType = request.headerValue("Content-Type");
                if (contentType != null && isBinaryContentType(contentType)) {
                    return EMPTY_HASH;
                }
                contentToHash = request.bodyToString();
            } else if ("GET".equalsIgnoreCase(request.method()) && request.query() != null && !request.query().isEmpty()) {
                contentToHash = request.query();
            }

            if (contentToHash == null || contentToHash.trim().isEmpty()) {
                return EMPTY_HASH;
            }

            MessageDigest digest = DIGEST_TL.get();
            digest.reset();
            byte[] hashBytes = digest.digest(contentToHash.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (Exception e) {
            logging.logToError("Error computing content hash: " + e.getMessage());
            return "HASH_ERROR_" + System.currentTimeMillis();
        }
    }

    private boolean hasRequestBody(HttpRequest request) {
        return request.body() != null && request.body().length() > 0;
    }

    private boolean isBinaryContentType(String contentType) {
        String lower = contentType.toLowerCase();
        return lower.startsWith("image/") ||
               lower.startsWith("video/") ||
               lower.startsWith("audio/") ||
               lower.contains("application/octet-stream") ||
               lower.startsWith("multipart/form-data");
    }
}
