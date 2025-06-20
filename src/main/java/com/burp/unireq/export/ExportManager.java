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
    private final CsvExporter csvExporter;
    private final MarkdownExporter markdownExporter;
    
    /**
     * Constructor initializes the export manager with logging support.
     * 
     * @param logging Burp's logging interface for debug output and error reporting
     */
    public ExportManager(Logging logging) {
        this.logging = logging;
        this.jsonExporter = new JsonExporter(logging);
        this.csvExporter = new CsvExporter(logging);
        this.markdownExporter = new MarkdownExporter(logging);
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
                    csvExporter.export(config);
                    break;
                case HTML:
                    exportToHtml(config);
                    break;
                case MARKDOWN:
                    markdownExporter.export(config);
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
     * Exports data to HTML format.
     * Creates a styled HTML table with proper formatting and CSS.
     * 
     * @param config The export configuration
     * @throws IOException if the export fails
     */
    private void exportToHtml(ExportConfiguration config) throws IOException {
        // Create HTML content with table styling
        
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
     * Validates that the export manager is ready to perform exports.
     * 
     * @return true if the export manager is ready, false otherwise
     */
    public boolean isReady() {
        return jsonExporter != null && csvExporter != null && markdownExporter != null;
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
        
        // Use modular exporters for accurate size estimation
        switch (config.getFormat()) {
            case JSON:
                return jsonExporter.estimateSize(config);
            case CSV:
                return csvExporter.estimateSize(config);
            case HTML:
                // Keep simple estimation for HTML (still embedded)
                int entryCount = config.getEntryCount();
                return entryCount * (config.isIncludeMetadata() ? 400 : 200) + 1000; // + HTML overhead
            case MARKDOWN:
                return markdownExporter.estimateSize(config);
            default:
                return -1;
        }
    }
} 