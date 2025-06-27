package com.burp.unireq.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.logging.Logging;
import com.burp.unireq.core.FilterEngine;
import com.burp.unireq.core.RequestDeduplicator;
import com.burp.unireq.export.ExportManager;
import com.burp.unireq.model.ExportConfiguration;
import com.burp.unireq.model.RequestResponseEntry;
import com.burp.unireq.ui.components.ControlPanel;
import com.burp.unireq.ui.components.ExportPanel;
import com.burp.unireq.ui.components.RequestTablePanel;
import com.burp.unireq.ui.components.StatsPanel;
import com.burp.unireq.ui.components.ViewerPanel;
import com.burp.unireq.utils.SwingUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UniReqGui - Main GUI coordinator for the UniReq extension
 * 
 * This class acts as a coordinator for the modular UI components, managing
 * the overall layout and orchestrating communication between components.
 * It provides a clean separation of concerns while maintaining all the
 * functionality of the original monolithic design.
 * 
 * Architecture:
 * - NORTH: StatsPanel (statistics display)
 * - CENTER: Main JSplitPane with RequestTablePanel (top) and ViewerPanel (bottom)
 * - SOUTH: ControlPanel (action buttons and status)
 * 
 * Performance Optimizations:
 * - Debounced UI refresh (250ms) to prevent lag during high traffic
 * - scheduleRefresh() replaces direct updateStatistics() calls
 * - Automatic cleanup of refresh timers on extension unload
 * - Thread-safe update scheduling using AtomicBoolean flags
 * 
 * @author Harshit Shah
 */
public class UniReqGui {
    
    // Core components
    private final RequestDeduplicator deduplicator;
    private final Logging logging;
    private MontoyaApi api;
    private ExportManager exportManager;
    private FilterEngine filterEngine;
    
    // Main UI components
    private JPanel mainPanel;
    private JSplitPane mainSplitPane;
    
    // Modular UI components
    private StatsPanel statsPanel;
    private RequestTablePanel requestTablePanel;
    private ViewerPanel viewerPanel;
    private ControlPanel controlPanel;
    private ExportPanel exportPanel;
    
    // Current request data (cached for table selection handling)
    private List<RequestResponseEntry> currentRequests;
    
    // Performance optimization: Debounced UI refresh mechanism
    private final Timer uiRefreshTimer;
    private final AtomicBoolean refreshPending = new AtomicBoolean(false);
    private static final int REFRESH_DEBOUNCE_DELAY_MS = 250; // 250ms debounce delay
    
    /**
     * Constructor initializes the GUI coordinator with the deduplicator and logging.
     * 
     * @param deduplicator The request deduplicator instance
     * @param logging Burp's logging interface
     */
    public UniReqGui(RequestDeduplicator deduplicator, Logging logging) {
        this.deduplicator = deduplicator;
        this.logging = logging;
        
        // Initialize debounced refresh timer
        this.uiRefreshTimer = new Timer(REFRESH_DEBOUNCE_DELAY_MS, e -> performDebouncedRefresh());
        this.uiRefreshTimer.setRepeats(false); // Only fire once per trigger
        
        initializeComponents();
        setupEventHandlers();
    }
    
    /**
     * Initializes all UI components and assembles the layout.
     */
    private void initializeComponents() {
        // Create main panel
        mainPanel = new JPanel(new BorderLayout());
        
        // Create modular components
        createComponents();
        
        // Assemble layout
        assembleLayout();
        
        logging.logToOutput("UniReq GUI components initialized");
    }
    
