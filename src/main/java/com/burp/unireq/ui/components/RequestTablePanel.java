package com.burp.unireq.ui.components;

import com.burp.unireq.model.FilterCriteria;
import com.burp.unireq.model.RequestResponseEntry;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowSorter;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * RequestTablePanel - HTTP request table component with filters for UniReq extension
 * 
 * This component displays a table of unique HTTP requests with columns for
 * sequence number, method, host, path, and status code. It includes modern
 * filter controls and provides selection handling and refresh capabilities.
 * 
 * Features:
 * - Modern filter panel with Host, Method, Status, and Show options
 * - Read-only JTable with proper column sizing
 * - Multi-selection mode with context menu support
 * - Thread-safe table refresh from request data
 * - Titled border with scroll pane
 * - Auto-selection of first row when available
 * - Right-click context menu for export, copy, and send actions
 * - Real-time filter application
 * 
 * @author Harshit Shah
 */
public class RequestTablePanel extends JPanel {
    
    // Filter components
    private final FilterPanel filterPanel;
    
    // Table components
    private final JTable requestTable;
    private final DefaultTableModel tableModel;
    private final TableRowSorter<DefaultTableModel> tableRowSorter;
    private final JScrollPane tableScrollPane;
    
    // Table column indices
    private static final int COL_SEQUENCE = 0;
    private static final int COL_METHOD = 1;
    private static final int COL_HOST = 2;
    private static final int COL_PATH = 3;
    private static final int COL_STATUS = 4;
    
    // Selection listeners
    private final List<RequestSelectionListener> selectionListeners;
    
    // Context action listeners
    private final List<ContextActionListener> contextActionListeners;
    
    // Filter change listeners
    private final List<FilterChangeListener> filterChangeListeners;
    
    // Current requests cache for context menu actions
    private List<RequestResponseEntry> currentRequests;
    private List<RequestResponseEntry> allRequests; // Unfiltered requests
    
    /**
     * Interface for listening to request selection changes.
     */
    public interface RequestSelectionListener {
        /**
         * Called when a request is selected in the table.
         * 
         * @param entry The selected request entry, or null if no selection
         * @param selectedIndex The index of the selected row, or -1 if no selection
         */
        void onRequestSelected(RequestResponseEntry entry, int selectedIndex);
    }
    
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
     * Interface for listening to context menu actions.
     */
    public interface ContextActionListener {
        /**
         * Called when export action is requested from context menu.
         * 
         * @param format The export format
         * @param selectedEntries The selected request entries
         */
        void onExportRequested(String format, List<RequestResponseEntry> selectedEntries);
        
        /**
         * Called when copy action is requested from context menu.
         * 
         * @param type The copy type (urls, requests, responses)
         * @param selectedEntries The selected request entries
         */
        void onCopyRequested(String type, List<RequestResponseEntry> selectedEntries);
        
        /**
         * Called when send to tool action is requested from context menu.
         * 
         * @param tool The target tool (repeater, comparer)
         * @param selectedEntries The selected request entries
         */
        void onSendToRequested(String tool, List<RequestResponseEntry> selectedEntries);
        
        /**
         * Called when remove from view action is requested from context menu.
         * 
         * @param selectedEntries The selected request entries
         */
        void onRemoveFromViewRequested(List<RequestResponseEntry> selectedEntries);
    }
    
