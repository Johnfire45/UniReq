package com.burp.unireq.export;

import com.burp.unireq.model.ExportConfiguration;
import com.burp.unireq.model.RequestResponseEntry;
import burp.api.montoya.logging.Logging;

import java.io.IOException;
import java.util.List;

/**
 * Coordinates HTTP request data export across supported formats.
 *
 * @author Harshit Shah
 */
public class ExportManager {

    private final Logging logging;
    private final JsonExporter jsonExporter;
    private final CsvExporter csvExporter;
    private final MarkdownExporter markdownExporter;

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
     * @throws IOException              if the export operation fails
     * @throws IllegalArgumentException if the configuration is invalid
     */
    public void exportData(ExportConfiguration config) throws IOException {
        if (config == null) {
            throw new IllegalArgumentException("Export configuration cannot be null");
        }

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

    private void exportToHtml(ExportConfiguration config) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head>\n");
        html.append("<title>").append(config.getExportTitle()).append("</title>\n");
        html.append("<style>\n");
        html.append("table { border-collapse: collapse; width: 100%; }\n");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        html.append("th { background-color: #f2f2f2; }\n");
        html.append("</style>\n");
        html.append("</head>\n<body>\n");

        html.append("<h1>").append(config.getExportTitle()).append("</h1>\n");
        if (!config.getExportDescription().isEmpty()) {
            html.append("<p>").append(config.getExportDescription()).append("</p>\n");
        }

        html.append("<table>\n<tr>");
        html.append("<th>Method</th><th>Host</th><th>Path</th><th>Status</th>");
        if (config.isIncludeMetadata()) {
            html.append("<th>Timestamp</th><th>Fingerprint</th>");
        }
        html.append("</tr>\n");

        for (RequestResponseEntry entry : config.getEntries()) {
            html.append("<tr>");
            html.append("<td>").append(entry.getMethod()).append("</td>");
            html.append("<td>").append(entry.getRequest().httpService().host()).append("</td>");
            html.append("<td>").append(entry.getPath()).append("</td>");
            html.append("<td>").append(entry.getStatusCode()).append("</td>");
            if (config.isIncludeMetadata()) {
                html.append("<td>").append(entry.getFormattedTimestamp()).append("</td>");
                html.append("<td>").append(entry.getFingerprint()).append("</td>");
            }
            html.append("</tr>\n");
        }

        html.append("</table>\n</body>\n</html>");

        java.nio.file.Files.write(config.getDestinationFile().toPath(),
                html.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));

        logging.logToOutput("HTML export completed");
    }
}
