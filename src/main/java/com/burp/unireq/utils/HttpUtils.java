package com.burp.unireq.utils;

/**
 * HttpUtils - Utility functions for HTTP request/response processing
 *
 * @author Harshit Shah
 */
public class HttpUtils {

    /**
     * Removes query parameters from a URL path.
     *
     * @param path The URL path with query string
     * @return The path without query parameters
     */
    private static String removeQueryParameters(String path) {
        if (path == null || !path.contains("?")) {
            return path;
        }
        return path.substring(0, path.indexOf("?"));
    }

    /**
     * Gets the file extension from a URL path.
     *
     * @param path The URL path
     * @return The file extension (without dot), or empty string if none
     */
    public static String getFileExtension(String path) {
        if (path == null || path.isEmpty()) return "";

        String cleanPath = removeQueryParameters(path);

        int fragmentIndex = cleanPath.indexOf('#');
        if (fragmentIndex != -1) {
            cleanPath = cleanPath.substring(0, fragmentIndex);
        }

        int lastSlash = cleanPath.lastIndexOf('/');
        int lastDot = cleanPath.lastIndexOf('.');

        if (lastDot > lastSlash && lastDot < cleanPath.length() - 1) {
            return cleanPath.substring(lastDot + 1).toLowerCase();
        }

        return "";
    }
}
