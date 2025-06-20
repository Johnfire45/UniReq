package com.burp.unireq.export;

import com.burp.unireq.model.ExportConfiguration;
import com.burp.unireq.model.RequestResponseEntry;
import burp.api.montoya.logging.Logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;

/**
 * MarkdownExporter - Handles export of HTTP request data to Markdown format
 * 
 * This class provides specialized Markdown export functionality with GitHub-flavored
 * markdown table formatting and proper escaping of special characters.
 * 
 * Markdown Output Structure:
 * - Title and description (if metadata enabled)
 * - GitHub-flavored markdown table with proper alignment
 * - Pipe-separated columns with header separators
 * - Proper escaping of pipe characters and newlines
 * - UTF-8 encoding for international character support
 * - Clean, readable formatting for documentation
 * 
 * Security Features:
 * - Escapes markdown special characters (|, `, *, _, etc.)
 * - Handles newlines and special formatting safely
 * - Sanitizes content for safe rendering
 * 
 * @author Harshit Shah
 */
public class MarkdownExporter {
    
    private final Logging logging;
    
    /**
     * Constructor initializes the Markdown exporter with logging support.
     * 
     * @param logging Burp's logging interface for debug output and error reporting
     */
    public MarkdownExporter(Logging logging) {
        this.logging = logging;
    }
    
