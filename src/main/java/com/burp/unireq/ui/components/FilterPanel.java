package com.burp.unireq.ui.components;

import com.burp.unireq.model.FilterCriteria;
import com.burp.unireq.utils.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FilterPanel - Modern filter controls component for UniReq extension
 * 
 * This component provides filter controls for HTTP requests including
 * Host pattern, Method selection, and Status code filtering. It features
 * modern UI styling with proper layout grouping and labeled controls.
 * 
 * Features:
 * - Logical grouping of related controls
 * - Labeled checkboxes instead of cryptic icon buttons
 * - Host pattern text field with associated options
 * - Method and status dropdowns with action buttons
 * - Professional GridBagLayout for responsive design
 * - Thread-safe filter criteria updates
 * - Event-driven filter change notifications
 * 
 * Layout:
 * [Host: _______________] [☐ Regex] [☐ Case] [☐ Invert] | [Method ▼] [Status ▼] [Show All ▼] [Reset] [Advanced Filters]
 * 
 * @author Harshit Shah
 */
public class FilterPanel extends JPanel {
    
    // Filter components
    private final JTextField hostField;
    private final JCheckBox regexCheckBox;
    private final JCheckBox caseCheckBox;
    private final JCheckBox invertCheckBox;
    private final JComboBox<String> methodComboBox;
    private final JComboBox<String> statusComboBox;
    private final JComboBox<String> showAllComboBox;
    private final JButton resetFiltersButton;
    private final JButton advancedFiltersButton;
    
    // Filter change listeners
    private final List<FilterChangeListener> filterChangeListeners;
    
    // Advanced filter state management
    private FilterCriteria advancedCriteria = null;
    
    // Performance optimization: Debounced filter change mechanism
    private final Timer filterDebounceTimer;
    private final AtomicBoolean filterChangePending = new AtomicBoolean(false);
    private static final int FILTER_DEBOUNCE_DELAY_MS = 300; // 300ms debounce delay for typing
    
    // Method options
    private static final String[] METHOD_OPTIONS = {
        "All", "GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"
    };
    
    // Status code options
    private static final String[] STATUS_OPTIONS = {
        "All", "2xx", "3xx", "4xx", "5xx", "200", "302", "404", "500"
    };
    
    // Show all options
    private static final String[] SHOW_ALL_OPTIONS = {
        "Show all", "With responses only", "Highlighted only"
    };
    
    /**
     * Interface for listening to filter changes.
     */
    public interface FilterChangeListener {
        /**
         * Called when filter criteria changes.
         * 
         * @param criteria The new filter criteria
         */
        void onFilterChanged(FilterCriteria criteria);
    }
    
    /**
     * Constructor initializes the filter panel with modern styling.
     */
    public FilterPanel() {
        filterChangeListeners = new ArrayList<>();
        
        // Initialize debounced filter change timer
        this.filterDebounceTimer = new Timer(FILTER_DEBOUNCE_DELAY_MS, e -> performDebouncedFilterChange());
        this.filterDebounceTimer.setRepeats(false); // Only fire once per trigger
        
        // Create filter components with modern styling
        hostField = SwingUtils.createModernTextField("Host filter (e.g., example.com)", 20);
        
        // Create labeled checkboxes instead of cryptic toggle buttons
        regexCheckBox = createLabeledCheckBox("Regex", "Use regex patterns in host filter");
        caseCheckBox = createLabeledCheckBox("Case", "Case-sensitive host matching");
        invertCheckBox = createLabeledCheckBox("Invert", "Exclude matching hosts (invert filter)");
        
        methodComboBox = SwingUtils.createModernComboBox(METHOD_OPTIONS, "All");
        statusComboBox = SwingUtils.createModernComboBox(STATUS_OPTIONS, "All");
        showAllComboBox = SwingUtils.createModernComboBox(SHOW_ALL_OPTIONS, "Show all");
        
        // Create action buttons
        resetFiltersButton = SwingUtils.createModernButton("Reset", "Reset all filters to default values", null);
        advancedFiltersButton = SwingUtils.createModernButton("Advanced Filters", "Open advanced filter dialog", null);
        
        initializeComponents();
        setupEventHandlers();
    }
    
