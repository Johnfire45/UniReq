package com.burp.unireq.ui.components;

import com.burp.unireq.model.FilterCriteria;
import com.burp.unireq.utils.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * UniReqFilterDialog - Advanced modal filter dialog for HTTP requests
 * 
 * Provides a comprehensive Burp-style filter interface with tabbed layout
 * for different filter categories. Maps all settings to/from FilterCriteria.
 * 
 * @author Harshit Shah
 */
public class UniReqFilterDialog extends JDialog {
    
    // Dialog result
    private FilterCriteria result = null;
    private boolean wasApplied = false;
    
    // Original criteria for revert functionality
    private FilterCriteria originalCriteria;
    
    // UI Components - Method Filter
    private JCheckBox[] methodCheckboxes;
    private static final String[] HTTP_METHODS = {
        "GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS", "TRACE"
    };
    
    // UI Components - Status Filter
    private JCheckBox[] statusCheckboxes;
    private static final String[] STATUS_RANGES = {"2xx", "3xx", "4xx", "5xx"};
    private static final Integer[] STATUS_PREFIXES = {2, 3, 4, 5};
    
    // UI Components - MIME Type Filter
    private JCheckBox[] mimeCheckboxes;
    private static final String[] MIME_TYPES = {
        "text/html", "application/json", "application/xml", "text/css", 
        "application/javascript", "image/png", "image/jpeg", "image/gif"
    };
    
    // UI Components - Pattern Filters
    private FilterFieldPanel hostFilterPanel;
    private FilterFieldPanel pathFilterPanel;
    
    // UI Components - Extension Filters
    private JTextField includeExtensionsField;
    private JTextField excludeExtensionsField;
    
    // UI Components - Options
    private JCheckBox requireResponseCheckbox;
    private JCheckBox onlyInScopeCheckbox;
    
    // UI Components - Actions
    private JButton applyButton;
    private JButton cancelButton;
    private JButton showAllButton;
    private JButton revertButton;
    
