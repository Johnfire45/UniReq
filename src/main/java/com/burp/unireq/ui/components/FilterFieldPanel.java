package com.burp.unireq.ui.components;

import com.burp.unireq.utils.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * FilterFieldPanel - Reusable component for text filtering with advanced options
 * 
 * Provides a text field with compact toggle buttons for:
 * - Regex mode ([.*])
 * - Case sensitivity ([Aa])  
 * - Invert match ([!])
 * 
 * This component encapsulates the pattern used in host/path filtering
 * and can be reused across different filter dialogs.
 * 
 * @author Harshit Shah
 */
public class FilterFieldPanel extends JPanel {
    
    // UI Components
    private final JTextField textField;
    private final JToggleButton regexToggle;
    private final JToggleButton caseToggle;
    private final JToggleButton invertToggle;
    
    // Configuration
    private final String fieldName;
    
    /**
     * Constructor creates a filter field panel with specified configuration.
     * 
     * @param fieldName The name of the field (used for placeholder and tooltips)
     * @param fieldWidth The preferred width of the text field
     */
    public FilterFieldPanel(String fieldName, int fieldWidth) {
        this.fieldName = fieldName;
        
        // Create components
        textField = SwingUtils.createModernTextField(
            fieldName + " filter (e.g., example.com)", fieldWidth);
        
        regexToggle = createCompactToggle(".*", "Enable regex mode for " + fieldName + " filter");
        caseToggle = createCompactToggle("Aa", "Enable case-sensitive matching");
        invertToggle = createCompactToggle("!", "Invert " + fieldName + " filter match");
        
        initializeLayout();
    }
    
    /**
     * Creates a compact toggle button for filter controls.
     * 
     * @param text The text to display on the button
     * @param tooltip The tooltip text
     * @return A configured JToggleButton
     */
    private JToggleButton createCompactToggle(String text, String tooltip) {
        JToggleButton toggle = new JToggleButton(text);
        toggle.setPreferredSize(new Dimension(20, 26));
        toggle.setFont(new Font(Font.MONOSPACED, Font.BOLD, 10));
        toggle.setToolTipText(tooltip);
        toggle.setFocusable(false);
        return toggle;
    }
    
    /**
     * Initializes the panel layout.
     */
    private void initializeLayout() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
        
        // Add components in order
        add(textField);
        add(regexToggle);
        add(caseToggle);
        add(invertToggle);
        
        // Set tooltips
        textField.setToolTipText("Enter " + fieldName.toLowerCase() + " pattern to filter requests");
    }
    
    // ==================== Public API ====================
    
    /**
     * Gets the current text value.
     * 
     * @return The text field value
     */
    public String getText() {
        return textField.getText().trim();
    }
    
    /**
     * Sets the text value.
     * 
     * @param text The text to set
     */
    public void setText(String text) {
        textField.setText(text != null ? text : "");
    }
    
    /**
     * Gets the regex mode state.
     * 
     * @return true if regex mode is enabled
     */
    public boolean isRegexMode() {
        return regexToggle.isSelected();
    }
    
    /**
     * Sets the regex mode state.
     * 
     * @param regexMode true to enable regex mode
     */
    public void setRegexMode(boolean regexMode) {
        regexToggle.setSelected(regexMode);
    }
    
    /**
     * Gets the case sensitive state.
     * 
     * @return true if case sensitive matching is enabled
     */
    public boolean isCaseSensitive() {
        return caseToggle.isSelected();
    }
    
    /**
     * Sets the case sensitive state.
     * 
     * @param caseSensitive true to enable case sensitive matching
     */
    public void setCaseSensitive(boolean caseSensitive) {
        caseToggle.setSelected(caseSensitive);
    }
    
    /**
     * Gets the invert match state.
     * 
     * @return true if invert matching is enabled
     */
    public boolean isInvertMatch() {
        return invertToggle.isSelected();
    }
    
    /**
     * Sets the invert match state.
     * 
     * @param invertMatch true to enable invert matching
     */
    public void setInvertMatch(boolean invertMatch) {
        invertToggle.setSelected(invertMatch);
    }
    
    /**
     * Clears all field values and resets toggles.
     */
    public void clear() {
        textField.setText("");
        regexToggle.setSelected(false);
        caseToggle.setSelected(false);
        invertToggle.setSelected(false);
    }
    
    /**
     * Checks if the field has any active content.
     * 
     * @return true if text field is not empty or any toggle is active
     */
    public boolean hasActiveContent() {
        return !getText().isEmpty() || 
               regexToggle.isSelected() || 
               caseToggle.isSelected() || 
               invertToggle.isSelected();
    }
    
    // ==================== Event Handling ====================
    
    /**
     * Adds an action listener to the text field.
     * 
     * @param listener The action listener
     */
    public void addTextFieldActionListener(ActionListener listener) {
        textField.addActionListener(listener);
    }
    
    /**
     * Adds an action listener to all toggle buttons.
     * 
     * @param listener The action listener
     */
    public void addToggleActionListener(ActionListener listener) {
        regexToggle.addActionListener(listener);
        caseToggle.addActionListener(listener);
        invertToggle.addActionListener(listener);
    }
    
    /**
     * Adds a document listener to the text field for real-time changes.
     * 
     * @param listener The document listener
     */
    public void addTextFieldDocumentListener(javax.swing.event.DocumentListener listener) {
        textField.getDocument().addDocumentListener(listener);
    }
    
    // ==================== Component Access ====================
    
    /**
     * Gets the text field component for advanced configuration.
     * 
     * @return The JTextField component
     */
    public JTextField getTextField() {
        return textField;
    }
    
    /**
     * Gets the regex toggle button.
     * 
     * @return The regex JToggleButton
     */
    public JToggleButton getRegexToggle() {
        return regexToggle;
    }
    
    /**
     * Gets the case sensitive toggle button.
     * 
     * @return The case sensitive JToggleButton
     */
    public JToggleButton getCaseToggle() {
        return caseToggle;
    }
    
    /**
     * Gets the invert match toggle button.
     * 
     * @return The invert match JToggleButton
     */
    public JToggleButton getInvertToggle() {
        return invertToggle;
    }
} 