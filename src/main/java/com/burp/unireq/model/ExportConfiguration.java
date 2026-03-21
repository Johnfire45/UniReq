package com.burp.unireq.model;

import java.io.File;
import java.util.List;

/**
 * Configuration class for HTTP request export functionality.
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
    }

    // Core configuration
    private ExportFormat format;
    private File destinationFile;
    private List<RequestResponseEntry> entries;

    // Export options
    private boolean includeFullData;
    private boolean includeMetadata;

    // Additional metadata
    private String exportTitle;
    private String exportDescription;

    /**
     * Default constructor creates a minimal export configuration.
     */
    public ExportConfiguration() {
        this.includeMetadata = true;
        this.exportTitle = "UniReq HTTP Requests Export";
        this.exportDescription = "Exported HTTP requests from UniReq extension";
    }

    /**
     * Constructor with essential parameters for export configuration.
     *
     * @param format          The export format
     * @param destinationFile The target file for export
     * @param entries         The list of entries to export
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

        String fileName = destinationFile.getName().toLowerCase();
        String expectedExtension = "." + format.getExtension();
        if (!fileName.endsWith(expectedExtension)) {
            throw new IllegalStateException("File extension should be " + expectedExtension + " for " + format + " format");
        }
    }

    /**
     * @return The number of entries to be exported
     */
    public int getEntryCount() {
        return entries != null ? entries.size() : 0;
    }

    @Override
    public String toString() {
        return String.format("ExportConfiguration{format=%s, file='%s', entries=%d, fullData=%s}",
                format, destinationFile != null ? destinationFile.getName() : "null",
                getEntryCount(), includeFullData);
    }
}
