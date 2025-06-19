package com.burp.unireq.model;

import java.io.File;
import java.util.List;

/**
 * Configuration class for HTTP request export functionality.
 * 
 * This model encapsulates all the settings and data required for
 * exporting HTTP requests to various formats. It provides a clean
 * interface for passing export parameters between UI components
 * and the export service.
 * 
 * Supported Export Formats:
 * - CSV (Comma-Separated Values)
 * - HTML (Formatted web page)
 * - Markdown (GitHub-flavored markdown)
 * - JSON (JavaScript Object Notation)
 * 
 * Export Options:
 * - Basic metadata only (method, path, status, timestamp)
 * - Full request/response data (complete HTTP content)
 * - Custom file destination
 * - Format-specific options
 * 
 * @author Harshit Shah
 */
public class ExportConfiguration {
    
    /**
     * Enumeration of supported export formats.
     */
    public enum ExportFormat {
        CSV("csv", "Comma-Separated Values"),
        HTML("html", "HTML Document"),
        MARKDOWN("md", "Markdown Document"),
        JSON("json", "JSON Document");
        
        private final String extension;
        private final String description;
        
        ExportFormat(String extension, String description) {
            this.extension = extension;
            this.description = description;
        }
        
        public String getExtension() {
            return extension;
        }
        
        public String getDescription() {
            return description;
        }
        
        /**
         * Parses a string to an ExportFormat enum.
         * 
         * @param format The format string (case-insensitive)
         * @return The corresponding ExportFormat
         * @throws IllegalArgumentException if the format is not supported
         */
        public static ExportFormat fromString(String format) {
            if (format == null) {
                throw new IllegalArgumentException("Format cannot be null");
            }
            
            for (ExportFormat f : values()) {
                if (f.name().equalsIgnoreCase(format)) {
                    return f;
                }
            }
            
            throw new IllegalArgumentException("Unsupported export format: " + format);
        }
    }
    
    // Core configuration
    private ExportFormat format;
    private File destinationFile;
    private List<RequestResponseEntry> entries;
    
    // Export options
    private boolean includeFullData;
    private boolean includeMetadata;
    private boolean prettifyOutput;
    
    // Additional metadata
    private String exportTitle;
    private String exportDescription;
    
    /**
     * Default constructor creates a minimal export configuration.
     * Additional settings must be set before export.
     */
    public ExportConfiguration() {
        this.includeMetadata = true;
        this.prettifyOutput = true;
        this.exportTitle = "UniReq HTTP Requests Export";
        this.exportDescription = "Exported HTTP requests from UniReq extension";
    }
    
    /**
     * Constructor with essential parameters for export configuration.
     * 
     * @param format The export format
     * @param destinationFile The target file for export
     * @param entries The list of entries to export
     * @param includeFullData Whether to include full request/response data
     */
    public ExportConfiguration(ExportFormat format, File destinationFile, 
                             List<RequestResponseEntry> entries, boolean includeFullData) {
        this();
        this.format = format;
        this.destinationFile = destinationFile;
        this.entries = entries;
        this.includeFullData = includeFullData;
    }
    
    // ==================== Getters and Setters ====================
    
    public ExportFormat getFormat() {
        return format;
    }
    
    public void setFormat(ExportFormat format) {
        this.format = format;
    }
    
    public File getDestinationFile() {
        return destinationFile;
    }
    
    public void setDestinationFile(File destinationFile) {
        this.destinationFile = destinationFile;
    }
    
    public List<RequestResponseEntry> getEntries() {
        return entries;
    }
    
    public void setEntries(List<RequestResponseEntry> entries) {
        this.entries = entries;
    }
    
    public boolean isIncludeFullData() {
        return includeFullData;
    }
    
    public void setIncludeFullData(boolean includeFullData) {
        this.includeFullData = includeFullData;
    }
    
    public boolean isIncludeMetadata() {
        return includeMetadata;
    }
    
    public void setIncludeMetadata(boolean includeMetadata) {
        this.includeMetadata = includeMetadata;
    }
    
    public boolean isPrettifyOutput() {
        return prettifyOutput;
    }
    
    public void setPrettifyOutput(boolean prettifyOutput) {
        this.prettifyOutput = prettifyOutput;
    }
    
    public String getExportTitle() {
        return exportTitle;
    }
    
    public void setExportTitle(String exportTitle) {
        this.exportTitle = exportTitle != null ? exportTitle : "UniReq Export";
    }
    
    public String getExportDescription() {
        return exportDescription;
    }
    
    public void setExportDescription(String exportDescription) {
        this.exportDescription = exportDescription != null ? exportDescription : "";
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Validates the export configuration for completeness and correctness.
     * 
     * @throws IllegalStateException if the configuration is invalid
     */
    public void validate() {
        if (format == null) {
            throw new IllegalStateException("Export format must be specified");
        }
        
        if (destinationFile == null) {
            throw new IllegalStateException("Destination file must be specified");
        }
        
        if (entries == null || entries.isEmpty()) {
            throw new IllegalStateException("No entries to export");
        }
        
        // Validate file extension matches format
        String fileName = destinationFile.getName().toLowerCase();
        String expectedExtension = "." + format.getExtension();
        if (!fileName.endsWith(expectedExtension)) {
            throw new IllegalStateException("File extension should be " + expectedExtension + " for " + format + " format");
        }
    }
    
    /**
     * Checks if the configuration is valid without throwing exceptions.
     * 
     * @return true if the configuration is valid for export
     */
    public boolean isValid() {
        try {
            validate();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }
    
    /**
     * @return The number of entries to be exported
     */
    public int getEntryCount() {
        return entries != null ? entries.size() : 0;
    }
    
    /**
     * @return true if this export includes full request/response data
     */
    public boolean isFullExport() {
        return includeFullData;
    }
    
    /**
     * Generates a suggested file name based on export settings.
     * 
     * @return A suggested file name with appropriate extension
     */
    public String getSuggestedFileName() {
        String timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("unireq_export_%s.%s", timestamp, format.getExtension());
    }
    
    /**
     * Creates a copy of this ExportConfiguration with the same settings.
     * 
     * @return A new ExportConfiguration with identical settings
     */
    public ExportConfiguration copy() {
        ExportConfiguration copy = new ExportConfiguration();
        copy.format = this.format;
        copy.destinationFile = this.destinationFile;
        copy.entries = this.entries; // Shallow copy - entries are immutable
        copy.includeFullData = this.includeFullData;
        copy.includeMetadata = this.includeMetadata;
        copy.prettifyOutput = this.prettifyOutput;
        copy.exportTitle = this.exportTitle;
        copy.exportDescription = this.exportDescription;
        return copy;
    }
    
    @Override
    public String toString() {
        return String.format("ExportConfiguration{format=%s, file='%s', entries=%d, fullData=%s}",
                           format, destinationFile != null ? destinationFile.getName() : "null",
                           getEntryCount(), includeFullData);
    }
} 