    /**
     * Exports request/response data to Markdown format according to the configuration.
     * 
     * @param config The export configuration
     * @throws IOException if the export operation fails
     */
    public void export(ExportConfiguration config) throws IOException {
        logging.logToOutput("Starting Markdown export with " + config.getEntryCount() + " entries");
        
        StringBuilder mdContent = new StringBuilder();
        
        // Add document header and metadata
        addMarkdownHeader(mdContent, config);
        
        // Add table header
        addMarkdownTableHeader(mdContent, config);
        
        // Add data rows
        for (RequestResponseEntry entry : config.getEntries()) {
            addMarkdownTableRow(mdContent, entry, config);
        }
        
        // Add footer if metadata is enabled
        if (config.isIncludeMetadata()) {
            mdContent.append("\n---\n");
            mdContent.append("*Export generated on ").append(java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("*\n");
            mdContent.append("*Total entries: ").append(config.getEntryCount()).append("*\n");
        }
        
        // Write to file with UTF-8 encoding
        Files.write(config.getDestinationFile().toPath(), 
                   mdContent.toString().getBytes(StandardCharsets.UTF_8));
        
        logging.logToOutput("Markdown export completed: " + config.getDestinationFile().getName());
    }
    
    /**
     * Adds the document header and metadata section.
     * 
     * @param md The StringBuilder to append to
     * @param config The export configuration
     */
    private void addMarkdownHeader(StringBuilder md, ExportConfiguration config) {
        // Document title
        md.append("# ").append(escapeMarkdown(config.getExportTitle())).append("\n\n");
        
        // Description if available
        if (config.isIncludeMetadata() && !config.getExportDescription().isEmpty()) {
            md.append(escapeMarkdown(config.getExportDescription())).append("\n\n");
        }
        
        // Export summary
        if (config.isIncludeMetadata()) {
            md.append("## Export Summary\n\n");
            md.append("- **Total Entries**: ").append(config.getEntryCount()).append("\n");
            md.append("- **Export Format**: ").append(config.getFormat().getDescription()).append("\n");
            md.append("- **Include Full Data**: ").append(config.isIncludeFullData() ? "Yes" : "No").append("\n");
            md.append("- **Export Time**: ").append(java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
        }
        
        // Table section header
        md.append("## HTTP Requests\n\n");
    }
    
    /**
     * Adds the markdown table header with proper alignment.
     * 
     * @param md The StringBuilder to append to
     * @param config The export configuration
     */
    private void addMarkdownTableHeader(StringBuilder md, ExportConfiguration config) {
        // Header row
        md.append("| Method | Host | Path | Status |");
        if (config.isIncludeMetadata()) {
            md.append(" Timestamp | Fingerprint |");
        }
        if (config.isIncludeFullData()) {
            md.append(" Request Headers | Request Body | Response Headers | Response Body |");
        }
        md.append("\n");
        
        // Separator row with alignment
        md.append("|--------|------|------|--------|");
        if (config.isIncludeMetadata()) {
            md.append("-----------|-------------|");
        }
        if (config.isIncludeFullData()) {
            md.append("----------------|--------------|------------------|---------------|");
        }
        md.append("\n");
    }
    
    /**
     * Adds a single data row to the markdown table.
     * 
     * @param md The StringBuilder to append to
     * @param entry The request/response entry to add
     * @param config The export configuration
     */
    private void addMarkdownTableRow(StringBuilder md, RequestResponseEntry entry, ExportConfiguration config) {
        md.append("| ").append(escapeMarkdown(entry.getMethod()));
        md.append(" | ").append(escapeMarkdown(entry.getRequest().httpService().host()));
        md.append(" | ").append(escapeMarkdown(entry.getPath()));
        md.append(" | ").append(escapeMarkdown(entry.getStatusCode()));
        
        // Optional metadata columns
        if (config.isIncludeMetadata()) {
            md.append(" | ").append(escapeMarkdown(entry.getTimestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
            md.append(" | ").append(escapeMarkdown(entry.getFingerprint().substring(0, Math.min(12, entry.getFingerprint().length())) + "..."));
        }
        
        // Optional full data columns
        if (config.isIncludeFullData()) {
            md.append(" | ");
            // Request headers (truncated for readability)
            String reqHeaders = entry.getRequest().headers().toString();
            md.append(escapeMarkdown(truncateForTable(reqHeaders, 50)));
            
            md.append(" | ");
            // Request body (truncated for readability)
            if (entry.getRequest().body() != null && entry.getRequest().body().length() > 0) {
                String reqBody = entry.getRequest().bodyToString();
                md.append(escapeMarkdown(truncateForTable(reqBody, 30)));
            }
            
            md.append(" | ");
            // Response headers (truncated for readability)
            if (entry.getResponse() != null) {
                String respHeaders = entry.getResponse().headers().toString();
                md.append(escapeMarkdown(truncateForTable(respHeaders, 50)));
            }
            
            md.append(" | ");
            // Response body (truncated for readability)
            if (entry.getResponse() != null && entry.getResponse().body() != null && entry.getResponse().body().length() > 0) {
                String respBody = entry.getResponse().bodyToString();
                md.append(escapeMarkdown(truncateForTable(respBody, 30)));
            }
        }
        
        md.append(" |\n");
    }
    
    /**
     * Escapes special characters in markdown content.
     * 
     * This method handles:
     * - Pipe characters (escaped for table formatting)
     * - Backticks (escaped to prevent code formatting)
     * - Asterisks and underscores (escaped to prevent emphasis)
     * - Newlines (converted to spaces for table cells)
     * - Other markdown special characters
     * 
     * @param input The input string to escape
     * @return The escaped string safe for markdown
     */
    private String escapeMarkdown(String input) {
        if (input == null) {
            return "";
        }
        
        return input
            .replace("|", "\\|")           // Escape pipes for table formatting
            .replace("`", "\\`")           // Escape backticks
            .replace("*", "\\*")           // Escape asterisks
            .replace("_", "\\_")           // Escape underscores
            .replace("#", "\\#")           // Escape headers
            .replace("[", "\\[")           // Escape link brackets
            .replace("]", "\\]")           // Escape link brackets
            .replace("(", "\\(")           // Escape link parentheses
            .replace(")", "\\)")           // Escape link parentheses
            .replace("\n", " ")            // Convert newlines to spaces for table cells
            .replace("\r", " ")            // Convert carriage returns to spaces
            .replace("\t", " ")            // Convert tabs to spaces
            .replaceAll("\\s+", " ")       // Collapse multiple spaces
            .trim();                       // Remove leading/trailing whitespace
    }
    
    /**
     * Truncates content for table display with ellipsis.
     * 
     * @param content The content to truncate
     * @param maxLength The maximum length before truncation
     * @return The truncated content with ellipsis if needed
     */
    private String truncateForTable(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        
        // Remove newlines and normalize whitespace first
        String normalized = content.replaceAll("\\s+", " ").trim();
        
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        
        return normalized.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Validates that the Markdown exporter is ready to perform exports.
     * 
     * @return true if the exporter is ready, false otherwise
     */
    public boolean isReady() {
        return true; // Markdown exporter has no external dependencies
    }
    
    /**
     * Estimates the size of a Markdown export for the given configuration.
     * 
     * @param config The export configuration
     * @return Estimated size in bytes
     */
    public long estimateSize(ExportConfiguration config) {
        if (config == null || config.getEntries() == null) {
            return 0;
        }
        
        int headerSize = config.isIncludeMetadata() ? 500 : 200; // Document header and metadata
        int tableHeaderSize = config.isIncludeFullData() ? 200 : 100; // Table header
        int basePerEntrySize = config.isIncludeMetadata() ? 250 : 120; // Basic table row
        int fullDataPerEntrySize = config.isIncludeFullData() ? 800 : 0; // Additional columns
        int footerSize = config.isIncludeMetadata() ? 100 : 0; // Footer
        
        return headerSize + tableHeaderSize + footerSize + 
               (config.getEntryCount() * (basePerEntrySize + fullDataPerEntrySize));
    }
} 