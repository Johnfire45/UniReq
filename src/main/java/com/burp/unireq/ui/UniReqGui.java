package com.burp.unireq.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.logging.Logging;
import com.burp.unireq.core.RequestDeduplicator;

import javax.swing.*;
import java.awt.*;

/**
 * UniReqGui - Main GUI component for the UniReq extension
 * 
 * This class provides the user interface for the UniReq extension, including
 * controls for managing request deduplication, viewing statistics, and 
 * inspecting unique HTTP requests and responses.
 * 
 * @author Harshit Shah
 */
public class UniReqGui {
    
    private final RequestDeduplicator deduplicator;
    private final Logging logging;
    private MontoyaApi api;
    
    // Main UI component
    private JPanel mainPanel;
    
    /**
     * Constructor initializes the GUI with the deduplicator and logging.
     * 
     * @param deduplicator The request deduplicator instance
     * @param logging Burp's logging interface
     */
    public UniReqGui(RequestDeduplicator deduplicator, Logging logging) {
        this.deduplicator = deduplicator;
        this.logging = logging;
        
        initializeComponents();
    }
    
    /**
     * Initializes the GUI components.
     */
    private void initializeComponents() {
        mainPanel = new JPanel(new BorderLayout());
        
        // Create a simple placeholder interface
        JLabel titleLabel = new JLabel("UniReq - HTTP Request Deduplicator", SwingConstants.CENTER);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        
        // Statistics section
        gbc.gridx = 0; gbc.gridy = 0;
        contentPanel.add(new JLabel("Total Requests:"), gbc);
        gbc.gridx = 1;
        JLabel totalLabel = new JLabel("0");
        contentPanel.add(totalLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        contentPanel.add(new JLabel("Unique Requests:"), gbc);
        gbc.gridx = 1;
        JLabel uniqueLabel = new JLabel("0");
        contentPanel.add(uniqueLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        contentPanel.add(new JLabel("Duplicate Requests:"), gbc);
        gbc.gridx = 1;
        JLabel duplicateLabel = new JLabel("0");
        contentPanel.add(duplicateLabel, gbc);
        
        // Control buttons
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton enableButton = new JButton("Enable Filtering");
        JButton clearButton = new JButton("Clear Data");
        
        buttonPanel.add(enableButton);
        buttonPanel.add(clearButton);
        contentPanel.add(buttonPanel, gbc);
        
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        
        // Add a status message
        JLabel statusLabel = new JLabel("Ready - Extension loaded successfully", SwingConstants.CENTER);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);
    }
    
    /**
     * Sets the Montoya API reference for the GUI.
     * 
     * @param api The Montoya API instance
     */
    public void setApi(MontoyaApi api) {
        this.api = api;
    }
    
    /**
     * Gets the main UI component for registration with Burp.
     * 
     * @return The main JPanel component
     */
    public Component getUiComponent() {
        return mainPanel;
    }
    
    /**
     * Updates the statistics display with current values.
     */
    public void updateStatistics() {
        // TODO: Implement statistics update
        // This method will be called by the RequestFingerprintListener
        // to update the GUI with current statistics
        
        if (deduplicator != null) {
            SwingUtilities.invokeLater(() -> {
                // Update the labels with current statistics
                // This is a placeholder implementation
                logging.logToOutput("GUI statistics updated");
            });
        }
    }
    
    /**
     * Cleanup method called when the extension is unloaded.
     */
    public void cleanup() {
        // TODO: Implement cleanup logic
        // Stop any timers, clear resources, etc.
        logging.logToOutput("GUI cleanup completed");
    }
} 