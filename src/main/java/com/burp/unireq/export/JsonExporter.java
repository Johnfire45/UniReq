package com.burp.unireq.export;

import com.burp.unireq.model.ExportConfiguration;
import com.burp.unireq.model.RequestResponseEntry;
import burp.api.montoya.logging.Logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;

/**
 * JsonExporter - Handles export of HTTP request data to JSON format
 * 
 * This class provides specialized JSON export functionality with support for
 * both basic metadata and full request/response data export. It generates
 * well-formatted JSON that can be easily parsed by other tools.
 * 
 * @author Harshit Shah
 */
public class JsonExporter {
    
    private final Logging logging;
    
    /**
     * Constructor initializes the JSON exporter with logging support.
     * 
     * @param logging Burp's logging interface for debug output and error reporting
     */
    public JsonExporter(Logging logging) {
        this.logging = logging;
    }
    
    /**
     * Exports request/response data to JSON format according to the configuration.
     * 
     * @param config The export configuration
     * @throws IOException if the export operation fails
     */
    public void export(ExportConfiguration config) throws IOException {
        logging.logToOutput("Starting JSON export with " + config.getEntryCount() + " entries");
        
        StringBuilder jsonContent = new StringBuilder();
        
        // Start JSON object
        jsonContent.append("{\n");
        
        // Add metadata
        addMetadata(jsonContent, config);
        
        // Add entries array
        jsonContent.append("  \"entries\": [\n");
        
        boolean first = true;
        for (RequestResponseEntry entry : config.getEntries()) {
            if (!first) {
                jsonContent.append(",\n");
            }
            addEntry(jsonContent, entry, config);
            first = false;
        }
        
        jsonContent.append("\n  ]\n");
        jsonContent.append("}");
        
        // Write to file
        Files.write(config.getDestinationFile().toPath(), 
                   jsonContent.toString().getBytes(StandardCharsets.UTF_8));
        
        logging.logToOutput("JSON export completed: " + config.getDestinationFile().getName());
    }
    
    /**
     * Adds metadata section to the JSON output.
     * 
     * @param json The StringBuilder to append to
     * @param config The export configuration
     */
    private void addMetadata(StringBuilder json, ExportConfiguration config) {
        if (!config.isIncludeMetadata()) {
            return;
        }
        
        json.append("  \"metadata\": {\n");
        json.append("    \"title\": \"").append(escapeJson(config.getExportTitle())).append("\",\n");
        json.append("    \"description\": \"").append(escapeJson(config.getExportDescription())).append("\",\n");
        json.append("    \"exportTime\": \"").append(java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",\n");
        json.append("    \"format\": \"").append(config.getFormat()).append("\",\n");
        json.append("    \"entryCount\": ").append(config.getEntryCount()).append(",\n");
        json.append("    \"includeFullData\": ").append(config.isIncludeFullData()).append("\n");
        json.append("  },\n");
    }
    
    /**
     * Adds a single entry to the JSON output.
     * 
     * @param json The StringBuilder to append to
     * @param entry The request/response entry to add
     * @param config The export configuration
     */
    private void addEntry(StringBuilder json, RequestResponseEntry entry, ExportConfiguration config) {
        json.append("    {\n");
        
        // Basic entry information
        json.append("      \"timestamp\": \"").append(entry.getTimestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)).append("\",\n");
        json.append("      \"fingerprint\": \"").append(escapeJson(entry.getFingerprint())).append("\",\n");
        
        // Request information
        json.append("      \"request\": {\n");
        json.append("        \"method\": \"").append(escapeJson(entry.getMethod())).append("\",\n");
        json.append("        \"host\": \"").append(escapeJson(entry.getRequest().httpService().host())).append("\",\n");
        json.append("        \"port\": ").append(entry.getRequest().httpService().port()).append(",\n");
        json.append("        \"secure\": ").append(entry.getRequest().httpService().secure()).append(",\n");
        json.append("        \"path\": \"").append(escapeJson(entry.getPath())).append("\"");
        
        if (config.isIncludeFullData()) {
            json.append(",\n        \"headers\": \"").append(escapeJson(entry.getRequest().headers().toString())).append("\"");
            if (entry.getRequest().body() != null && entry.getRequest().body().length() > 0) {
                json.append(",\n        \"body\": \"").append(escapeJson(entry.getRequest().bodyToString())).append("\"");
            }
        }
        
        json.append("\n      }");
        
        // Response information (if available)
        if (entry.getResponse() != null) {
            json.append(",\n      \"response\": {\n");
            json.append("        \"statusCode\": ").append(entry.getResponse().statusCode()).append(",\n");
            json.append("        \"reasonPhrase\": \"").append(escapeJson(entry.getResponse().reasonPhrase())).append("\"");
            
            if (config.isIncludeFullData()) {
                json.append(",\n        \"headers\": \"").append(escapeJson(entry.getResponse().headers().toString())).append("\"");
                if (entry.getResponse().body() != null && entry.getResponse().body().length() > 0) {
                    json.append(",\n        \"body\": \"").append(escapeJson(entry.getResponse().bodyToString())).append("\"");
                }
            }
            
            json.append("\n      }");
        }
        
        json.append("\n    }");
    }
    
    /**
     * Escapes special characters in JSON strings according to RFC 8259.
     * 
     * This method handles:
     * - Backslash and quote escaping
     * - Common control characters (\n, \r, \t, \b, \f)
     * - All other control characters (0x00-0x1F) as unicode escapes
     * - Ensures valid JSON output for any input string
     * 
     * @param input The input string to escape
     * @return The escaped string safe for JSON
     */
    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            
            switch (c) {
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                default:
                    // Handle other control characters (0x00-0x1F) as unicode escapes
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                    break;
            }
        }
        
        return escaped.toString();
    }
    
    /**
     * Validates that the JSON exporter is ready to perform exports.
     * 
     * @return true if the exporter is ready, false otherwise
     */
    public boolean isReady() {
        return true; // JSON exporter has no external dependencies
    }
    
    /**
     * Estimates the size of a JSON export for the given configuration.
     * 
     * @param config The export configuration
     * @return Estimated size in bytes
     */
    public long estimateSize(ExportConfiguration config) {
        if (config == null || config.getEntries() == null) {
            return 0;
        }
        
        int baseSize = 200; // JSON structure overhead
        int metadataSize = config.isIncludeMetadata() ? 300 : 0;
        int perEntrySize = config.isIncludeFullData() ? 2000 : 300;
        
        return baseSize + metadataSize + (config.getEntryCount() * perEntrySize);
    }
} 