    /**
     * Constructor initializes the request table panel.
     */
    public RequestTablePanel() {
        selectionListeners = new ArrayList<>();
        contextActionListeners = new ArrayList<>();
        filterChangeListeners = new ArrayList<>();
        
        // Create filter panel
        filterPanel = new FilterPanel();
        
        // Create table model
        String[] columnNames = {"Req#", "Method", "Host", "Path", "Status"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        
        // Create table
        requestTable = new JTable(tableModel);
        requestTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        // Initialize table row sorter with custom comparators
        tableRowSorter = new TableRowSorter<>(tableModel);
        setupTableSorting();
        requestTable.setRowSorter(tableRowSorter);
        
        // Set column widths
        setupColumnWidths();
        
        // Add selection listener
        requestTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                notifySelectionListeners();
            }
        });
        
        // Add context menu support
        setupContextMenu();
        
        // Create scroll pane
        tableScrollPane = new JScrollPane(requestTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder("Unique Requests"));
        
        // Add viewport listener to handle initial sizing and scrollpane resize
        tableScrollPane.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                // Adjust column width when viewport is resized
                SwingUtilities.invokeLater(() -> adjustPathColumnWidth());
            }
        });
        
        // Setup event handlers for filter panel
        filterPanel.addFilterChangeListener(criteria -> {
            // Apply filters and notify listeners
            applyFilters(criteria);
            notifyFilterChangeListeners(criteria);
        });
        
        // Setup panel layout
        setLayout(new BorderLayout());
        add(filterPanel, BorderLayout.NORTH);
        add(tableScrollPane, BorderLayout.CENTER);
        
        // Schedule initial column width adjustment after component is fully initialized
        SwingUtilities.invokeLater(() -> {
            // Small delay to ensure all components are properly sized
            Timer timer = new Timer(100, e -> {
                adjustPathColumnWidth();
                ((Timer) e.getSource()).stop();
            });
            timer.setRepeats(false);
            timer.start();
        });
    }
    
    /**
     * Sets up the column widths for optimal display.
     */
    private void setupColumnWidths() {
        // Set fixed preferred widths as specified
        requestTable.getColumnModel().getColumn(COL_SEQUENCE).setPreferredWidth(40);
        requestTable.getColumnModel().getColumn(COL_SEQUENCE).setMaxWidth(40);
        requestTable.getColumnModel().getColumn(COL_SEQUENCE).setMinWidth(40);
        
        requestTable.getColumnModel().getColumn(COL_METHOD).setPreferredWidth(60);
        requestTable.getColumnModel().getColumn(COL_METHOD).setMaxWidth(60);
        requestTable.getColumnModel().getColumn(COL_METHOD).setMinWidth(60);
        
        requestTable.getColumnModel().getColumn(COL_HOST).setPreferredWidth(150);
        requestTable.getColumnModel().getColumn(COL_HOST).setMinWidth(100);
        
        requestTable.getColumnModel().getColumn(COL_STATUS).setPreferredWidth(60);
        requestTable.getColumnModel().getColumn(COL_STATUS).setMaxWidth(60);
        requestTable.getColumnModel().getColumn(COL_STATUS).setMinWidth(60);
        
        // Path column will be set dynamically to fill remaining space
        requestTable.getColumnModel().getColumn(COL_PATH).setMinWidth(150);
        
        // Keep auto-resize OFF to maintain fixed column behavior
        requestTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        // Set up initial dynamic column sizing
        adjustPathColumnWidth();
        
        // Add component listener to handle table resize events
        requestTable.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                // Adjust Path column width when table is resized
                SwingUtilities.invokeLater(() -> adjustPathColumnWidth());
            }
        });
    }
    
    /**
     * Adjusts the Path column width to fill remaining space in the table viewport.
     * This method ensures no whitespace appears on the right side of the table.
     */
    private void adjustPathColumnWidth() {
        try {
            // Get the viewport width (visible area of the table)
            int viewportWidth = tableScrollPane.getViewport().getWidth();
            
            // If viewport is not yet initialized, use table width
            if (viewportWidth <= 0) {
                viewportWidth = requestTable.getWidth();
            }
            
            // Calculate total width of fixed columns
            int fixedColumnsWidth = 40 + 60 + 150 + 60; // Req# + Method + Host + Status
            
            // Calculate remaining width for Path column
            int remainingWidth = viewportWidth - fixedColumnsWidth;
            
            // Ensure Path column has at least its minimum width
            int pathColumnWidth = Math.max(remainingWidth, 150);
            
            // Set the Path column width
            requestTable.getColumnModel().getColumn(COL_PATH).setPreferredWidth(pathColumnWidth);
            
            // Force table to recalculate layout
            requestTable.doLayout();
            
        } catch (Exception e) {
            // Fallback to default width if calculation fails
            requestTable.getColumnModel().getColumn(COL_PATH).setPreferredWidth(250);
        }
    }
    
    /**
     * Sets up table sorting with custom comparators for each column.
     */
    private void setupTableSorting() {
        // Disable multi-column sorting (single column only)
        tableRowSorter.setMaxSortKeys(1);
        
        // Set up custom comparators for each column
        
        // COL_SEQUENCE (Req#): Numeric comparator
        tableRowSorter.setComparator(COL_SEQUENCE, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return Integer.compare(o1, o2);
            }
        });
        
        // COL_METHOD: Case-insensitive string comparator
        tableRowSorter.setComparator(COL_METHOD, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareToIgnoreCase(s2);
            }
        });
        
        // COL_HOST: Case-insensitive string comparator
        tableRowSorter.setComparator(COL_HOST, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareToIgnoreCase(s2);
            }
        });
        
        // COL_PATH: Case-insensitive string comparator
        tableRowSorter.setComparator(COL_PATH, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareToIgnoreCase(s2);
            }
        });
        
        // COL_STATUS: Numeric comparator with string fallback
        tableRowSorter.setComparator(COL_STATUS, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                try {
                    int status1 = Integer.parseInt(s1);
                    int status2 = Integer.parseInt(s2);
                    return Integer.compare(status1, status2);
                } catch (NumberFormatException e) {
                    // Fallback to string comparison if parsing fails
                    return s1.compareToIgnoreCase(s2);
                }
            }
        });
        
        // Start with no sorting (unsorted)
        tableRowSorter.setSortKeys(null);
    }
    
    /**
     * Sets up the context menu for right-click actions.
     */
    private void setupContextMenu() {
        JPopupMenu contextMenu = createContextMenu();
        
        // Add mouse listener for context menu
        requestTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMouseEvent(e, contextMenu);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseEvent(e, contextMenu);
            }
            
            private void handleMouseEvent(MouseEvent e, JPopupMenu menu) {
                if (e.isPopupTrigger()) {
                    // Select row under mouse if not already selected
                    int row = requestTable.rowAtPoint(e.getPoint());
                    if (row >= 0 && !requestTable.isRowSelected(row)) {
                        requestTable.setRowSelectionInterval(row, row);
                    }
                    
                    // Show context menu if we have selections
                    if (requestTable.getSelectedRowCount() > 0) {
                        menu.show(requestTable, e.getX(), e.getY());
                    }
                }
            }
        });
    }
    
    /**
     * Creates the context menu with all available actions.
     * 
     * @return The configured JPopupMenu
     */
    private JPopupMenu createContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        
        // Export submenu
        JMenu exportMenu = new JMenu("Export");
        exportMenu.add(createMenuItem("Selected to JSON", () -> notifyContextAction("export", "json")));
        exportMenu.add(createMenuItem("Selected to CSV", () -> notifyContextAction("export", "csv")));
        exportMenu.add(createMenuItem("Selected to Markdown", () -> notifyContextAction("export", "markdown")));
        contextMenu.add(exportMenu);
        
        // Copy submenu
        JMenu copyMenu = new JMenu("Copy");
        copyMenu.add(createMenuItem("URL(s)", () -> notifyContextAction("copy", "urls")));
        copyMenu.add(createMenuItem("Request(s)", () -> notifyContextAction("copy", "requests")));
        copyMenu.add(createMenuItem("Response(s)", () -> notifyContextAction("copy", "responses")));
        contextMenu.add(copyMenu);
        
        // Send to submenu
        JMenu sendToMenu = new JMenu("Send to");
        sendToMenu.add(createMenuItem("Repeater", () -> notifyContextAction("sendto", "repeater")));
        sendToMenu.add(createMenuItem("Comparer", () -> notifyContextAction("sendto", "comparer")));
        contextMenu.add(sendToMenu);
        
        // Separator
        contextMenu.addSeparator();
        
        // Remove from view
        contextMenu.add(createMenuItem("Remove from View", () -> notifyContextAction("remove", "view")));
        
        return contextMenu;
    }
    
    /**
     * Creates a menu item with the specified text and action.
     * 
     * @param text The menu item text
     * @param action The action to perform when clicked
     * @return The configured JMenuItem
     */
    private JMenuItem createMenuItem(String text, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(e -> action.run());
        return item;
    }
    
    /**
     * Notifies context action listeners of a context menu action.
     * 
     * @param actionType The type of action (export, copy, sendto, remove)
     * @param actionTarget The target of the action (json, csv, urls, etc.)
     */
    private void notifyContextAction(String actionType, String actionTarget) {
        List<RequestResponseEntry> selectedEntries = getSelectedEntries();
        if (selectedEntries.isEmpty()) {
            return;
        }
        
        for (ContextActionListener listener : contextActionListeners) {
            try {
                switch (actionType) {
                    case "export":
                        listener.onExportRequested(actionTarget, selectedEntries);
                        break;
                    case "copy":
                        listener.onCopyRequested(actionTarget, selectedEntries);
                        break;
                    case "sendto":
                        listener.onSendToRequested(actionTarget, selectedEntries);
                        break;
                    case "remove":
                        listener.onRemoveFromViewRequested(selectedEntries);
                        break;
                }
            } catch (Exception e) {
                // Log error silently - don't expose internal errors to user
                System.err.println("Error in context action: " + e.getMessage());
            }
        }
    }
    
    /**
     * Gets the currently selected request entries.
     * 
     * @return List of selected RequestResponseEntry objects
     */
    private List<RequestResponseEntry> getSelectedEntries() {
        List<RequestResponseEntry> selectedEntries = new ArrayList<>();
        int[] selectedRows = requestTable.getSelectedRows();
        
        if (currentRequests != null) {
            for (int viewRow : selectedRows) {
                // Convert view row index to model row index for sorting compatibility
                int modelRow = requestTable.convertRowIndexToModel(viewRow);
                if (modelRow >= 0 && modelRow < currentRequests.size()) {
                    selectedEntries.add(currentRequests.get(modelRow));
                }
            }
        }
        
        return selectedEntries;
    }
    
    /**
     * Refreshes the table with new request data.
     * This method is thread-safe and can be called from any thread.
     * 
     * @param requests The list of request entries to display
     */
    public void refreshTable(List<RequestResponseEntry> requests) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Store all requests for filtering
                this.allRequests = requests != null ? new ArrayList<>(requests) : new ArrayList<>();
                
                // Apply current filters
                FilterCriteria currentCriteria = filterPanel.getCurrentFilterCriteria();
                applyFilters(currentCriteria);
                
            } catch (Exception e) {
                // Log error silently - don't expose internal errors to user
                // System.err.println("Error refreshing request table: " + e.getMessage());
            }
        });
    }
    
    /**
     * Clears all data from the table.
     * This method is thread-safe and can be called from any thread.
     */
    public void clearTable() {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            notifySelectionListeners(); // Notify that nothing is selected
        });
    }
    
    /**
     * Gets the currently selected request entry.
     * 
     * @param requests The current list of requests (needed to map selection to entry)
     * @return The selected request entry, or null if no selection
     */
    public RequestResponseEntry getSelectedRequest(List<RequestResponseEntry> requests) {
        int selectedViewRow = requestTable.getSelectedRow();
        if (selectedViewRow == -1 || requests == null) {
            return null;
        }
        
        // Convert view row index to model row index for sorting compatibility
        int selectedModelRow = requestTable.convertRowIndexToModel(selectedViewRow);
        if (selectedModelRow >= 0 && selectedModelRow < requests.size()) {
            return requests.get(selectedModelRow);
        }
        
        return null;
    }
    
    /**
     * Gets the index of the currently selected row.
     * 
     * @return The selected row index, or -1 if no selection
     */
    public int getSelectedIndex() {
        return requestTable.getSelectedRow();
    }
    
    /**
     * Sets the selected row by model index.
     * This method is thread-safe and can be called from any thread.
     * 
     * @param modelIndex The model row index to select, or -1 to clear selection
     */
    public void setSelectedIndex(int modelIndex) {
        SwingUtilities.invokeLater(() -> {
            if (modelIndex >= 0 && modelIndex < tableModel.getRowCount()) {
                // Convert model row index to view row index for sorting compatibility
                int viewIndex = requestTable.convertRowIndexToView(modelIndex);
                if (viewIndex >= 0) {
                    requestTable.setRowSelectionInterval(viewIndex, viewIndex);
                } else {
                    requestTable.clearSelection();
                }
            } else {
                requestTable.clearSelection();
            }
        });
    }
    
    /**
     * Adds a selection listener to be notified when the selection changes.
     * 
     * @param listener The listener to add
     */
    public void addSelectionListener(RequestSelectionListener listener) {
        if (listener != null) {
            selectionListeners.add(listener);
        }
    }
    
    /**
     * Removes a selection listener.
     * 
     * @param listener The listener to remove
     */
    public void removeSelectionListener(RequestSelectionListener listener) {
        selectionListeners.remove(listener);
    }
    
    /**
     * Adds a context action listener to be notified of context menu actions.
     * 
     * @param listener The listener to add
     */
    public void addContextActionListener(ContextActionListener listener) {
        if (listener != null) {
            contextActionListeners.add(listener);
        }
    }
    
    /**
     * Removes a context action listener.
     * 
     * @param listener The listener to remove
     */
    public void removeContextActionListener(ContextActionListener listener) {
        contextActionListeners.remove(listener);
    }
    
    /**
     * Notifies all selection listeners of the current selection.
     * For multi-selection, notifies with the first selected item.
     */
    private void notifySelectionListeners() {
        // Note: This method is called from EDT, so we don't need to use invokeLater
        int selectedViewIndex = getSelectedIndex();
        RequestResponseEntry selectedEntry = null;
        
        // Get the first selected entry for viewer display
        if (selectedViewIndex >= 0 && currentRequests != null) {
            // Convert view row index to model row index for sorting compatibility
            int selectedModelIndex = requestTable.convertRowIndexToModel(selectedViewIndex);
            if (selectedModelIndex >= 0 && selectedModelIndex < currentRequests.size()) {
                selectedEntry = currentRequests.get(selectedModelIndex);
            }
        }
        
        for (RequestSelectionListener listener : selectionListeners) {
            try {
                listener.onRequestSelected(selectedEntry, selectedViewIndex);
            } catch (Exception e) {
                // Log error silently - don't expose internal errors to user
                // System.err.println("Error notifying selection listener: " + e.getMessage());
            }
        }
    }
    
    /**
     * Gets the number of rows currently in the table.
     * 
     * @return The row count
     */
    public int getRowCount() {
        return requestTable.getRowCount();
    }
    
    /**
     * Gets the underlying JTable component for advanced operations.
     * 
     * @return The JTable instance
     */
    public JTable getTable() {
        return requestTable;
    }
    
    /**
     * Applies filters to the current request list.
     * 
     * @param criteria The filter criteria to apply
     */
    private void applyFilters(FilterCriteria criteria) {
        if (allRequests == null) {
            return;
        }
        
        // For now, we'll implement basic filtering
        // In a full implementation, this would use the FilterEngine
        List<RequestResponseEntry> filteredRequests = new ArrayList<>();
        
        for (RequestResponseEntry entry : allRequests) {
            if (matchesFilter(entry, criteria)) {
                filteredRequests.add(entry);
            }
        }
        
        // Update the table with filtered results
        SwingUtilities.invokeLater(() -> {
            currentRequests = filteredRequests;
            refreshTableInternal(filteredRequests);
        });
    }
    
    /**
     * Checks if a request entry matches the filter criteria.
     * 
     * @param entry The request entry to check
     * @param criteria The filter criteria
     * @return true if the entry matches the criteria
     */
    private boolean matchesFilter(RequestResponseEntry entry, FilterCriteria criteria) {
        // Method filter
        if (!"All".equals(criteria.getMethod()) && 
            !criteria.getMethod().equalsIgnoreCase(entry.getMethod())) {
            return false;
        }
        
        // Status filter
        if (!"All".equals(criteria.getStatusCode())) {
            String statusFilter = criteria.getStatusCode();
            String entryStatus = entry.getStatusCode();
            
            if (statusFilter.endsWith("xx")) {
                // Range filter (2xx, 3xx, etc.)
                try {
                    int statusCode = Integer.parseInt(entryStatus);
                    int rangeStart = Integer.parseInt(statusFilter.substring(0, 1)) * 100;
                    if (statusCode < rangeStart || statusCode >= rangeStart + 100) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            } else if (!statusFilter.equals(entryStatus)) {
                // Exact match
                return false;
            }
        }
        
        // Host filter
        String hostPattern = criteria.getHostPattern();
        if (!hostPattern.isEmpty()) {
            String host = entry.getRequest().httpService().host();
            if (!host.toLowerCase().contains(hostPattern.toLowerCase())) {
                return false;
            }
        }
        
        // Response presence filter
        if (criteria.isOnlyWithResponses() && entry.getResponse() == null) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Internal method to refresh the table without triggering filter events.
     * 
     * @param requests The requests to display
     */
    private void refreshTableInternal(List<RequestResponseEntry> requests) {
        try {
            // Remember current selection and sort state
            int previousSelection = requestTable.getSelectedRow();
            List<? extends RowSorter.SortKey> sortKeys = tableRowSorter.getSortKeys();
            
            // Clear existing rows
            tableModel.setRowCount(0);
            
            // Add new rows
            if (requests != null) {
                for (int i = 0; i < requests.size(); i++) {
                    RequestResponseEntry entry = requests.get(i);
                    Object[] rowData = {
                        (i + 1),                           // Req# column (sequence number)
                        entry.getMethod(),                 // Method
                        entry.getRequest().httpService().host(), // Host
                        entry.getPath(),                   // Path
                        entry.getStatusCode()              // Status
                    };
                    tableModel.addRow(rowData);
                }
            }
            
            // Restore sort state
            if (sortKeys != null && !sortKeys.isEmpty()) {
                tableRowSorter.setSortKeys(sortKeys);
            }
            
            // Restore selection or select first row
            if (requestTable.getRowCount() > 0) {
                if (previousSelection >= 0 && previousSelection < requestTable.getRowCount()) {
                    requestTable.setRowSelectionInterval(previousSelection, previousSelection);
                } else {
                    requestTable.setRowSelectionInterval(0, 0);
                }
            } else {
                // No rows, notify listeners of no selection
                notifySelectionListeners();
            }
            
        } catch (Exception e) {
            // Log error silently
        }
    }
    
    /**
     * Notifies all filter change listeners.
     * 
     * @param criteria The filter criteria that changed
     */
    private void notifyFilterChangeListeners(FilterCriteria criteria) {
        for (FilterChangeListener listener : filterChangeListeners) {
            try {
                listener.onFilterChanged(criteria);
            } catch (Exception e) {
                // Log error silently
            }
        }
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
     * Gets the filter panel component.
     * 
     * @return The FilterPanel instance
     */
    public FilterPanel getFilterPanel() {
        return filterPanel;
    }
} 