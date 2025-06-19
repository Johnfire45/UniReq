package com.burp.unireq.export;

import com.burp.unireq.model.ExportConfiguration;
import com.burp.unireq.model.RequestResponseEntry;
import burp.api.montoya.logging.Logging;

import java.io.IOException;
import java.util.List;

/**
 * ExportManager - Coordinates the export of HTTP request data
 * 
 * This class manages the export process by coordinating different export formats
 * and delegating the actual export work to specialized exporters. It provides
 * a unified interface for exporting request/response data in various formats.
 * 
 * @author Harshit Shah
 */
public class ExportManager {
    
    private final Logging logging;
    private final JsonExporter jsonExporter;
    
    /**
     * Constructor initializes the export manager with logging support.
     * 
     * @param logging Burp's logging interface for debug output and error reporting
     */
    public ExportManager(Logging logging) {
        this.logging = logging;
        this.jsonExporter = new JsonExporter(logging);
    }
    
    /**
     * Exports request/response data according to the provided configuration.
     * 
     * @param config The export configuration containing format, destination, and options
     * @throws IOException if the export operation fails
     * @throws IllegalArgumentException if the configuration is invalid
     */
    public void exportData(ExportConfiguration config) throws IOException {
        if (config == null) {
            throw new IllegalArgumentException("Export configuration cannot be null");
        }
        
        // Validate configuration
        config.validate();
        
        logging.logToOutput(String.format("Starting export: %d entries to %s format", 
                                         config.getEntryCount(), config.getFormat()));
        
        try {
            switch (config.getFormat()) {
                case JSON:
                    jsonExporter.export(config);
                    break;
                case CSV:
                    exportToCsv(config);
                    break;
                case HTML:
                    exportToHtml(config);
                    break;
                case MARKDOWN:
                    exportToMarkdown(config);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported export format: " + config.getFormat());
            }
            
            logging.logToOutput("Export completed successfully: " + config.getDestinationFile().getName());
            
        } catch (Exception e) {
            logging.logToError("Export failed: " + e.getMessage());
            throw new IOException("Export failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Exports data to CSV format.
     * This is a placeholder implementation that will be expanded in the future.
     * 
     * @param config The export configuration
     * @throws IOException if the export fails
     */
    private void exportToCsv(ExportConfiguration config) throws IOException {
        // TODO: Implement CSV export
        // For now, create a simple CSV with basic fields
        
        StringBuilder csvContent = new StringBuilder();
        
        // CSV header
        if (config.isIncludeMetadata()) {
            csvContent.append("Timestamp,Method,Host,Path,Status,Fingerprint\n");
        } else {
            csvContent.append("Method,Host,Path,Status\n");
        }
        
        // CSV data rows
        for (RequestResponseEntry entry : config.getEntries()) {
            if (config.isIncludeMetadata()) {
                csvContent.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                    entry.getFormattedTimestamp(),
                    entry.getMethod(),
                    entry.getRequest().httpService().host(),
                    entry.getPath(),
                    entry.getStatusCode(),
                    entry.getFingerprint()));
            } else {
                csvContent.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\"\n",
                    entry.getMethod(),
                    entry.getRequest().httpService().host(),
                    entry.getPath(),
                    entry.getStatusCode()));
            }
        }
        
        // Write to file
        java.nio.file.Files.write(config.getDestinationFile().toPath(), 
                                 csvContent.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        
        logging.logToOutput("CSV export completed");
    }
    
