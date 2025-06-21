package com.burp.unireq.ui.components;

import com.burp.unireq.model.ExportConfiguration;
import com.burp.unireq.utils.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ExportPanel - Export functionality component for UniReq extension
 * 
 * This component provides export controls for exporting HTTP request data
 * in various formats. It supports format selection, export actions, and
 * status feedback with thread-safe operations.
 * 
 * Features:
 * - Format selection dropdown (JSON, CSV, HTML, Markdown)
 * - Export action button with file chooser integration
 * - Status label with thread-safe updates and color coding
 * - Customizable action listeners for export events
 * - Clean horizontal layout with proper spacing
 * - Consistent styling with other UniReq components
 * 
 * @author Harshit Shah
 */
public class ExportPanel extends JPanel {
    
    // UI Components
    private final JComboBox<ExportConfiguration.ExportFormat> formatComboBox;
    private final JButton exportButton;
    private final JLabel statusLabel;
    
    // Action listeners
    private final List<ExportActionListener> actionListeners;
    
    /**
     * Interface for listening to export panel actions.
     */
    public interface ExportActionListener {
        /**
         * Called when an export action is requested.
         * 
         * @param format The selected export format
         */
        void onExportRequested(ExportConfiguration.ExportFormat format);
    }
    
    /**
     * Constructor initializes the export panel with default configuration.
     */
    public ExportPanel() {
        actionListeners = new ArrayList<>();
        
        // Create format combo box with modern styling
        String[] formatNames = new String[ExportConfiguration.ExportFormat.values().length];
        ExportConfiguration.ExportFormat[] formats = ExportConfiguration.ExportFormat.values();
        for (int i = 0; i < formats.length; i++) {
            formatNames[i] = formats[i].name();
        }
        formatComboBox = new JComboBox<>(ExportConfiguration.ExportFormat.values());
        formatComboBox.setSelectedItem(ExportConfiguration.ExportFormat.JSON); // Default to JSON
        formatComboBox.setToolTipText("Select export format");
        formatComboBox.setBorder(SwingUtils.createRoundedBorder(SwingUtils.BORDER_RADIUS, SwingUtils.BORDER_COLOR));
        formatComboBox.setBackground(Color.WHITE);
        
        // Create export button using modern styling
        exportButton = SwingUtils.createModernButton(
            "Export", 
            "Export unique requests to selected format", 
            e -> handleExportAction()
        );
        
        // Create status label
        statusLabel = SwingUtils.createStatusLabel("Ready for export", SwingUtils.StatusType.INFO);
        
        initializeComponents();
    }
    
