package com.burp.unireq.utils;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import java.util.List;
import java.util.ArrayList;

/**
 * HttpUtils - Utility functions for HTTP request/response processing
 * 
 * This class provides common utility functions for working with HTTP
 * requests and responses in the UniReq extension. It includes methods
 * for parsing, validation, and content analysis.
 * 
 * @author Harshit Shah
 */
public class HttpUtils {
    
    // Common HTTP methods
    public static final String[] COMMON_METHODS = {
        "GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH", "TRACE"
    };
    
    // Common status code ranges
    public static final String[] STATUS_RANGES = {
        "All", "2xx", "3xx", "4xx", "5xx"
    };
    
    /**
     * Checks if an HTTP method is considered safe (read-only).
     * 
     * @param method The HTTP method to check
     * @return true if the method is safe (GET, HEAD, OPTIONS)
     */
    public static boolean isSafeMethod(String method) {
        if (method == null) return false;
        String upperMethod = method.toUpperCase();
        return "GET".equals(upperMethod) || "HEAD".equals(upperMethod) || "OPTIONS".equals(upperMethod);
    }
    
    /**
     * Checks if an HTTP method typically has a request body.
     * 
     * @param method The HTTP method to check
     * @return true if the method typically has a body (POST, PUT, PATCH)
     */
    public static boolean methodHasBody(String method) {
        if (method == null) return false;
        String upperMethod = method.toUpperCase();
        return "POST".equals(upperMethod) || "PUT".equals(upperMethod) || "PATCH".equals(upperMethod);
    }
    