    /**
     * Exports data to HTML format.
     * This is a placeholder implementation that will be expanded in the future.
     * 
     * @param config The export configuration
     * @throws IOException if the export fails
     */
    private void exportToHtml(ExportConfiguration config) throws IOException {
        // TODO: Implement HTML export
        // For now, create a simple HTML table
        
        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<!DOCTYPE html>\n<html>\n<head>\n");
        htmlContent.append("<title>").append(config.getExportTitle()).append("</title>\n");
        htmlContent.append("<style>\n");
        htmlContent.append("table { border-collapse: collapse; width: 100%; }\n");
        htmlContent.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        htmlContent.append("th { background-color: #f2f2f2; }\n");
        htmlContent.append("</style>\n");
        htmlContent.append("</head>\n<body>\n");
        
        htmlContent.append("<h1>").append(config.getExportTitle()).append("</h1>\n");
        if (!config.getExportDescription().isEmpty()) {
            htmlContent.append("<p>").append(config.getExportDescription()).append("</p>\n");
        }
        
        htmlContent.append("<table>\n<tr>");
        htmlContent.append("<th>Method</th><th>Host</th><th>Path</th><th>Status</th>");
        if (config.isIncludeMetadata()) {
            htmlContent.append("<th>Timestamp</th><th>Fingerprint</th>");
        }
        htmlContent.append("</tr>\n");
        
        for (RequestResponseEntry entry : config.getEntries()) {
            htmlContent.append("<tr>");
            htmlContent.append("<td>").append(entry.getMethod()).append("</td>");
            htmlContent.append("<td>").append(entry.getRequest().httpService().host()).append("</td>");
            htmlContent.append("<td>").append(entry.getPath()).append("</td>");
            htmlContent.append("<td>").append(entry.getStatusCode()).append("</td>");
            if (config.isIncludeMetadata()) {
                htmlContent.append("<td>").append(entry.getFormattedTimestamp()).append("</td>");
                htmlContent.append("<td>").append(entry.getFingerprint()).append("</td>");
            }
            htmlContent.append("</tr>\n");
        }
        
        htmlContent.append("</table>\n</body>\n</html>");
        
        // Write to file
        java.nio.file.Files.write(config.getDestinationFile().toPath(), 
                                 htmlContent.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        
        logging.logToOutput("HTML export completed");
    }
    
    /**
     * Exports data to Markdown format.
     * This is a placeholder implementation that will be expanded in the future.
     * 
     * @param config The export configuration
     * @throws IOException if the export fails
     */
    private void exportToMarkdown(ExportConfiguration config) throws IOException {
        // TODO: Implement Markdown export
        // For now, create a simple Markdown table
        
        StringBuilder mdContent = new StringBuilder();
        mdContent.append("# ").append(config.getExportTitle()).append("\n\n");
        
        if (!config.getExportDescription().isEmpty()) {
            mdContent.append(config.getExportDescription()).append("\n\n");
        }
        
        // Markdown table header
        mdContent.append("| Method | Host | Path | Status |");
        if (config.isIncludeMetadata()) {
            mdContent.append(" Timestamp | Fingerprint |");
        }
        mdContent.append("\n");
        
        mdContent.append("|--------|------|------|--------|");
        if (config.isIncludeMetadata()) {
            mdContent.append("-----------|-------------|");
        }
        mdContent.append("\n");
        
        // Markdown table rows
        for (RequestResponseEntry entry : config.getEntries()) {
            mdContent.append("| ").append(entry.getMethod())
                    .append(" | ").append(entry.getRequest().httpService().host())
                    .append(" | ").append(entry.getPath())
                    .append(" | ").append(entry.getStatusCode())
                    .append(" |");
            if (config.isIncludeMetadata()) {
                mdContent.append(" ").append(entry.getFormattedTimestamp())
                        .append(" | ").append(entry.getFingerprint())
                        .append(" |");
            }
            mdContent.append("\n");
        }
        
        // Write to file
        java.nio.file.Files.write(config.getDestinationFile().toPath(), 
                                 mdContent.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        
        logging.logToOutput("Markdown export completed");
    }
    
    /**
     * Validates that the export manager is ready to perform exports.
     * 
     * @return true if the export manager is ready, false otherwise
     */
    public boolean isReady() {
        return jsonExporter != null;
    }
    
    /**
     * Gets a list of supported export formats.
     * 
     * @return Array of supported ExportFormat values
     */
    public ExportConfiguration.ExportFormat[] getSupportedFormats() {
        return ExportConfiguration.ExportFormat.values();
    }
    
    /**
     * Estimates the size of the export data for a given configuration.
     * This can be used for progress indication or validation.
     * 
     * @param config The export configuration
     * @return Estimated size in bytes, or -1 if estimation is not possible
     */
    public long estimateExportSize(ExportConfiguration config) {
        if (config == null || config.getEntries() == null) {
            return -1;
        }
        
        // Rough estimation based on format and entry count
        int entryCount = config.getEntryCount();
        switch (config.getFormat()) {
            case JSON:
                return entryCount * (config.isIncludeFullData() ? 2000 : 200);
            case CSV:
                return entryCount * (config.isIncludeMetadata() ? 300 : 150);
            case HTML:
                return entryCount * (config.isIncludeMetadata() ? 400 : 200) + 1000; // + HTML overhead
            case MARKDOWN:
                return entryCount * (config.isIncludeMetadata() ? 250 : 120) + 500; // + MD overhead
            default:
                return -1;
        }
    }
} 