    /**
     * Creates all the modular UI components.
     */
    private void createComponents() {
        // Create title panel
        JPanel titlePanel = createTitlePanel();
        
        // Create FilterEngine for consistent filtering behavior (API will be set later)
        filterEngine = new FilterEngine(logging, null);
        
        // Create modular components
        statsPanel = new StatsPanel();
        requestTablePanel = new RequestTablePanel(filterEngine);
        viewerPanel = new ViewerPanel(); // Will be initialized when API is set
        controlPanel = new ControlPanel();
        exportPanel = new ExportPanel();
        
        // Combine title, stats, and export panel with compact layout
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(titlePanel, BorderLayout.NORTH);
        
        // Create compact stats and export combined panel
        JPanel statsExportPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 1));
        statsExportPanel.setBorder(BorderFactory.createEmptyBorder(1, 10, 1, 10));
        
        // Add stats panel
        statsExportPanel.add(statsPanel);
        
        // Add subtle separator
        JLabel separator = new JLabel("|");
        separator.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        separator.setForeground(new Color(150, 150, 150));
        separator.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        statsExportPanel.add(separator);
        
        // Add export panel
        statsExportPanel.add(exportPanel);
        
        topPanel.add(statsExportPanel, BorderLayout.CENTER);
        
        // Store reference to top panel for layout
        mainPanel.add(topPanel, BorderLayout.NORTH);
    }
    
    /**
     * Creates the title panel.
     */
    private JPanel createTitlePanel() {
        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("UniReq - HTTP Request Deduplicator", SwingConstants.CENTER);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(3, 10, 2, 10));
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        return titlePanel;
    }
    
    /**
     * Assembles the main layout using the created components.
     */
    private void assembleLayout() {
        // Create main split pane (table on top, viewers on bottom)
        mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setTopComponent(requestTablePanel);
        mainSplitPane.setBottomComponent(viewerPanel);
        mainSplitPane.setDividerLocation(250); // Initial divider position
        mainSplitPane.setResizeWeight(0.4); // Give 40% to table, 60% to viewers
        
        // Add components to main panel
        mainPanel.add(mainSplitPane, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Sets up event handlers for inter-component communication.
     */
    private void setupEventHandlers() {
        // Handle table selection changes
        requestTablePanel.addSelectionListener((entry, selectedIndex) -> {
            // Update viewers with selected entry (entry is now properly provided)
            viewerPanel.updateViewers(entry);
            
            // Update export scope state based on selection
            boolean hasSelection = selectedIndex >= 0;
            exportPanel.updateScopeState(hasSelection);
        });
        
        // Handle context menu actions
        requestTablePanel.addContextActionListener(new RequestTablePanel.ContextActionListener() {
            @Override
            public void onExportRequested(String format, List<RequestResponseEntry> selectedEntries) {
                handleContextExport(format, selectedEntries);
            }
            
            @Override
            public void onCopyRequested(String type, List<RequestResponseEntry> selectedEntries) {
                handleCopyToClipboard(type, selectedEntries);
            }
            
            @Override
            public void onSendToRequested(String tool, List<RequestResponseEntry> selectedEntries) {
                handleSendToTool(tool, selectedEntries);
            }
            
            @Override
            public void onRemoveFromViewRequested(List<RequestResponseEntry> selectedEntries) {
                handleRemoveFromView(selectedEntries);
            }
        });
        
        // Handle control panel actions
        controlPanel.addActionListener((action, source) -> {
            handleControlAction(action, source);
        });
        
        // Handle export panel actions
        exportPanel.addActionListener((format) -> {
            handleExportAction(format);
        });
        
        // Initialize control panel state
        controlPanel.updateFilteringButton(deduplicator.isFilteringEnabled());
        
        // Initialize export button state (disabled initially since no data)
        exportPanel.setExportEnabled(false, 0);
        
        // Setup visible count callback to keep statistics synchronized
        requestTablePanel.setVisibleCountUpdateCallback(this::updateVisibleRequestCount);
        
        // Setup global filtering state supplier to bypass UI filters when filtering is disabled
        requestTablePanel.setGlobalFilteringEnabledSupplier(() -> deduplicator.isFilteringEnabled());
    }
    
    /**
     * Handles actions from the control panel.
     * 
     * @param action The action identifier
     * @param source The button that triggered the action
     */
    private void handleControlAction(String action, JButton source) {
        try {
            switch (action) {
                case ControlPanel.ACTION_TOGGLE_FILTERING:
                    handleToggleFiltering();
                    break;
                    
                case ControlPanel.ACTION_CLEAR_DATA:
                    handleClearData();
                    break;
                    
                case ControlPanel.ACTION_REFRESH:
                    handleRefresh();
                    break;
                    
                default:
                    logging.logToOutput("Unknown control action: " + action);
            }
        } catch (Exception e) {
            logging.logToError("Error handling control action: " + e.getMessage());
            controlPanel.updateStatus("Error: " + e.getMessage(), ControlPanel.StatusType.ERROR);
        }
    }
    
    /**
     * Handles the toggle filtering action.
     */
    private void handleToggleFiltering() {
        boolean currentState = deduplicator.isFilteringEnabled();
        deduplicator.setFilteringEnabled(!currentState);
        controlPanel.updateFilteringButton(deduplicator.isFilteringEnabled());
        
        // When disabling filtering, clear UI filters and show all requests
        if (!deduplicator.isFilteringEnabled()) {
            // Clear the UI filter criteria
            requestTablePanel.getFilterPanel().clearFilters();
            
            // Immediately refresh the table with all stored requests to bypass any filtering
            requestTablePanel.refreshTable(deduplicator.getStoredRequests());
            
            controlPanel.updateStatus("Filtering disabled – all requests visible", ControlPanel.StatusType.INFO);
        } else {
            controlPanel.updateStatus("Filtering enabled", ControlPanel.StatusType.INFO);
        }
        
        updateStatistics(); // Refresh display
    }
    
    /**
     * Handles the clear data action.
     */
    private void handleClearData() {
        deduplicator.clearFingerprints();
        updateStatistics(); // Refresh display
        requestTablePanel.clearTable(); // Clear table
        viewerPanel.clearViewers(); // Clear viewers
        
        controlPanel.updateStatus("Data cleared successfully", ControlPanel.StatusType.SUCCESS);
    }
    
    /**
     * Handles the refresh action.
     */
    private void handleRefresh() {
        updateStatistics();
        refreshRequestTable();
        
        controlPanel.updateStatus("Data refreshed", ControlPanel.StatusType.INFO);
    }
    
    /**
     * Handles export actions from the export panel.
     * 
     * @param format The selected export format
     */
    private void handleExportAction(ExportConfiguration.ExportFormat format) {
        try {
            // Show file chooser dialog
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Export UniReq Data");
            
            // Set file extension filter based on format
            String extension = format.getExtension();
            String description = format.getDescription();
            FileNameExtensionFilter filter = new FileNameExtensionFilter(description + " (*." + extension + ")", extension);
            fileChooser.setFileFilter(filter);
            
            // Set default filename
            fileChooser.setSelectedFile(new File("unireq_export." + extension));
            
            // Show save dialog
            int result = fileChooser.showSaveDialog(mainPanel);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                
                // Ensure file has correct extension
                if (!selectedFile.getName().toLowerCase().endsWith("." + extension)) {
                    selectedFile = new File(selectedFile.getAbsolutePath() + "." + extension);
                }
                
                // Get requests for export based on scope
                List<RequestResponseEntry> requestsToExport;
                if (exportPanel.isSelectedOnlyScope()) {
                    // Export only selected requests
                    requestsToExport = getSelectedRequestsForExport();
                    if (requestsToExport.isEmpty()) {
                        exportPanel.updateStatus("No requests selected for export", SwingUtils.StatusType.WARNING);
                        return;
                    }
                } else {
                    // Export all visible requests (filtered list from table panel)
                    requestsToExport = requestTablePanel.getCurrentRequests();
                    if (requestsToExport.isEmpty()) {
                        exportPanel.updateStatus("No requests to export", SwingUtils.StatusType.WARNING);
                        return;
                    }
                }
                
                // Create export configuration
                ExportConfiguration config = new ExportConfiguration(
                    format,
                    selectedFile,
                    requestsToExport,
                    true // Include full data
                );
                
                // Initialize export manager if needed
                if (exportManager == null && api != null) {
                    exportManager = new ExportManager(api.logging());
                }
                
                if (exportManager != null) {
                    // Perform export
                    exportManager.exportData(config);
                    
                    // Update status with full file path
                    String message = String.format("Exported %d requests to %s", 
                                                  requestsToExport.size(), 
                                                  selectedFile.getAbsolutePath());
                    exportPanel.updateStatus(message, SwingUtils.StatusType.SUCCESS);
                    
                    logging.logToOutput("Export completed: " + selectedFile.getAbsolutePath());
                } else {
                    exportPanel.updateStatus("Export manager not available", SwingUtils.StatusType.ERROR);
                }
            }
        } catch (Exception e) {
            String errorMsg = "Export failed: " + e.getMessage();
            exportPanel.updateStatus(errorMsg, SwingUtils.StatusType.ERROR);
            logging.logToError(errorMsg);
        }
    }
    
    /**
     * Handles context menu export actions for selected entries.
     * 
     * @param format The export format (json, csv, markdown)
     * @param selectedEntries The selected request entries
     */
    private void handleContextExport(String format, List<RequestResponseEntry> selectedEntries) {
        try {
            // Convert format string to ExportFormat enum
            ExportConfiguration.ExportFormat exportFormat;
            switch (format.toLowerCase()) {
                case "json":
                    exportFormat = ExportConfiguration.ExportFormat.JSON;
                    break;
                case "csv":
                    exportFormat = ExportConfiguration.ExportFormat.CSV;
                    break;
                case "markdown":
                    exportFormat = ExportConfiguration.ExportFormat.MARKDOWN;
                    break;
                default:
                    logging.logToError("Unknown export format: " + format);
                    return;
            }
            
            // Show file chooser dialog
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Export Selected Requests");
            
            // Set file extension filter based on format
            String extension = exportFormat.getExtension();
            String description = exportFormat.getDescription();
            FileNameExtensionFilter filter = new FileNameExtensionFilter(description + " (*." + extension + ")", extension);
            fileChooser.setFileFilter(filter);
            
            // Set default filename with timestamp
            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            fileChooser.setSelectedFile(new File("unireq_selected_" + timestamp + "." + extension));
            
            // Show save dialog
            int result = fileChooser.showSaveDialog(mainPanel);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                
                // Ensure file has correct extension
                if (!selectedFile.getName().toLowerCase().endsWith("." + extension)) {
                    selectedFile = new File(selectedFile.getAbsolutePath() + "." + extension);
                }
                
                // Create export configuration for selected entries
                ExportConfiguration config = new ExportConfiguration(
                    exportFormat,
                    selectedFile,
                    selectedEntries,
                    true // Include full data
                );
                
                // Initialize export manager if needed
                if (exportManager == null && api != null) {
                    exportManager = new ExportManager(api.logging());
                }
                
                if (exportManager != null) {
                    // Perform export
                    exportManager.exportData(config);
                    
                    // Log success
                    String message = String.format("Exported %d selected requests to %s", 
                                                  selectedEntries.size(), 
                                                  selectedFile.getAbsolutePath());
                    logging.logToOutput(message);
                } else {
                    logging.logToError("Export manager not available");
                }
            }
        } catch (Exception e) {
            String errorMsg = "Context export failed: " + e.getMessage();
            logging.logToError(errorMsg);
        }
    }
    
    /**
     * Handles copy to clipboard actions for selected entries.
     * 
     * @param type The copy type (urls, requests, responses)
     * @param selectedEntries The selected request entries
     */
    private void handleCopyToClipboard(String type, List<RequestResponseEntry> selectedEntries) {
        try {
            StringBuilder content = new StringBuilder();
            
            switch (type.toLowerCase()) {
                case "urls":
                    for (RequestResponseEntry entry : selectedEntries) {
                        String url = (entry.getRequest().httpService().secure() ? "https://" : "http://") +
                                   entry.getRequest().httpService().host() +
                                   (entry.getRequest().httpService().port() != (entry.getRequest().httpService().secure() ? 443 : 80) ? 
                                    ":" + entry.getRequest().httpService().port() : "") +
                                   entry.getPath();
                        content.append(url).append("\n");
                    }
                    break;
                    
                case "requests":
                    for (int i = 0; i < selectedEntries.size(); i++) {
                        if (i > 0) content.append("\n---- REQUEST ----\n");
                        content.append(selectedEntries.get(i).getRequest().toString());
                    }
                    break;
                    
                case "responses":
                    for (int i = 0; i < selectedEntries.size(); i++) {
                        if (i > 0) content.append("\n---- RESPONSE ----\n");
                        if (selectedEntries.get(i).getResponse() != null) {
                            content.append(selectedEntries.get(i).getResponse().toString());
                        } else {
                            content.append("No response available");
                        }
                    }
                    break;
                    
                default:
                    logging.logToError("Unknown copy type: " + type);
                    return;
            }
            
            // Copy to clipboard
            java.awt.datatransfer.StringSelection stringSelection = new java.awt.datatransfer.StringSelection(content.toString().trim());
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
            
            logging.logToOutput("Copied " + selectedEntries.size() + " " + type + " to clipboard");
            
        } catch (Exception e) {
            String errorMsg = "Copy to clipboard failed: " + e.getMessage();
            logging.logToError(errorMsg);
        }
    }
    
    /**
     * Handles send to tool actions for selected entries.
     * 
     * @param tool The target tool (repeater, comparer)
     * @param selectedEntries The selected request entries
     */
    private void handleSendToTool(String tool, List<RequestResponseEntry> selectedEntries) {
        try {
            if (api == null) {
                logging.logToError("Burp API not available for send to " + tool);
                return;
            }
            
            switch (tool.toLowerCase()) {
                case "repeater":
                    // Send only the first selected request to Repeater (as confirmed)
                    if (!selectedEntries.isEmpty()) {
                        RequestResponseEntry entry = selectedEntries.get(0);
                        api.repeater().sendToRepeater(entry.getRequest());
                        logging.logToOutput("Sent request to Repeater: " + entry.getMethod() + " " + entry.getPath());
                    }
                    break;
                    
                case "comparer":
                    // Send each selected request individually to Comparer (as confirmed)
                    for (RequestResponseEntry entry : selectedEntries) {
                        if (entry.getResponse() != null) {
                            api.comparer().sendToComparer(entry.getRequest().toByteArray());
                            api.comparer().sendToComparer(entry.getResponse().toByteArray());
                        } else {
                            api.comparer().sendToComparer(entry.getRequest().toByteArray());
                        }
                        logging.logToOutput("Sent to Comparer: " + entry.getMethod() + " " + entry.getPath());
                    }
                    break;
                    
                default:
                    logging.logToError("Unknown tool: " + tool);
                    return;
            }
            
        } catch (Exception e) {
            String errorMsg = "Send to " + tool + " failed: " + e.getMessage();
            logging.logToError(errorMsg);
        }
    }
    
    /**
     * Handles remove from view actions for selected entries.
     * This implements soft removal (visual hide only).
     * 
     * @param selectedEntries The selected request entries to remove from view
     */
    private void handleRemoveFromView(List<RequestResponseEntry> selectedEntries) {
        try {
            // For now, we'll implement a simple approach by refreshing the table
            // without the removed entries. In a more sophisticated implementation,
            // we would maintain a Set<String> of removed fingerprints.
            
            if (currentRequests != null) {
                // Create a new list without the selected entries
                List<RequestResponseEntry> filteredRequests = new ArrayList<>();
                for (RequestResponseEntry entry : currentRequests) {
                    boolean shouldRemove = false;
                    for (RequestResponseEntry selectedEntry : selectedEntries) {
                        if (entry.getFingerprint().equals(selectedEntry.getFingerprint())) {
                            shouldRemove = true;
                            break;
                        }
                    }
                    if (!shouldRemove) {
                        filteredRequests.add(entry);
                    }
                }
                
                // Update the cached requests and refresh table
                currentRequests = filteredRequests;
                requestTablePanel.refreshTable(currentRequests);
                
                // Update statistics display
                updateStatistics();
                
                logging.logToOutput("Removed " + selectedEntries.size() + " requests from view");
            }
            
        } catch (Exception e) {
            String errorMsg = "Remove from view failed: " + e.getMessage();
            logging.logToError(errorMsg);
        }
    }
    
    /**
     * Sets the Montoya API reference and initializes API-dependent components.
     * 
     * @param api The Montoya API instance
     */
    public void setApi(MontoyaApi api) {
        this.api = api;
        
        // Initialize viewer panel with API
        if (viewerPanel != null) {
            viewerPanel.setApi(api);
        }
        
        // Initialize filter engine with API
        if (filterEngine != null) {
            filterEngine.setApi(api);
        }
        
        logging.logToOutput("UniReq GUI API initialized");
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
     * Schedules a debounced UI refresh to prevent excessive updates.
     * This method can be called frequently - it will only trigger an actual refresh
     * after the debounce delay has passed without additional calls.
     * 
     * Performance: Replaces immediate updateStatistics() calls to prevent UI lag.
     */
    public void scheduleRefresh() {
        if (refreshPending.compareAndSet(false, true)) {
            // Only restart timer if no refresh is currently pending
            SwingUtilities.invokeLater(() -> {
                if (uiRefreshTimer.isRunning()) {
                    uiRefreshTimer.restart();
                } else {
                    uiRefreshTimer.start();
                }
            });
        } else {
            // Refresh already pending, just restart the timer to extend the delay
            SwingUtilities.invokeLater(() -> uiRefreshTimer.restart());
        }
    }
    
    /**
     * Performs the actual debounced refresh operation.
     * This method is called by the timer after the debounce delay.
     */
    private void performDebouncedRefresh() {
        SwingUtilities.invokeLater(() -> {
            try {
                refreshPending.set(false); // Reset the pending flag
                updateStatistics(); // Perform the actual update
            } catch (Exception e) {
                logging.logToError("Error in debounced refresh: " + e.getMessage());
            }
        });
    }
    
    /**
     * Updates the statistics display and refreshes the request table.
     * This method is thread-safe and can be called from any thread.
     * Note: Visible count is now updated via callback from RequestTablePanel.
     * 
     * Performance: This method should not be called directly for real-time updates.
     * Use scheduleRefresh() instead to prevent UI lag.
     */
    public void updateStatistics() {
        if (deduplicator != null) {
            // Refresh request table (this will trigger the visible count callback)
            refreshRequestTable();
            
            // Update export button state based on data availability
            List<RequestResponseEntry> availableRequests = deduplicator.getStoredRequests();
            boolean hasRequests = !availableRequests.isEmpty();
            exportPanel.setExportEnabled(hasRequests, availableRequests.size());
            
            // Update export scope state based on table selection
            boolean hasSelection = requestTablePanel.getSelectedIndex() >= 0;
            exportPanel.updateScopeState(hasSelection);
            
            // Log GUI updates for monitoring (visible count logged in callback)
            logging.logToOutput("GUI statistics updated - Total: " + deduplicator.getTotalRequests() + 
                             ", Unique: " + deduplicator.getUniqueRequests() + 
                             ", Duplicates: " + deduplicator.getDuplicateRequests());
        }
    }
    
    /**
     * Refreshes the request table with current data from the deduplicator.
     */
    private void refreshRequestTable() {
        if (deduplicator != null && requestTablePanel != null) {
            // Get current requests and cache them
            currentRequests = deduplicator.getStoredRequests();
            
            // Update table
            requestTablePanel.refreshTable(currentRequests);
        }
    }
    
    /**
     * Gets the statistics panel component.
     * 
     * @return The StatsPanel instance
     */
    public StatsPanel getStatsPanel() {
        return statsPanel;
    }
    
    /**
     * Gets the request table panel component.
     * 
     * @return The RequestTablePanel instance
     */
    public RequestTablePanel getRequestTablePanel() {
        return requestTablePanel;
    }
    
    /**
     * Gets the viewer panel component.
     * 
     * @return The ViewerPanel instance
     */
    public ViewerPanel getViewerPanel() {
        return viewerPanel;
    }
    
    /**
     * Gets the control panel component.
     * 
     * @return The ControlPanel instance
     */
    public ControlPanel getControlPanel() {
        return controlPanel;
    }
    
    /**
     * Gets the export panel component.
     * 
     * @return The ExportPanel instance
     */
    public ExportPanel getExportPanel() {
        return exportPanel;
    }
    
    /**
     * Cleanup method called when the extension is unloaded.
     */
    public void cleanup() {
        // Stop the debounced refresh timer
        if (uiRefreshTimer != null && uiRefreshTimer.isRunning()) {
            uiRefreshTimer.stop();
        }
        
        // Clean up individual components
        if (viewerPanel != null) {
            viewerPanel.cleanup();
        }
        
        // Clear cached data
        currentRequests = null;
        
        logging.logToOutput("UniReq GUI cleanup completed");
    }
    
    /**
     * Gets the currently selected requests for export.
     * Uses the filtered request list from the table panel to ensure correct mapping.
     * 
     * @return List of selected RequestResponseEntry objects
     */
    private List<RequestResponseEntry> getSelectedRequestsForExport() {
        List<RequestResponseEntry> selectedRequests = new ArrayList<>();
        
        if (requestTablePanel != null) {
            JTable table = requestTablePanel.getTable();
            int[] selectedRows = table.getSelectedRows();
            
            // Get the filtered requests from the table panel (what user actually sees)
            List<RequestResponseEntry> filteredRequests = requestTablePanel.getCurrentRequests();
            
            for (int selectedRow : selectedRows) {
                // Convert view index to model index
                int modelIndex = table.convertRowIndexToModel(selectedRow);
                if (modelIndex >= 0 && modelIndex < filteredRequests.size()) {
                    selectedRequests.add(filteredRequests.get(modelIndex));
                }
            }
        }
        
        return selectedRequests;
    }
    
    /**
     * Updates the visible request count in the statistics display.
     * This method is called via callback when the filtered request count changes.
     * 
     * @param visibleCount The current number of visible (filtered) requests
     */
    private void updateVisibleRequestCount(int visibleCount) {
        SwingUtilities.invokeLater(() -> {
            if (deduplicator != null) {
                // Update statistics panel with the exact filtered count
                statsPanel.updateStatistics(
                    deduplicator.getTotalRequests(),
                    deduplicator.getUniqueRequests(),
                    deduplicator.getDuplicateRequests(),
                    visibleCount
                );
                
                // Log the update for monitoring
                logging.logToOutput("Visible count updated via callback: " + visibleCount);
            }
        });
    }
} 