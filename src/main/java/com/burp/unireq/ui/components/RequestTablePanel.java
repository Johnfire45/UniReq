package com.burp.unireq.ui.components;

import com.burp.unireq.model.RequestResponseEntry;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;

/**
 * RequestTablePanel - HTTP request table component for UniReq extension
 * 
 * This component displays a table of unique HTTP requests with columns for
 * method, host, path, status code, and timestamp. It provides selection
 * handling and refresh capabilities for real-time updates.
 * 
 * Features:
 * - Read-only JTable with proper column sizing
 * - Single selection mode with selection listeners
 * - Thread-safe table refresh from request data
 * - Titled border with scroll pane
 * - Auto-selection of first row when available
 * 
 * @author Harshit Shah
 */
public class RequestTablePanel extends JPanel {
    
    // Table components
    private final JTable requestTable;
    private final DefaultTableModel tableModel;
    private final JScrollPane tableScrollPane;
    
    // Table column indices
    private static final int COL_METHOD = 0;
    private static final int COL_HOST = 1;
    private static final int COL_PATH = 2;
    private static final int COL_STATUS = 3;
    private static final int COL_TIME = 4;
    
    // Selection listeners
    private final List<RequestSelectionListener> selectionListeners;
    
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
     * Constructor initializes the request table panel.
     */
    public RequestTablePanel() {
        selectionListeners = new ArrayList<>();
        
        // Create table model
        String[] columnNames = {"Method", "Host", "Path", "Status", "Time"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        
        // Create table
        requestTable = new JTable(tableModel);
        requestTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        requestTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        
        // Set column widths
        setupColumnWidths();
        
        // Add selection listener
        requestTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                notifySelectionListeners();
            }
        });
        
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
        requestTable.getColumnModel().getColumn(COL_METHOD).setPreferredWidth(80);
        requestTable.getColumnModel().getColumn(COL_METHOD).setMaxWidth(100);
        requestTable.getColumnModel().getColumn(COL_HOST).setPreferredWidth(200);
        requestTable.getColumnModel().getColumn(COL_PATH).setPreferredWidth(300);
        requestTable.getColumnModel().getColumn(COL_STATUS).setPreferredWidth(80);
        requestTable.getColumnModel().getColumn(COL_STATUS).setMaxWidth(100);
        requestTable.getColumnModel().getColumn(COL_TIME).setPreferredWidth(100);
        requestTable.getColumnModel().getColumn(COL_TIME).setMaxWidth(120);
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
                // Remember current selection
                int previousSelection = requestTable.getSelectedRow();
                
                // Clear existing rows
                tableModel.setRowCount(0);
                
                // Add new rows
                if (requests != null) {
                    for (RequestResponseEntry entry : requests) {
                        Object[] rowData = {
                            entry.getMethod(),
                            entry.getRequest().httpService().host(),
                            entry.getPath(),
                            entry.getStatusCode(),
                            entry.getFormattedTimestamp()
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
     * Notifies all selection listeners of the current selection.
     */
    private void notifySelectionListeners() {
        // Note: This method is called from EDT, so we don't need to use invokeLater
        int selectedIndex = getSelectedIndex();
        
        // We can't determine the actual entry without the current request list,
        // so we pass null for the entry and let the listener handle it
        for (RequestSelectionListener listener : selectionListeners) {
            try {
                listener.onRequestSelected(null, selectedIndex);
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