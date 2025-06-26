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
 * modern UI styling with rounded input fields and proper spacing.
 * 
 * Features:
 * - Modern rounded input fields and dropdowns
 * - Host pattern text field with placeholder
 * - Method dropdown (All, GET, POST, PUT, DELETE, etc.)
 * - Status code dropdown (All, 2xx, 3xx, 4xx, 5xx, specific codes)
 * - Show all dropdown for additional filtering options
 * - Proper horizontal spacing and alignment
 * - Thread-safe filter criteria updates
 * - Event-driven filter change notifications
 * 
 * @author Harshit Shah
 */
public class FilterPanel extends JPanel {
    
    // Filter components
    private final JTextField hostField;
    private final JComboBox<String> methodComboBox;
    private final JComboBox<String> statusComboBox;
    private final JComboBox<String> showAllComboBox;
    private final JButton resetFiltersButton;
    
    // Filter change listeners
    private final List<FilterChangeListener> filterChangeListeners;
    
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
        hostField = SwingUtils.createModernTextField("Host filter (e.g., example.com)", 15);
        methodComboBox = SwingUtils.createModernComboBox(METHOD_OPTIONS, "All");
        statusComboBox = SwingUtils.createModernComboBox(STATUS_OPTIONS, "All");
        showAllComboBox = SwingUtils.createModernComboBox(SHOW_ALL_OPTIONS, "Show all");
        
        // Create reset button with existing icon
        ImageIcon resetIcon = loadResetIcon();
        resetFiltersButton = SwingUtils.createModernButton("Reset", "Reset all filters to default values", null);
        if (resetIcon != null) {
            resetFiltersButton.setIcon(resetIcon);
            resetFiltersButton.setHorizontalTextPosition(SwingConstants.RIGHT);
            resetFiltersButton.setIconTextGap(4);
        }
        
        initializeComponents();
        setupEventHandlers();
    }
    
    /**
     * Loads the reset icon from resources.
     * 
     * @return ImageIcon for the reset button, or null if not found
     */
    private ImageIcon loadResetIcon() {
        try {
            java.net.URL iconUrl = getClass().getResource("/icons/image.png");
            if (iconUrl != null) {
                ImageIcon originalIcon = new ImageIcon(iconUrl);
                // Scale the icon to 16x16 for button use
                Image scaledImage = originalIcon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImage);
            }
        } catch (Exception e) {
            System.err.println("Could not load reset icon: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Initializes the panel components and layout.
     */
    private void initializeComponents() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 2));
        setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        
        // Set placeholders and tooltips instead of labels
        hostField.setToolTipText("Enter host pattern to filter requests");
        SwingUtils.setPlaceholder(hostField, "Host");
        
        methodComboBox.setToolTipText("Filter by HTTP method");
        statusComboBox.setToolTipText("Filter by response status code");
        showAllComboBox.setToolTipText("Show all requests or filter by specific criteria");
        
        // Set compact component sizes for horizontal layout
        Dimension compactFieldSize = new Dimension(120, 26);
        Dimension compactComboSize = new Dimension(90, 26);
        
        hostField.setPreferredSize(compactFieldSize);
        methodComboBox.setPreferredSize(compactComboSize);
        statusComboBox.setPreferredSize(compactComboSize);
        showAllComboBox.setPreferredSize(new Dimension(110, 26));
        resetFiltersButton.setPreferredSize(new Dimension(85, 26));
        
        // Add components directly in horizontal layout
        add(hostField);
        add(methodComboBox);
        add(statusComboBox);
        add(showAllComboBox);
        add(resetFiltersButton);
        
        // Add subtle bottom border
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, SwingUtils.BORDER_COLOR),
            BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
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
        
        // Combo box change listeners - immediate (no typing involved)
        methodComboBox.addActionListener(e -> notifyFilterChange());
        statusComboBox.addActionListener(e -> notifyFilterChange());
        showAllComboBox.addActionListener(e -> notifyFilterChange());
        
        // Reset button listener
        resetFiltersButton.addActionListener(e -> clearFilters());
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
     * Gets the current filter criteria based on UI state.
     * 
     * @return The current FilterCriteria
     */
    public FilterCriteria getCurrentFilterCriteria() {
        FilterCriteria criteria = new FilterCriteria();
        
        // Set method filter
        String selectedMethod = (String) methodComboBox.getSelectedItem();
        criteria.setMethod(selectedMethod != null ? selectedMethod : "All");
        
        // Set status filter
        String selectedStatus = (String) statusComboBox.getSelectedItem();
        criteria.setStatusCode(selectedStatus != null ? selectedStatus : "All");
        
        // Set host pattern
        String hostPattern = hostField.getText().trim();
        criteria.setHostPattern(hostPattern);
        
        // Set show all option
        String showOption = (String) showAllComboBox.getSelectedItem();
        if ("With responses only".equals(showOption)) {
            criteria.setOnlyWithResponses(true);
        } else if ("Highlighted only".equals(showOption)) {
            criteria.setOnlyHighlighted(true);
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
            }
        });
    }
    
    /**
     * Clears all filters and resets to default state.
     * This method is thread-safe and can be called from any thread.
     */
    public void clearFilters() {
        SwingUtilities.invokeLater(() -> {
            boolean wasEnabled = isEnabled();
            setEnabled(false);
            
            try {
                hostField.setText("");
                methodComboBox.setSelectedItem("All");
                statusComboBox.setSelectedItem("All");
                showAllComboBox.setSelectedItem("Show all");
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