    /**
     * Creates a labeled checkbox with consistent styling.
     * 
     * @param text The label text
     * @param tooltip The tooltip text
     * @return A configured JCheckBox
     */
    private JCheckBox createLabeledCheckBox(String text, String tooltip) {
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.setToolTipText(tooltip);
        checkBox.setFocusable(false);
        checkBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        return checkBox;
    }
    
    /**
     * Initializes the panel components and layout using GridBagLayout for professional appearance.
     */
    private void initializeComponents() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, SwingUtils.BORDER_COLOR),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        
        // Host Filter Group
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel hostLabel = new JLabel("Host:");
        hostLabel.setFont(hostLabel.getFont().deriveFont(Font.BOLD));
        add(hostLabel, gbc);
        
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        hostField.setPreferredSize(new Dimension(200, 26));
        hostField.setToolTipText("Enter host pattern to filter requests (supports regex when enabled)");
        add(hostField, gbc);
        
        gbc.gridx = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(2, 3, 2, 3); // Tighter spacing for checkbox group
        add(regexCheckBox, gbc);
        
        gbc.gridx = 3;
        add(caseCheckBox, gbc);
        
        gbc.gridx = 4;
        add(invertCheckBox, gbc);
        
        // Filter Controls Group (removed pipe separator, using spacing instead)
        gbc.gridx = 5; gbc.insets = new Insets(2, 12, 2, 5);
        methodComboBox.setPreferredSize(new Dimension(85, 26));
        methodComboBox.setToolTipText("Filter by HTTP method");
        add(methodComboBox, gbc);
        
        gbc.gridx = 6; gbc.insets = new Insets(2, 5, 2, 5);
        statusComboBox.setPreferredSize(new Dimension(85, 26));
        statusComboBox.setToolTipText("Filter by response status code");
        add(statusComboBox, gbc);
        
        gbc.gridx = 7;
        showAllComboBox.setPreferredSize(new Dimension(130, 26));
        showAllComboBox.setToolTipText("Show all requests or filter by specific criteria");
        add(showAllComboBox, gbc);
        
        // Action Buttons Group
        gbc.gridx = 8;
        resetFiltersButton.setPreferredSize(new Dimension(70, 26));
        add(resetFiltersButton, gbc);
        
        gbc.gridx = 9;
        advancedFiltersButton.setPreferredSize(new Dimension(110, 26));
        add(advancedFiltersButton, gbc);
        
        // Add flexible space at the end to push everything left
        gbc.gridx = 10; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        add(Box.createHorizontalGlue(), gbc);
    }
    
    /**
     * Sets up event handlers for filter components.
     */
    private void setupEventHandlers() {
        // Host field change listener - debounced for performance
        hostField.addActionListener(e -> scheduleFilterChange());
        hostField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { scheduleFilterChange(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { scheduleFilterChange(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { scheduleFilterChange(); }
        });
        
        // Checkbox change listeners - immediate
        regexCheckBox.addActionListener(e -> {
            notifyFilterChange();
            updateCheckboxStyles();
        });
        caseCheckBox.addActionListener(e -> {
            notifyFilterChange();
            updateCheckboxStyles();
        });
        invertCheckBox.addActionListener(e -> {
            notifyFilterChange();
            updateCheckboxStyles();
        });
        
        // Combo box change listeners - immediate (no typing involved)
        methodComboBox.addActionListener(e -> notifyFilterChange());
        statusComboBox.addActionListener(e -> notifyFilterChange());
        showAllComboBox.addActionListener(e -> notifyFilterChange());
        
        // Reset button listener
        resetFiltersButton.addActionListener(e -> clearFilters());
        
        // Advanced filters button listener
        advancedFiltersButton.addActionListener(e -> openAdvancedFilterDialog());
        
        // Initialize checkbox styles
        updateCheckboxStyles();
    }
    
    /**
     * Updates checkbox visual styles to highlight active states.
     * This provides visual feedback about which filter options are enabled.
     */
    private void updateCheckboxStyles() {
        SwingUtilities.invokeLater(() -> {
            // Update regex checkbox
            if (regexCheckBox.isSelected()) {
                regexCheckBox.setFont(regexCheckBox.getFont().deriveFont(Font.BOLD));
                regexCheckBox.setForeground(new Color(34, 139, 34)); // Forest green for active
            } else {
                regexCheckBox.setFont(regexCheckBox.getFont().deriveFont(Font.PLAIN));
                regexCheckBox.setForeground(UIManager.getColor("Label.foreground"));
            }
            
            // Update case checkbox
            if (caseCheckBox.isSelected()) {
                caseCheckBox.setFont(caseCheckBox.getFont().deriveFont(Font.BOLD));
                caseCheckBox.setForeground(new Color(34, 139, 34)); // Forest green for active
            } else {
                caseCheckBox.setFont(caseCheckBox.getFont().deriveFont(Font.PLAIN));
                caseCheckBox.setForeground(UIManager.getColor("Label.foreground"));
            }
            
            // Update invert checkbox
            if (invertCheckBox.isSelected()) {
                invertCheckBox.setFont(invertCheckBox.getFont().deriveFont(Font.BOLD));
                invertCheckBox.setForeground(new Color(184, 134, 11)); // Amber for invert (different action)
            } else {
                invertCheckBox.setFont(invertCheckBox.getFont().deriveFont(Font.PLAIN));
                invertCheckBox.setForeground(UIManager.getColor("Label.foreground"));
            }
        });
    }
    
    /**
     * Schedules a debounced filter change notification to prevent excessive updates.
     * This method can be called frequently (e.g., on every keystroke) - it will only
     * trigger an actual filter change after the debounce delay has passed.
     * 
     * Performance: Prevents UI lag during fast typing in filter fields.
     */
    private void scheduleFilterChange() {
        if (filterChangePending.compareAndSet(false, true)) {
            // Only restart timer if no filter change is currently pending
            SwingUtilities.invokeLater(() -> {
                if (filterDebounceTimer.isRunning()) {
                    filterDebounceTimer.restart();
                } else {
                    filterDebounceTimer.start();
                }
            });
        } else {
            // Filter change already pending, just restart the timer to extend the delay
            SwingUtilities.invokeLater(() -> filterDebounceTimer.restart());
        }
    }
    
    /**
     * Performs the actual debounced filter change notification.
     * This method is called by the timer after the debounce delay.
     */
    private void performDebouncedFilterChange() {
        SwingUtilities.invokeLater(() -> {
            try {
                filterChangePending.set(false); // Reset the pending flag
                notifyFilterChange(); // Perform the actual notification
            } catch (Exception e) {
                System.err.println("Error in debounced filter change: " + e.getMessage());
            }
        });
    }
    
    /**
     * Notifies all listeners of filter changes.
     * 
     * Performance: This method should not be called directly for real-time text input.
     * Use scheduleFilterChange() instead to prevent UI lag during typing.
     */
    private void notifyFilterChange() {
        SwingUtilities.invokeLater(() -> {
            FilterCriteria criteria = getCurrentFilterCriteria();
            for (FilterChangeListener listener : filterChangeListeners) {
                try {
                    listener.onFilterChanged(criteria);
                } catch (Exception e) {
                    // Log error silently
                    System.err.println("Error notifying filter change: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Opens the advanced filter dialog and processes the result.
     * 
     * This method:
     * 1. Gets current filter criteria (basic + advanced)
     * 2. Launches UniReqFilterDialog with current state
     * 3. If user applies changes, updates the UI and notifies listeners
     * 4. Updates visual indicators for active advanced filters
     */
    private void openAdvancedFilterDialog() {
        try {
            // Get current criteria including both basic and advanced settings
            FilterCriteria currentCriteria = getCurrentFilterCriteria();
            
            // Find parent window for modal dialog
            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            Frame parentFrame = null;
            if (parentWindow instanceof Frame) {
                parentFrame = (Frame) parentWindow;
            }
            
            // Launch advanced filter dialog
            UniReqFilterDialog dialog = new UniReqFilterDialog(parentFrame, currentCriteria);
            FilterCriteria result = dialog.showDialog();
            
            // Process dialog result
            if (result != null && dialog.wasApplied()) {
                // Apply the returned criteria to the UI
                applyAdvancedFilterCriteria(result);
                
                // Notify listeners of the filter change
                notifyFilterChange();
            }
            
        } catch (Exception e) {
            // Log error and show user-friendly message
            System.err.println("Error opening advanced filter dialog: " + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "Error opening advanced filter dialog: " + e.getMessage(),
                "Filter Dialog Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Applies advanced filter criteria to the UI and internal state.
     * 
     * This method:
     * 1. Updates basic UI components (host, method, status) from criteria
     * 2. Stores advanced criteria for FilterEngine consumption
     * 3. Updates visual indicators to show active advanced filters
     * 
     * @param criteria The FilterCriteria returned from the advanced dialog
     */
    private void applyAdvancedFilterCriteria(FilterCriteria criteria) {
        if (criteria == null) {
            // Clear advanced criteria if null
            advancedCriteria = null;
            updateAdvancedFilterIndicator();
            return;
        }
        
        // Store the complete advanced criteria
        advancedCriteria = criteria.copy();
        
        // Update basic UI components from the criteria
        // This ensures the basic filters stay in sync with advanced dialog
        SwingUtilities.invokeLater(() -> {
            // Temporarily disable event firing to prevent recursion
            boolean wasEnabled = isEnabled();
            setEnabled(false);
            
            try {
                // Update basic filter components
                hostField.setText(criteria.getHostPattern());
                regexCheckBox.setSelected(criteria.isRegexMode());
                caseCheckBox.setSelected(criteria.isCaseSensitive());
                invertCheckBox.setSelected(criteria.isInvertHostFilter());
                
                methodComboBox.setSelectedItem(criteria.getMethod());
                statusComboBox.setSelectedItem(criteria.getStatusCode());
                
                // Update show all option based on criteria
                if (criteria.isOnlyWithResponses()) {
                    showAllComboBox.setSelectedItem("With responses only");
                } else if (criteria.isOnlyHighlighted()) {
                    showAllComboBox.setSelectedItem("Highlighted only");
                } else {
                    showAllComboBox.setSelectedItem("Show all");
                }
                
            } finally {
                setEnabled(wasEnabled);
            }
            
            // Update visual indicators for advanced filters and checkboxes
            updateAdvancedFilterIndicator();
            updateCheckboxStyles();
        });
    }
    
    /**
     * Updates the visual indicator on the Advanced button to show when advanced filters are active.
     * 
     * Changes button text from "Advanced Filters" to "Advanced*" and makes it bold
     * when advanced filter criteria are present and active.
     */
    private void updateAdvancedFilterIndicator() {
        SwingUtilities.invokeLater(() -> {
            boolean hasAdvanced = hasActiveAdvancedFilters();
            
            if (hasAdvanced) {
                advancedFiltersButton.setText("Advanced*");
                advancedFiltersButton.setFont(advancedFiltersButton.getFont().deriveFont(Font.BOLD));
                advancedFiltersButton.setToolTipText("Advanced filters are active - click to modify");
            } else {
                advancedFiltersButton.setText("Advanced Filters");
                advancedFiltersButton.setFont(advancedFiltersButton.getFont().deriveFont(Font.PLAIN));
                advancedFiltersButton.setToolTipText("Open advanced filter dialog");
            }
        });
    }
    
    /**
     * Checks if there are any active advanced filter criteria.
     * 
     * @return true if advanced filters are configured and active
     */
    private boolean hasActiveAdvancedFilters() {
        if (advancedCriteria == null) {
            return false;
        }
        
        // Check if any advanced filter fields have non-default values
        return !advancedCriteria.getAllowedMethods().isEmpty() ||
               !advancedCriteria.getAllowedMimeTypes().isEmpty() ||
               !advancedCriteria.getIncludedExtensions().isEmpty() ||
               !advancedCriteria.getExcludedExtensions().isEmpty() ||
               !advancedCriteria.getAllowedStatusPrefixes().isEmpty() ||
               advancedCriteria.isRequireResponse() ||
               advancedCriteria.isOnlyInScope();
    }
    
    /**
     * Gets the current filter criteria based on UI state.
     * 
     * This method merges basic filter settings from the UI with any stored
     * advanced filter criteria to provide a complete FilterCriteria object
     * for the FilterEngine.
     * 
     * @return The current FilterCriteria (basic + advanced merged)
     */
    public FilterCriteria getCurrentFilterCriteria() {
        FilterCriteria criteria = new FilterCriteria();
        
        // Set basic filter settings from UI components
        String selectedMethod = (String) methodComboBox.getSelectedItem();
        criteria.setMethod(selectedMethod != null ? selectedMethod : "All");
        
        String selectedStatus = (String) statusComboBox.getSelectedItem();
        criteria.setStatusCode(selectedStatus != null ? selectedStatus : "All");
        
        String hostPattern = hostField.getText().trim();
        criteria.setHostPattern(hostPattern);
        
        // Set toggle states for pattern filtering
        criteria.setRegexMode(regexCheckBox.isSelected());
        criteria.setCaseSensitive(caseCheckBox.isSelected());
        criteria.setInvertHostFilter(invertCheckBox.isSelected());
        
        // Set show all option
        String showOption = (String) showAllComboBox.getSelectedItem();
        if ("With responses only".equals(showOption)) {
            criteria.setOnlyWithResponses(true);
        } else if ("Highlighted only".equals(showOption)) {
            criteria.setOnlyHighlighted(true);
        }
        
        // Merge with stored advanced criteria if available
        if (advancedCriteria != null) {
            // Advanced method filtering (overrides basic method if set)
            if (!advancedCriteria.getAllowedMethods().isEmpty()) {
                criteria.setAllowedMethods(advancedCriteria.getAllowedMethods());
            }
            
            // Advanced status filtering (overrides basic status if set)
            if (!advancedCriteria.getAllowedStatusPrefixes().isEmpty()) {
                criteria.setAllowedStatusPrefixes(advancedCriteria.getAllowedStatusPrefixes());
            }
            
            // Advanced MIME type filtering
            criteria.setAllowedMimeTypes(advancedCriteria.getAllowedMimeTypes());
            
            // File extension filtering
            criteria.setIncludedExtensions(advancedCriteria.getIncludedExtensions());
            criteria.setExcludedExtensions(advancedCriteria.getExcludedExtensions());
            
            // Advanced options (merge with basic options)
            criteria.setRequireResponse(advancedCriteria.isRequireResponse() || criteria.isOnlyWithResponses());
            criteria.setOnlyInScope(advancedCriteria.isOnlyInScope());
            
            // Path pattern from advanced dialog (if set)
            if (!advancedCriteria.getPathPattern().isEmpty()) {
                criteria.setPathPattern(advancedCriteria.getPathPattern());
            }
        }
        
        return criteria;
    }
    
    /**
     * Sets the filter criteria and updates the UI.
     * This method is thread-safe and can be called from any thread.
     * 
     * @param criteria The filter criteria to set
     */
    public void setFilterCriteria(FilterCriteria criteria) {
        SwingUtilities.invokeLater(() -> {
            if (criteria == null) {
                clearFilters();
                return;
            }
            
            // Update UI components without triggering events
            boolean wasEnabled = isEnabled();
            setEnabled(false);
            
            try {
                hostField.setText(criteria.getHostPattern());
                
                // Set toggle states
                regexCheckBox.setSelected(criteria.isRegexMode());
                caseCheckBox.setSelected(criteria.isCaseSensitive());
                invertCheckBox.setSelected(criteria.isInvertHostFilter());
                
                methodComboBox.setSelectedItem(criteria.getMethod());
                statusComboBox.setSelectedItem(criteria.getStatusCode());
                
                // Set show all option
                if (criteria.isOnlyWithResponses()) {
                    showAllComboBox.setSelectedItem("With responses only");
                } else if (criteria.isOnlyHighlighted()) {
                    showAllComboBox.setSelectedItem("Highlighted only");
                } else {
                    showAllComboBox.setSelectedItem("Show all");
                }
            } finally {
                setEnabled(wasEnabled);
                // Update checkbox styles after setting values
                updateCheckboxStyles();
            }
        });
    }
    
    /**
     * Clears all filters and resets to default state.
     * This method clears both basic UI filters and stored advanced criteria.
     * This method is thread-safe and can be called from any thread.
     */
    public void clearFilters() {
        SwingUtilities.invokeLater(() -> {
            boolean wasEnabled = isEnabled();
            setEnabled(false);
            
            try {
                // Clear basic UI components
                hostField.setText("");
                
                // Reset toggle states
                regexCheckBox.setSelected(false);
                caseCheckBox.setSelected(false);
                invertCheckBox.setSelected(false);
                
                methodComboBox.setSelectedItem("All");
                statusComboBox.setSelectedItem("All");
                showAllComboBox.setSelectedItem("Show all");
                
                // Clear stored advanced criteria
                advancedCriteria = null;
                
                // Update visual indicators
                updateAdvancedFilterIndicator();
                updateCheckboxStyles();
                
            } finally {
                setEnabled(wasEnabled);
                notifyFilterChange();
            }
        });
    }
    
    /**
     * Adds a filter change listener.
     * 
     * @param listener The listener to add
     */
    public void addFilterChangeListener(FilterChangeListener listener) {
        if (listener != null) {
            filterChangeListeners.add(listener);
        }
    }
    
    /**
     * Removes a filter change listener.
     * 
     * @param listener The listener to remove
     */
    public void removeFilterChangeListener(FilterChangeListener listener) {
        filterChangeListeners.remove(listener);
    }
    
    /**
     * Checks if any filters are currently active.
     * 
     * @return true if any filter is active
     */
    public boolean hasActiveFilters() {
        return getCurrentFilterCriteria().hasActiveFilters();
    }
    
    /**
     * Gets the host field component.
     * 
     * @return The host text field
     */
    public JTextField getHostField() {
        return hostField;
    }
    
    /**
     * Gets the method combo box component.
     * 
     * @return The method combo box
     */
    public JComboBox<String> getMethodComboBox() {
        return methodComboBox;
    }
    
    /**
     * Gets the status combo box component.
     * 
     * @return The status combo box
     */
    public JComboBox<String> getStatusComboBox() {
        return statusComboBox;
    }
    
    /**
     * Gets the show all combo box component.
     * 
     * @return The show all combo box
     */
    public JComboBox<String> getShowAllComboBox() {
        return showAllComboBox;
    }
    
    /**
     * Gets the reset filters button component.
     * 
     * @return The reset filters button
     */
    public JButton getResetFiltersButton() {
        return resetFiltersButton;
    }
} 