    /**
     * Extracts the content type from an HTTP request.
     * 
     * @param request The HTTP request
     * @return The content type, or null if not found
     */
    public static String getContentType(HttpRequest request) {
        if (request == null || request.headers() == null) return null;
        
        return request.headers().stream()
                .filter(header -> "Content-Type".equalsIgnoreCase(header.name()))
                .map(header -> header.value())
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Extracts the content type from an HTTP response.
     * 
     * @param response The HTTP response
     * @return The content type, or null if not found
     */
    public static String getContentType(HttpResponse response) {
        if (response == null || response.headers() == null) return null;
        
        return response.headers().stream()
                .filter(header -> "Content-Type".equalsIgnoreCase(header.name()))
                .map(header -> header.value())
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Checks if an HTTP status code represents success (2xx range).
     * 
     * @param statusCode The status code to check
     * @return true if the status code is in the 2xx range
     */
    public static boolean isSuccessStatus(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }
    
    /**
     * Checks if an HTTP status code represents a client error (4xx range).
     * 
     * @param statusCode The status code to check
     * @return true if the status code is in the 4xx range
     */
    public static boolean isClientError(int statusCode) {
        return statusCode >= 400 && statusCode < 500;
    }
    
    /**
     * Checks if an HTTP status code represents a server error (5xx range).
     * 
     * @param statusCode The status code to check
     * @return true if the status code is in the 5xx range
     */
    public static boolean isServerError(int statusCode) {
        return statusCode >= 500 && statusCode < 600;
    }
    
    /**
     * Gets a human-readable description of an HTTP status code.
     * 
     * @param statusCode The status code
     * @return A description of the status code
     */
    public static String getStatusDescription(int statusCode) {
        switch (statusCode) {
            case 200: return "OK";
            case 201: return "Created";
            case 204: return "No Content";
            case 301: return "Moved Permanently";
            case 302: return "Found";
            case 304: return "Not Modified";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 500: return "Internal Server Error";
            case 502: return "Bad Gateway";
            case 503: return "Service Unavailable";
            default:
                if (isSuccessStatus(statusCode)) return "Success";
                if (statusCode >= 300 && statusCode < 400) return "Redirection";
                if (isClientError(statusCode)) return "Client Error";
                if (isServerError(statusCode)) return "Server Error";
                return "Unknown";
        }
    }
    
    /**
     * Extracts query parameters from a URL path.
     * 
     * @param path The URL path with query string
     * @return List of parameter name=value pairs
     */
    public static List<String> extractQueryParameters(String path) {
        List<String> parameters = new ArrayList<>();
        
        if (path == null || !path.contains("?")) {
            return parameters;
        }
        
        String queryString = path.substring(path.indexOf("?") + 1);
        String[] pairs = queryString.split("&");
        
        for (String pair : pairs) {
            if (!pair.trim().isEmpty()) {
                parameters.add(pair);
            }
        }
        
        return parameters;
    }
    
    /**
     * Removes query parameters from a URL path.
     * 
     * @param path The URL path with query string
     * @return The path without query parameters
     */
    public static String removeQueryParameters(String path) {
        if (path == null || !path.contains("?")) {
            return path;
        }
        
        return path.substring(0, path.indexOf("?"));
    }
    
    /**
     * Checks if a request appears to be an AJAX request.
     * 
     * @param request The HTTP request to check
     * @return true if the request appears to be AJAX
     */
    public static boolean isAjaxRequest(HttpRequest request) {
        if (request == null || request.headers() == null) return false;
        
        return request.headers().stream()
                .anyMatch(header -> 
                    ("X-Requested-With".equalsIgnoreCase(header.name()) && "XMLHttpRequest".equals(header.value())) ||
                    ("Accept".equalsIgnoreCase(header.name()) && header.value().contains("application/json"))
                );
    }
    
    /**
     * Checks if a response contains JSON content.
     * 
     * @param response The HTTP response to check
     * @return true if the response appears to contain JSON
     */
    public static boolean isJsonResponse(HttpResponse response) {
        String contentType = getContentType(response);
        return contentType != null && contentType.toLowerCase().contains("application/json");
    }
    
    /**
     * Checks if a response contains HTML content.
     * 
     * @param response The HTTP response to check
     * @return true if the response appears to contain HTML
     */
    public static boolean isHtmlResponse(HttpResponse response) {
        String contentType = getContentType(response);
        return contentType != null && contentType.toLowerCase().contains("text/html");
    }
    
    /**
     * Sanitizes a URL for safe display by removing sensitive information.
     * 
     * @param url The URL to sanitize
     * @return A sanitized version of the URL
     */
    public static String sanitizeUrl(String url) {
        if (url == null) return "";
        
        // Remove common sensitive parameters
        String sanitized = url.replaceAll("(?i)([?&])(api[_-]?key|token|password|secret|auth)=[^&]*", "$1$2=[REDACTED]");
        
        return sanitized;
    }
    
    /**
     * Gets the file extension from a URL path.
     * 
     * @param path The URL path
     * @return The file extension (without dot), or empty string if none
     */
    public static String getFileExtension(String path) {
        if (path == null || path.isEmpty()) return "";
        
        // Remove query parameters first
        String cleanPath = removeQueryParameters(path);
        
        // Find the last dot after the last slash
        int lastSlash = cleanPath.lastIndexOf('/');
        int lastDot = cleanPath.lastIndexOf('.');
        
        if (lastDot > lastSlash && lastDot < cleanPath.length() - 1) {
            return cleanPath.substring(lastDot + 1).toLowerCase();
        }
        
        return "";
    }
    
    /**
     * Checks if a path represents a static resource (CSS, JS, images, etc.).
     * 
     * @param path The URL path to check
     * @return true if the path appears to be a static resource
     */
    public static boolean isStaticResource(String path) {
        String extension = getFileExtension(path);
        
        String[] staticExtensions = {
            "css", "js", "png", "jpg", "jpeg", "gif", "ico", "svg", "woff", "woff2", "ttf", "eot"
        };
        
        for (String staticExt : staticExtensions) {
            if (staticExt.equals(extension)) {
                return true;
            }
        }
        
        return false;
    }
} 