    /**
     * Initializes the panel components and layout.
     */
    private void initializeComponents() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 2));
        setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        
        // Create compact format label
        JLabel formatLabel = new JLabel("Format:");
        formatLabel.setLabelFor(formatComboBox);
        formatLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        
        // Set compact size for combo box
        formatComboBox.setPreferredSize(new Dimension(80, 26));
        
        // Set compact size for export button
        exportButton.setPreferredSize(new Dimension(70, 26));
        
        // Create compact status label (inline)
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 9));
        statusLabel.setForeground(new Color(100, 100, 100)); // Gray text
        statusLabel.setText("Ready");
        
        // Add components horizontally with compact spacing
        add(formatLabel);
        add(formatComboBox);
        add(exportButton);
        add(statusLabel);
    }
    
    /**
     * Handles the export button action.
     */
    private void handleExportAction() {
        ExportConfiguration.ExportFormat selectedFormat = 
            (ExportConfiguration.ExportFormat) formatComboBox.getSelectedItem();
        
        if (selectedFormat != null) {
            notifyActionListeners(selectedFormat);
        }
    }
    
    /**
     * Adds an action listener to be notified of export actions.
     * 
     * @param listener The listener to add
     */
    public void addActionListener(ExportActionListener listener) {
        if (listener != null) {
            actionListeners.add(listener);
        }
    }
    
    /**
     * Removes an action listener.
     * 
     * @param listener The listener to remove
     */
    public void removeActionListener(ExportActionListener listener) {
        actionListeners.remove(listener);
    }
    
    /**
     * Notifies all action listeners of an export action.
     * 
     * @param format The selected export format
     */
    private void notifyActionListeners(ExportConfiguration.ExportFormat format) {
        for (ExportActionListener listener : actionListeners) {
            try {
                listener.onExportRequested(format);
            } catch (Exception e) {
                // Log error silently - don't expose internal errors to user
                updateStatus("Export failed: " + e.getMessage(), SwingUtils.StatusType.ERROR);
            }
        }
    }
    
    /**
     * Updates the status label text.
     * This method is thread-safe and can be called from any thread.
     * 
     * @param status The new status message
     */
    public void updateStatus(String status) {
        updateStatus(status, SwingUtils.StatusType.INFO);
    }
    
    /**
     * Updates the status label with formatting for different message types.
     * This method is thread-safe and can be called from any thread.
     * 
     * @param status The status message
     * @param type The message type (info, warning, error, success)
     */
    public void updateStatus(String status, SwingUtils.StatusType type) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status != null ? status : "Ready for export");
            
            // Set color based on type
            switch (type) {
                case SUCCESS:
                    statusLabel.setForeground(SwingUtils.SUCCESS_COLOR);
                    break;
                case WARNING:
                    statusLabel.setForeground(SwingUtils.WARNING_COLOR);
                    break;
                case ERROR:
                    statusLabel.setForeground(SwingUtils.ERROR_COLOR);
                    break;
                case INFO:
                default:
                    statusLabel.setForeground(SwingUtils.INFO_COLOR);
                    break;
            }
        });
    }
    
    /**
     * Sets the export button enabled state with appropriate tooltip.
     * This method is thread-safe and can be called from any thread.
     * 
     * @param enabled true to enable the button, false to disable
     * @param requestCount the number of available requests for context
     */
    public void setExportEnabled(boolean enabled, int requestCount) {
        SwingUtilities.invokeLater(() -> {
            exportButton.setEnabled(enabled);
            formatComboBox.setEnabled(enabled);
            
            // Update tooltip based on state
            if (enabled) {
                exportButton.setToolTipText("Export " + requestCount + " unique requests to selected format");
                formatComboBox.setToolTipText("Select export format");
            } else {
                exportButton.setToolTipText("Export is disabled when no requests are available");
                formatComboBox.setToolTipText("Export is disabled when no requests are available");
            }
        });
    }
    
    /**
     * Sets the export button enabled state.
     * This method is thread-safe and can be called from any thread.
     * 
     * @param enabled true to enable the button, false to disable
     */
    public void setExportEnabled(boolean enabled) {
        setExportEnabled(enabled, 0);
    }
    
    /**
     * Gets the currently selected export format.
     * 
     * @return The selected ExportFormat, or null if none selected
     */
    public ExportConfiguration.ExportFormat getSelectedFormat() {
        return (ExportConfiguration.ExportFormat) formatComboBox.getSelectedItem();
    }
    
    /**
     * Sets the selected export format.
     * This method is thread-safe and can be called from any thread.
     * 
     * @param format The format to select
     */
    public void setSelectedFormat(ExportConfiguration.ExportFormat format) {
        SwingUtilities.invokeLater(() -> {
            formatComboBox.setSelectedItem(format);
        });
    }
    
    /**
     * Gets the export button component.
     * 
     * @return The JButton instance
     */
    public JButton getExportButton() {
        return exportButton;
    }
    
    /**
     * Gets the format combo box component.
     * 
     * @return The JComboBox instance
     */
    public JComboBox<ExportConfiguration.ExportFormat> getFormatComboBox() {
        return formatComboBox;
    }
    
    /**
     * Gets the status label component.
     * 
     * @return The JLabel instance
     */
    public JLabel getStatusLabel() {
        return statusLabel;
    }
} 