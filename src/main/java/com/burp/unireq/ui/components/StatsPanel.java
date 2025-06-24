package com.burp.unireq.ui.components;

import javax.swing.*;
import java.awt.*;

/**
 * StatsPanel - Statistics display component for UniReq extension
 * 
 * This component displays the current deduplication statistics including
 * total requests processed, unique requests identified, and duplicate
 * requests blocked. It provides colored labels for better visual distinction.
 * 
 * Features:
 * - Thread-safe statistics updates
 * - Color-coded labels (green for unique, red for duplicates)
 * - Clean titled border layout
 * - Bold font for emphasis on numbers
 * 
 * @author Harshit Shah
 */
public class StatsPanel extends JPanel {
    
    // Statistics labels that need to be updated
    private final JLabel totalLabel;
    private final JLabel uniqueLabel;
    private final JLabel duplicateLabel;
    private final JLabel visibleLabel;
    
    /**
     * Constructor initializes the statistics panel with default values.
     */
    public StatsPanel() {
        // Initialize labels first
        totalLabel = new JLabel("0");
        uniqueLabel = new JLabel("0");
        duplicateLabel = new JLabel("0");
        visibleLabel = new JLabel("0 of 0");
        
        initializeComponents();
    }
    
    /**
     * Initializes the panel components and layout.
     */
    private void initializeComponents() {
        // Use compact horizontal layout with reduced spacing
        setLayout(new FlowLayout(FlowLayout.LEFT, 10, 2));
        // Remove titled border for compact design
        setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        
        // Create stats labels with compact formatting
        JPanel totalPanel = createStatPanel("Total:", totalLabel, Color.BLACK);
        JPanel uniquePanel = createStatPanel("Unique:", uniqueLabel, new Color(0, 128, 0)); // Green
        JPanel duplicatePanel = createStatPanel("Duplicates:", duplicateLabel, new Color(128, 0, 0)); // Red
        JPanel visiblePanel = createStatPanel("Visible:", visibleLabel, Color.BLACK);
        
        // Add panels to main layout
        add(totalPanel);
        add(uniquePanel);
        add(duplicatePanel);
        add(visiblePanel);
    }
    
    /**
     * Creates a formatted statistics panel with label and value.
     * 
     * @param labelText The text for the label
     * @param valueLabel The JLabel to display the value
     * @param valueColor The color for the value label
     * @return A JPanel containing the formatted statistic
     */
    private JPanel createStatPanel(String labelText, JLabel valueLabel, Color valueColor) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
        
        // Create label with compact font
        JLabel label = new JLabel(labelText);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        panel.add(label);
        
        // Format the value label with compact font
        valueLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        valueLabel.setForeground(valueColor);
        panel.add(valueLabel);
        
        return panel;
    }
    
    /**
     * Updates the statistics display with new values.
     * This method is thread-safe and can be called from any thread.
     * 
     * @param total Total number of requests processed
     * @param unique Number of unique requests identified
     * @param duplicates Number of duplicate requests blocked
     * @param visible Number of visible requests (after filtering)
     */
    public void updateStatistics(long total, long unique, long duplicates, long visible) {
        SwingUtilities.invokeLater(() -> {
            totalLabel.setText(String.valueOf(total));
            uniqueLabel.setText(String.valueOf(unique));
            duplicateLabel.setText(String.valueOf(duplicates));
            visibleLabel.setText(visible + " of " + unique);
            
            // Repaint to ensure visual updates
            repaint();
        });
    }
    
    /**
     * Updates the statistics display with new values (backward compatibility).
     * This method is thread-safe and can be called from any thread.
     * 
     * @param total Total number of requests processed
     * @param unique Number of unique requests identified
     * @param duplicates Number of duplicate requests blocked
     */
    public void updateStatistics(long total, long unique, long duplicates) {
        updateStatistics(total, unique, duplicates, unique);
    }
    
    /**
     * Resets all statistics to zero.
     * This method is thread-safe and can be called from any thread.
     */
    public void resetStatistics() {
        updateStatistics(0, 0, 0, 0);
    }
    
    /**
     * Gets the current total value displayed.
     * 
     * @return The current total value as a string
     */
    public String getTotalValue() {
        return totalLabel.getText();
    }
    
    /**
     * Gets the current unique value displayed.
     * 
     * @return The current unique value as a string
     */
    public String getUniqueValue() {
        return uniqueLabel.getText();
    }
    
    /**
     * Gets the current duplicates value displayed.
     * 
     * @return The current duplicates value as a string
     */
    public String getDuplicatesValue() {
        return duplicateLabel.getText();
    }
} 