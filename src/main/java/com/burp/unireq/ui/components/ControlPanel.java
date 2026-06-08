package com.burp.unireq.ui.components;

import com.burp.unireq.utils.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ControlPanel - Control buttons and status component for UniReq extension
 * 
 * This component provides action buttons for controlling the deduplication
 * functionality and displays status information. It supports customizable
 * action listeners and thread-safe status updates.
 * 
 * Features:
 * - Enable/Disable filtering toggle button
 * - Clear data action button
 * - Refresh action button
 * - Status label with thread-safe updates
 * - Customizable action listeners
 * - Clean layout with proper spacing
 * 
 * @author Harshit Shah
 */
public class ControlPanel extends JPanel {
    
    // Control buttons
    private final JButton enableButton;
    private final JButton clearButton;
    private final JButton refreshButton;
    
    // Status display
    private final JLabel statusLabel;
    
    // Action identifiers
    public static final String ACTION_TOGGLE_FILTERING = "toggle_filtering";
    public static final String ACTION_CLEAR_DATA = "clear_data";
    public static final String ACTION_REFRESH = "refresh";
    
    // Action listeners
    private final List<ControlActionListener> actionListeners;
    
    /**
     * Interface for listening to control panel actions.
     */
    public interface ControlActionListener {
        /**
         * Called when a control action is performed.
         * 
         * @param action The action identifier (one of the ACTION_* constants)
         * @param source The button that triggered the action
         */
        void onControlAction(String action, JButton source);
    }
    
    /**
     * Constructor initializes the control panel with default configuration.
     */
    public ControlPanel() {
        actionListeners = new ArrayList<>();
        
        // Create buttons with modern styling
        enableButton = SwingUtils.createModernButton("Enable Filtering",
                "Toggle deduplication tracking on/off. All requests are always forwarded — UniReq only tracks unique ones.", null);
        clearButton = SwingUtils.createModernButton("Clear Data", "Clear all stored requests and statistics", null);
        refreshButton = SwingUtils.createModernButton("Refresh", "Refresh the request table and statistics", null);
        
        // Create status label
        statusLabel = new JLabel("Ready - Extension loaded successfully", SwingConstants.CENTER);
        
        initializeComponents();
        setupActionListeners();
    }
    
    /**
     * Initializes the panel components and layout.
     */
    private void initializeComponents() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));
        setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        
        // Add buttons directly with compact spacing
        add(enableButton);
        add(clearButton);
        add(refreshButton);
        
        // Add status label inline if needed (or can be removed for extra compactness)
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 10));
        statusLabel.setForeground(new Color(100, 100, 100));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        add(statusLabel);
    }
    
    /**
     * Sets up action listeners for the buttons.
     */
    private void setupActionListeners() {
        enableButton.addActionListener(e -> notifyActionListeners(ACTION_TOGGLE_FILTERING, enableButton));
        clearButton.addActionListener(e -> notifyActionListeners(ACTION_CLEAR_DATA, clearButton));
        refreshButton.addActionListener(e -> notifyActionListeners(ACTION_REFRESH, refreshButton));
    }
    
    /**
     * Adds an action listener to be notified of control actions.
     * 
     * @param listener The listener to add
     */
    public void addActionListener(ControlActionListener listener) {
        if (listener != null) {
            actionListeners.add(listener);
        }
    }
    
    /**
     * Notifies all action listeners of a control action.
     * 
     * @param action The action identifier
     * @param source The button that triggered the action
     */
    private void notifyActionListeners(String action, JButton source) {
        for (ControlActionListener listener : actionListeners) {
            try {
                listener.onControlAction(action, source);
            } catch (Exception e) {
                // Ignore listener errors
            }
        }
    }
    
    /**
     * Updates the enable/disable button text based on filtering state.
     * This method is thread-safe and can be called from any thread.
     * 
     * @param filteringEnabled true if filtering is enabled, false otherwise
     */
    public void updateFilteringButton(boolean filteringEnabled) {
        SwingUtilities.invokeLater(() -> {
            enableButton.setText(filteringEnabled ? "Disable Filtering" : "Enable Filtering");
        });
    }
    
    /**
     * Updates the status label with formatting for different message types.
     * This method is thread-safe and can be called from any thread.
     * 
     * @param status The status message
     * @param type The message type (info, warning, error)
     */
    public void updateStatus(String status, StatusType type) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status != null ? status : "");
            
            // Set color based on type
            switch (type) {
                case INFO:
                    statusLabel.setForeground(Color.BLACK);
                    break;
                case WARNING:
                    statusLabel.setForeground(new Color(204, 102, 0)); // Orange
                    break;
                case ERROR:
                    statusLabel.setForeground(Color.RED);
                    break;
                case SUCCESS:
                    statusLabel.setForeground(new Color(0, 128, 0)); // Green
                    break;
            }
        });
    }
    
    /**
     * Enumeration for status message types.
     */
    public enum StatusType {
        INFO, WARNING, ERROR, SUCCESS
    }
    
}