    /**
     * Constructor creates the filter dialog.
     * 
     * @param parent The parent frame
     * @param initialCriteria The initial filter criteria (can be null)
     */
    public UniReqFilterDialog(Frame parent, FilterCriteria initialCriteria) {
        super(parent, "Advanced HTTP Request Filters", true);
        
        this.originalCriteria = initialCriteria != null ? initialCriteria.copy() : new FilterCriteria();
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        
        // Load initial criteria
        if (initialCriteria != null) {
            loadCriteria(initialCriteria);
        }
        
        // Configure dialog
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);
        pack();
        setLocationRelativeTo(parent);
    }
    
    /**
     * Initializes all UI components.
     */
    private void initializeComponents() {
        // Method checkboxes
        methodCheckboxes = new JCheckBox[HTTP_METHODS.length];
        for (int i = 0; i < HTTP_METHODS.length; i++) {
            methodCheckboxes[i] = new JCheckBox(HTTP_METHODS[i]);
        }
        
        // Status checkboxes
        statusCheckboxes = new JCheckBox[STATUS_RANGES.length];
        for (int i = 0; i < STATUS_RANGES.length; i++) {
            statusCheckboxes[i] = new JCheckBox(STATUS_RANGES[i]);
        }
        
        // MIME type checkboxes
        mimeCheckboxes = new JCheckBox[MIME_TYPES.length];
        for (int i = 0; i < MIME_TYPES.length; i++) {
            mimeCheckboxes[i] = new JCheckBox(MIME_TYPES[i]);
        }
        
        // Pattern filter panels
        hostFilterPanel = new FilterFieldPanel("Host", 200);
        pathFilterPanel = new FilterFieldPanel("Path", 200);
        
        // Extension fields
        includeExtensionsField = SwingUtils.createModernTextField("e.g., php,jsp,asp", 150);
        excludeExtensionsField = SwingUtils.createModernTextField("e.g., css,js,png", 150);
        
        // Option checkboxes
        requireResponseCheckbox = new JCheckBox("Hide items without responses");
        onlyInScopeCheckbox = new JCheckBox("Show only in-scope items");
        
        // Action buttons
        applyButton = SwingUtils.createModernButton("Apply & Close", "Apply filters and close dialog", null);
        cancelButton = SwingUtils.createModernButton("Cancel", "Cancel changes and close dialog", null);
        showAllButton = SwingUtils.createModernButton("Show All", "Clear all filters", null);
        revertButton = SwingUtils.createModernButton("Revert", "Revert to original settings", null);
    }
    
    /**
     * Sets up the dialog layout using tabs for organization.
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Create tabbed pane for different filter categories
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Request Methods Tab
        tabbedPane.addTab("Methods", createMethodPanel());
        
        // Status Codes Tab
        tabbedPane.addTab("Status", createStatusPanel());
        
        // MIME Types Tab
        tabbedPane.addTab("MIME Types", createMimeTypePanel());
        
        // Patterns Tab (Host/Path)
        tabbedPane.addTab("Patterns", createPatternPanel());
        
        // Extensions Tab
        tabbedPane.addTab("Extensions", createExtensionPanel());
        
        // Options Tab
        tabbedPane.addTab("Options", createOptionsPanel());
        
        // Add tabbed pane to center
        add(tabbedPane, BorderLayout.CENTER);
        
        // Add button panel to bottom
        add(createButtonPanel(), BorderLayout.SOUTH);
    }
    
    /**
     * Creates the HTTP methods filter panel.
     */
    private JPanel createMethodPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 5, 2, 5);
        
        // Title
        JLabel titleLabel = new JLabel("Show only these HTTP methods:");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 4;
        panel.add(titleLabel, gbc);
        
        // Method checkboxes in grid
        gbc.gridwidth = 1;
        for (int i = 0; i < methodCheckboxes.length; i++) {
            gbc.gridx = i % 4;
            gbc.gridy = 1 + (i / 4);
            panel.add(methodCheckboxes[i], gbc);
        }
        
        return panel;
    }
    
    /**
     * Creates the status codes filter panel.
     */
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 5, 2, 5);
        
        // Title
        JLabel titleLabel = new JLabel("Show only these status code ranges:");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 4;
        panel.add(titleLabel, gbc);
        
        // Status checkboxes
        gbc.gridwidth = 1;
        for (int i = 0; i < statusCheckboxes.length; i++) {
            gbc.gridx = i;
            gbc.gridy = 1;
            panel.add(statusCheckboxes[i], gbc);
        }
        
        return panel;
    }
    
    /**
     * Creates the MIME types filter panel.
     */
    private JPanel createMimeTypePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 5, 2, 5);
        
        // Title
        JLabel titleLabel = new JLabel("Show only these MIME types:");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);
        
        // MIME type checkboxes in two columns
        gbc.gridwidth = 1;
        for (int i = 0; i < mimeCheckboxes.length; i++) {
            gbc.gridx = i % 2;
            gbc.gridy = 1 + (i / 2);
            panel.add(mimeCheckboxes[i], gbc);
        }
        
        return panel;
    }
    
    /**
     * Creates the pattern filters panel (Host/Path).
     */
    private JPanel createPatternPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Host filter
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Host:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(hostFilterPanel, gbc);
        
        // Path filter
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        panel.add(new JLabel("Path:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(pathFilterPanel, gbc);
        
        return panel;
    }
    
    /**
     * Creates the file extensions filter panel.
     */
    private JPanel createExtensionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Include extensions
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Show only these extensions:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(includeExtensionsField, gbc);
        
        // Exclude extensions
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        panel.add(new JLabel("Hide these extensions:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(excludeExtensionsField, gbc);
        
        // Help text
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JLabel helpLabel = new JLabel("<html><i>Separate multiple extensions with commas (e.g., php,jsp,asp)</i></html>");
        helpLabel.setForeground(Color.GRAY);
        panel.add(helpLabel, gbc);
        
        return panel;
    }
    
    /**
     * Creates the options panel.
     */
    private JPanel createOptionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(requireResponseCheckbox, gbc);
        
        gbc.gridy = 1;
        panel.add(onlyInScopeCheckbox, gbc);
        
        return panel;
    }
    
    /**
     * Creates the bottom button panel.
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
        
        panel.add(showAllButton);
        panel.add(revertButton);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(cancelButton);
        panel.add(applyButton);
        
        return panel;
    }
    
    /**
     * Sets up event handlers for all components.
     */
    private void setupEventHandlers() {
        applyButton.addActionListener(e -> {
            result = buildCriteriaFromUI();
            wasApplied = true;
            dispose();
        });
        
        cancelButton.addActionListener(e -> {
            result = null;
            wasApplied = false;
            dispose();
        });
        
        showAllButton.addActionListener(e -> clearAllFilters());
        revertButton.addActionListener(e -> loadCriteria(originalCriteria));
    }
    
    /**
     * Builds FilterCriteria from current UI state.
     */
    private FilterCriteria buildCriteriaFromUI() {
        FilterCriteria criteria = new FilterCriteria();
        
        // Method filter
        Set<String> allowedMethods = new HashSet<>();
        for (int i = 0; i < methodCheckboxes.length; i++) {
            if (methodCheckboxes[i].isSelected()) {
                allowedMethods.add(HTTP_METHODS[i]);
            }
        }
        criteria.setAllowedMethods(allowedMethods);
        
        // Status filter
        Set<Integer> allowedStatusPrefixes = new HashSet<>();
        for (int i = 0; i < statusCheckboxes.length; i++) {
            if (statusCheckboxes[i].isSelected()) {
                allowedStatusPrefixes.add(STATUS_PREFIXES[i]);
            }
        }
        criteria.setAllowedStatusPrefixes(allowedStatusPrefixes);
        
        // MIME type filter
        Set<String> allowedMimeTypes = new HashSet<>();
        for (int i = 0; i < mimeCheckboxes.length; i++) {
            if (mimeCheckboxes[i].isSelected()) {
                allowedMimeTypes.add(MIME_TYPES[i]);
            }
        }
        criteria.setAllowedMimeTypes(allowedMimeTypes);
        
        // Host pattern
        criteria.setHostPattern(hostFilterPanel.getText());
        criteria.setRegexMode(hostFilterPanel.isRegexMode());
        criteria.setCaseSensitive(hostFilterPanel.isCaseSensitive());
        criteria.setInvertHostFilter(hostFilterPanel.isInvertMatch());
        
        // Path pattern
        criteria.setPathPattern(pathFilterPanel.getText());
        
        // Extensions
        criteria.setIncludedExtensions(parseExtensions(includeExtensionsField.getText()));
        criteria.setExcludedExtensions(parseExtensions(excludeExtensionsField.getText()));
        
        // Options
        criteria.setRequireResponse(requireResponseCheckbox.isSelected());
        criteria.setOnlyInScope(onlyInScopeCheckbox.isSelected());
        
        return criteria;
    }
    
    /**
     * Loads FilterCriteria into UI components.
     */
    private void loadCriteria(FilterCriteria criteria) {
        if (criteria == null) {
            clearAllFilters();
            return;
        }
        
        // Method filter
        Set<String> allowedMethods = criteria.getAllowedMethods();
        for (int i = 0; i < methodCheckboxes.length; i++) {
            methodCheckboxes[i].setSelected(allowedMethods.contains(HTTP_METHODS[i]));
        }
        
        // Status filter
        Set<Integer> allowedStatusPrefixes = criteria.getAllowedStatusPrefixes();
        for (int i = 0; i < statusCheckboxes.length; i++) {
            statusCheckboxes[i].setSelected(allowedStatusPrefixes.contains(STATUS_PREFIXES[i]));
        }
        
        // MIME type filter
        Set<String> allowedMimeTypes = criteria.getAllowedMimeTypes();
        for (int i = 0; i < mimeCheckboxes.length; i++) {
            mimeCheckboxes[i].setSelected(allowedMimeTypes.contains(MIME_TYPES[i]));
        }
        
        // Host pattern
        hostFilterPanel.setText(criteria.getHostPattern());
        hostFilterPanel.setRegexMode(criteria.isRegexMode());
        hostFilterPanel.setCaseSensitive(criteria.isCaseSensitive());
        hostFilterPanel.setInvertMatch(criteria.isInvertHostFilter());
        
        // Path pattern
        pathFilterPanel.setText(criteria.getPathPattern());
        pathFilterPanel.setRegexMode(criteria.isRegexMode());
        pathFilterPanel.setCaseSensitive(criteria.isCaseSensitive());
        
        // Extensions
        includeExtensionsField.setText(formatExtensions(criteria.getIncludedExtensions()));
        excludeExtensionsField.setText(formatExtensions(criteria.getExcludedExtensions()));
        
        // Options
        requireResponseCheckbox.setSelected(criteria.isRequireResponse());
        onlyInScopeCheckbox.setSelected(criteria.isOnlyInScope());
    }
    
    /**
     * Clears all filter settings.
     */
    private void clearAllFilters() {
        // Clear method checkboxes
        for (JCheckBox checkbox : methodCheckboxes) {
            checkbox.setSelected(false);
        }
        
        // Clear status checkboxes
        for (JCheckBox checkbox : statusCheckboxes) {
            checkbox.setSelected(false);
        }
        
        // Clear MIME type checkboxes
        for (JCheckBox checkbox : mimeCheckboxes) {
            checkbox.setSelected(false);
        }
        
        // Clear pattern panels
        hostFilterPanel.clear();
        pathFilterPanel.clear();
        
        // Clear extension fields
        includeExtensionsField.setText("");
        excludeExtensionsField.setText("");
        
        // Clear option checkboxes
        requireResponseCheckbox.setSelected(false);
        onlyInScopeCheckbox.setSelected(false);
    }
    
    /**
     * Parses comma-separated extensions into a Set.
     */
    private Set<String> parseExtensions(String extensionsText) {
        Set<String> extensions = new HashSet<>();
        if (extensionsText != null && !extensionsText.trim().isEmpty()) {
            String[] parts = extensionsText.split(",");
            for (String part : parts) {
                String ext = part.trim().toLowerCase();
                if (!ext.isEmpty()) {
                    extensions.add(ext);
                }
            }
        }
        return extensions;
    }
    
    /**
     * Formats a Set of extensions into comma-separated string.
     */
    private String formatExtensions(Set<String> extensions) {
        if (extensions == null || extensions.isEmpty()) {
            return "";
        }
        return String.join(", ", extensions);
    }
    
    // ==================== Public API ====================
    
    /**
     * Shows the dialog and returns the result.
     * 
     * @return The FilterCriteria if applied, null if cancelled
     */
    public FilterCriteria showDialog() {
        setVisible(true);
        return wasApplied ? result : null;
    }
    
    /**
     * Gets whether the dialog was applied (not cancelled).
     * 
     * @return true if the dialog was applied
     */
    public boolean wasApplied() {
        return wasApplied;
    }
} 