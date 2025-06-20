package com.burp.unireq.export;

import com.burp.unireq.model.ExportConfiguration;
import com.burp.unireq.model.RequestResponseEntry;
import burp.api.montoya.logging.Logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;

/**
 * CsvExporter - Handles export of HTTP request data to CSV format
 * 
 * This class provides specialized CSV export functionality with proper
 * escaping of special characters (commas, quotes, newlines) and support
 * for both basic metadata and full request/response data export.
 * 
 * CSV Output Structure:
 * - Header row with column names
 * - Data rows with proper quoting and escaping
 * - Configurable columns based on includeMetadata setting
 * - UTF-8 encoding for international character support
 * - RFC 4180 compliant CSV formatting
 * 
 * Security Features:
 * - Prevents CSV injection attacks by proper escaping
 * - Sanitizes formula-like content (=, +, -, @)
 * - Handles special characters safely
 * 
 * @author Harshit Shah
 */
public class CsvExporter {
    
    private final Logging logging;
    
    /**
     * Constructor initializes the CSV exporter with logging support.
     * 
     * @param logging Burp's logging interface for debug output and error reporting
     */
    public CsvExporter(Logging logging) {
        this.logging = logging;
    }
    
    /**
     * Exports request/response data to CSV format according to the configuration.
     * 
     * @param config The export configuration
     * @throws IOException if the export operation fails
     */
    public void export(ExportConfiguration config) throws IOException {
        logging.logToOutput("Starting CSV export with " + config.getEntryCount() + " entries");
        
        StringBuilder csvContent = new StringBuilder();
        
        // Add CSV header
        addCsvHeader(csvContent, config);
        
        // Add data rows
        for (RequestResponseEntry entry : config.getEntries()) {
            addCsvRow(csvContent, entry, config);
        }
        
        // Write to file with UTF-8 encoding
        Files.write(config.getDestinationFile().toPath(), 
                   csvContent.toString().getBytes(StandardCharsets.UTF_8));
        
        logging.logToOutput("CSV export completed: " + config.getDestinationFile().getName());
    }
    
    /**
     * Adds the CSV header row based on configuration.
     * 
     * @param csv The StringBuilder to append to
     * @param config The export configuration
     */
    private void addCsvHeader(StringBuilder csv, ExportConfiguration config) {
        if (config.isIncludeMetadata()) {
            csv.append("Method,Host,Path,Status,Timestamp,Fingerprint");
            if (config.isIncludeFullData()) {
                csv.append(",Request Headers,Request Body,Response Headers,Response Body");
            }
        } else {
            csv.append("Method,Host,Path,Status");
        }
        csv.append("\n");
    }
    
    /**
     * Adds a single data row to the CSV output.
     * 
     * @param csv The StringBuilder to append to
     * @param entry The request/response entry to add
     * @param config The export configuration
     */
    private void addCsvRow(StringBuilder csv, RequestResponseEntry entry, ExportConfiguration config) {
        // Basic columns
        csv.append(escapeCsv(entry.getMethod())).append(",");
        csv.append(escapeCsv(entry.getRequest().httpService().host())).append(",");
        csv.append(escapeCsv(entry.getPath())).append(",");
        csv.append(escapeCsv(entry.getStatusCode())).append(",");
        
        // Optional metadata columns
        if (config.isIncludeMetadata()) {
            csv.append(escapeCsv(entry.getTimestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))).append(",");
            csv.append(escapeCsv(entry.getFingerprint()));
            
            // Optional full data columns
            if (config.isIncludeFullData()) {
                csv.append(",");
                // Request headers
                csv.append(escapeCsv(entry.getRequest().headers().toString()));
                csv.append(",");
                // Request body
                if (entry.getRequest().body() != null && entry.getRequest().body().length() > 0) {
                    csv.append(escapeCsv(entry.getRequest().bodyToString()));
                }
                csv.append(",");
                // Response headers
                if (entry.getResponse() != null) {
                    csv.append(escapeCsv(entry.getResponse().headers().toString()));
                    csv.append(",");
                    // Response body
                    if (entry.getResponse().body() != null && entry.getResponse().body().length() > 0) {
                        csv.append(escapeCsv(entry.getResponse().bodyToString()));
                    }
                }
            }
        }
        
        csv.append("\n");
    }
    
    /**
     * Escapes special characters in CSV fields according to RFC 4180.
     * 
     * This method handles:
     * - Double quotes (escaped as double double-quotes)
     * - Commas (field wrapped in quotes)
     * - Newlines and carriage returns (field wrapped in quotes)
     * - CSV injection prevention (sanitizes formula starters)
     * 
     * @param input The input string to escape
     * @return The escaped string safe for CSV
     */
    private String escapeCsv(String input) {
        if (input == null) {
            return "\"\"";
        }
        
        // Prevent CSV injection by sanitizing formula starters
        String sanitized = input;
        if (sanitized.startsWith("=") || sanitized.startsWith("+") || 
            sanitized.startsWith("-") || sanitized.startsWith("@")) {
            sanitized = "'" + sanitized; // Prefix with single quote to neutralize
        }
        
        // Check if field needs quoting
        boolean needsQuoting = sanitized.contains("\"") || 
                              sanitized.contains(",") || 
                              sanitized.contains("\n") || 
                              sanitized.contains("\r");
        
        if (needsQuoting) {
            // Escape double quotes by doubling them
            sanitized = sanitized.replace("\"", "\"\"");
            // Wrap in double quotes
            return "\"" + sanitized + "\"";
        }
        
        return sanitized;
    }
    
    /**
     * Validates that the CSV exporter is ready to perform exports.
     * 
     * @return true if the exporter is ready, false otherwise
     */
    public boolean isReady() {
        return true; // CSV exporter has no external dependencies
    }
    
    /**
     * Estimates the size of a CSV export for the given configuration.
     * 
     * @param config The export configuration
     * @return Estimated size in bytes
     */
    public long estimateSize(ExportConfiguration config) {
        if (config == null || config.getEntries() == null) {
            return 0;
        }
        
        int headerSize = config.isIncludeMetadata() ? 100 : 50;
        int basePerEntrySize = config.isIncludeMetadata() ? 300 : 150;
        int fullDataPerEntrySize = config.isIncludeFullData() ? 2000 : 0;
        
        return headerSize + (config.getEntryCount() * (basePerEntrySize + fullDataPerEntrySize));
    }
} 