package com.burp.unireq.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.logging.Logging;
import com.burp.unireq.core.RequestDeduplicator;
import com.burp.unireq.model.RequestResponseEntry;
import com.burp.unireq.ui.components.StatsPanel;
import com.burp.unireq.ui.components.RequestTablePanel;
import com.burp.unireq.ui.components.ViewerPanel;
import com.burp.unireq.ui.components.ControlPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

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
 * @author Harshit Shah
 */
public class UniReqGui {
    
    private final RequestDeduplicator deduplicator;
    private final Logging logging;
    private MontoyaApi api;
    
    // Main UI components
    private JPanel mainPanel;
    private JSplitPane mainSplitPane;
    
    // Modular UI components
    private StatsPanel statsPanel;
    private RequestTablePanel requestTablePanel;
    private ViewerPanel viewerPanel;
    private ControlPanel controlPanel;
    
    // Current request data (cached for table selection handling)
    private List<RequestResponseEntry> currentRequests;
    
    /**
     * Constructor initializes the GUI coordinator with the deduplicator and logging.
     * 
     * @param deduplicator The request deduplicator instance
     * @param logging Burp's logging interface
     */
    public UniReqGui(RequestDeduplicator deduplicator, Logging logging) {
        this.deduplicator = deduplicator;
        this.logging = logging;
        
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
        
        // Create modular components
        statsPanel = new StatsPanel();
        requestTablePanel = new RequestTablePanel();
        viewerPanel = new ViewerPanel(); // Will be initialized when API is set
        controlPanel = new ControlPanel();
        
        // Combine title and stats
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(titlePanel, BorderLayout.NORTH);
        topPanel.add(statsPanel, BorderLayout.CENTER);
        
        // Store reference to top panel for layout
        mainPanel.add(topPanel, BorderLayout.NORTH);
    }
    
    /**
     * Creates the title panel.
     */
    private JPanel createTitlePanel() {
        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("UniReq - HTTP Request Deduplicator", SwingConstants.CENTER);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
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
            // Get the actual entry from current requests
            RequestResponseEntry actualEntry = null;
            if (selectedIndex >= 0 && currentRequests != null && selectedIndex < currentRequests.size()) {
                actualEntry = currentRequests.get(selectedIndex);
            }
            
            // Update viewers with selected entry
            viewerPanel.updateViewers(actualEntry);
        });
        
        // Handle control panel actions
        controlPanel.addActionListener((action, source) -> {
            handleControlAction(action, source);
        });
        
        // Initialize control panel state
        controlPanel.updateFilteringButton(deduplicator.isFilteringEnabled());
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
        updateStatistics(); // Refresh display
        
        String status = deduplicator.isFilteringEnabled() ? "Filtering enabled" : "Filtering disabled";
        controlPanel.updateStatus(status, ControlPanel.StatusType.INFO);
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
     * Updates the statistics display and refreshes the request table.
     * This method is thread-safe and can be called from any thread.
     */
    public void updateStatistics() {
        if (deduplicator != null) {
            // Update statistics panel
            statsPanel.updateStatistics(
                deduplicator.getTotalRequests(),
                deduplicator.getUniqueRequests(),
                deduplicator.getDuplicateRequests()
            );
            
            // Refresh request table
            refreshRequestTable();
            
            // Log GUI updates for monitoring
            logging.logToOutput("GUI statistics updated - Total: " + deduplicator.getTotalRequests() + 
                             ", Unique: " + deduplicator.getUniqueRequests() + 
                             ", Duplicates: " + deduplicator.getDuplicateRequests());
            
            logging.logToOutput("GUI statistics updated - Total: " + deduplicator.getTotalRequests());
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
     * Cleanup method called when the extension is unloaded.
     */
    public void cleanup() {
        // Clean up individual components
        if (viewerPanel != null) {
            viewerPanel.cleanup();
        }
        
        // Clear cached data
        currentRequests = null;
        
        logging.logToOutput("UniReq GUI cleanup completed");
    }
} 