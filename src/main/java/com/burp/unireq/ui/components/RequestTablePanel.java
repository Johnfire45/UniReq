package com.burp.unireq.ui.components;

import com.burp.unireq.model.RequestResponseEntry;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ArrayList;

/**
 * RequestTablePanel - HTTP request table component for UniReq extension
 * 
 * This component displays a table of unique HTTP requests with columns for
 * sequence number, method, host, path, and status code. It provides selection
 * handling and refresh capabilities for real-time updates.
 * 
 * Features:
 * - Read-only JTable with proper column sizing
 * - Multi-selection mode with context menu support
 * - Thread-safe table refresh from request data
 * - Titled border with scroll pane
 * - Auto-selection of first row when available
 * - Right-click context menu for export, copy, and send actions
 * 
 * @author Harshit Shah
 */
public class RequestTablePanel extends JPanel {
    
    // Table components
    private final JTable requestTable;
    private final DefaultTableModel tableModel;
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
    
    // Current requests cache for context menu actions
    private List<RequestResponseEntry> currentRequests;
    
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
        requestTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        
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
        
        // Setup panel layout
        setLayout(new BorderLayout());
        add(tableScrollPane, BorderLayout.CENTER);
    }
    
    /**
     * Sets up the column widths for optimal display.
     */
    private void setupColumnWidths() {
        requestTable.getColumnModel().getColumn(COL_SEQUENCE).setPreferredWidth(50);
        requestTable.getColumnModel().getColumn(COL_SEQUENCE).setMaxWidth(60);
        requestTable.getColumnModel().getColumn(COL_METHOD).setPreferredWidth(80);
        requestTable.getColumnModel().getColumn(COL_METHOD).setMaxWidth(100);
        requestTable.getColumnModel().getColumn(COL_HOST).setPreferredWidth(200);
        requestTable.getColumnModel().getColumn(COL_PATH).setPreferredWidth(300);
        requestTable.getColumnModel().getColumn(COL_STATUS).setPreferredWidth(80);
        requestTable.getColumnModel().getColumn(COL_STATUS).setMaxWidth(100);
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
            for (int row : selectedRows) {
                if (row >= 0 && row < currentRequests.size()) {
                    selectedEntries.add(currentRequests.get(row));
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
                // Cache current requests for context menu actions
                this.currentRequests = requests;
                
                // Remember current selection
                int previousSelection = requestTable.getSelectedRow();
                
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
        int selectedRow = requestTable.getSelectedRow();
        if (selectedRow == -1 || requests == null || selectedRow >= requests.size()) {
            return null;
        }
        return requests.get(selectedRow);
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
     * Sets the selected row by index.
     * This method is thread-safe and can be called from any thread.
     * 
     * @param index The row index to select, or -1 to clear selection
     */
    public void setSelectedIndex(int index) {
        SwingUtilities.invokeLater(() -> {
            if (index >= 0 && index < requestTable.getRowCount()) {
                requestTable.setRowSelectionInterval(index, index);
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
        int selectedIndex = getSelectedIndex();
        RequestResponseEntry selectedEntry = null;
        
        // Get the first selected entry for viewer display
        if (selectedIndex >= 0 && currentRequests != null && selectedIndex < currentRequests.size()) {
            selectedEntry = currentRequests.get(selectedIndex);
        }
        
        for (RequestSelectionListener listener : selectionListeners) {
            try {
                listener.onRequestSelected(selectedEntry, selectedIndex);
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
} 