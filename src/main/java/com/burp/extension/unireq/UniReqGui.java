package com.burp.extension.unireq;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.ArrayList;

/**
 * UniReqGui - HTTP History-Style GUI for UniReq Extension
 * 
 * This class provides a comprehensive GUI interface similar to Burp's HTTP History tab.
 * It displays unique HTTP requests in a table format with detailed request/response viewers.
 * 
 * Features:
 * - Top panel: Table listing all unique requests (Method, Path, Status, Timestamp)
 * - Bottom panel: Split view with request and response editors
 * - Control panel: Enable/disable filtering, clear data, refresh statistics
 * - Statistics panel: Real-time counters and status indicators
 * - Memory management: Caps storage at 1000 requests with FIFO eviction
 * 
 * UI Layout:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ Control Panel: [Enable] [Clear] [Refresh] | Statistics      │
 * ├─────────────────────────────────────────────────────────────┤
 * │ Request List Table (Method | Path | Status | Time)          │
 * │ ┌─────────────────────────────────────────────────────────┐ │
 * │ │ GET /api/users    200    10:30:45                       │ │
 * │ │ POST /api/login   302    10:31:12                       │ │
 * │ └─────────────────────────────────────────────────────────┘ │
 * ├─────────────────────────────────────────────────────────────┤
 * │ Request Viewer        │ Response Viewer                     │
 * │ ┌───────────────────┐ │ ┌─────────────────────────────────┐ │
 * │ │ [Pretty][Raw][Hex]│ │ │ [Pretty][Raw][Hex]              │ │
 * │ │                   │ │ │                                 │ │
 * │ │ GET /api/users    │ │ │ HTTP/1.1 200 OK                 │ │
 * │ │ Host: example.com │ │ │ Content-Type: application/json  │ │
 * │ └───────────────────┘ │ └─────────────────────────────────┘ │
 * └─────────────────────────────────────────────────────────────┘
 * 
 * Thread Safety:
 * - All GUI updates are performed on the EDT
 * - Data access is synchronized with the RequestDeduplicator
 * - Table model updates are thread-safe
 */
public class UniReqGui extends JPanel {
    
    // Core components
    private final RequestDeduplicator deduplicator;
    private final Logging logging;
    private MontoyaApi api; // Will be set when available
    
    // GUI Components - Control Panel
    private JButton enableFilteringButton;
    private JButton clearDataButton;
    private JButton refreshButton;
    
    // GUI Components - Statistics Panel
    private JLabel uniqueLabel;
    private JLabel duplicatesLabel;
    private JLabel storedLabel;
    private JLabel filteringLabel;
    
    // GUI Components - Request List Table
    private JTable requestTable;
    private RequestTableModel tableModel;
    private JScrollPane tableScrollPane;
    
    // GUI Components - Request/Response Viewers
    private HttpRequestEditor requestEditor;
    private HttpResponseEditor responseEditor;
    private JSplitPane viewerSplitPane;
    private JPanel requestViewerPanel;
    private JPanel responseViewerPanel;
    private JTabbedPane requestTabbedPane;
    private JTabbedPane responseTabbedPane;
    private JButton targetButton;
    
    // Auto-refresh timer for statistics
    private Timer refreshTimer;
    private static final int REFRESH_INTERVAL_MS = 2000; // 2 seconds
    
    /**
     * Constructor initializes the GUI with the deduplicator and logging components.
     * 
     * @param deduplicator The request deduplication engine
     * @param logging Burp's logging interface
     */
    public UniReqGui(RequestDeduplicator deduplicator, Logging logging) {
        this.deduplicator = deduplicator;
        this.logging = logging;
        
        // Initialize the GUI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            initializeGui();
            setupAutoRefresh();
            logging.logToOutput("UniReq GUI initialized with HTTP History-style interface");
        });
    }
    
    /**
     * Sets the Montoya API reference for creating request/response editors.
     * This is called after the extension is fully initialized.
     * 
     * @param api The Montoya API instance
     */
    public void setApi(MontoyaApi api) {
        this.api = api;
        SwingUtilities.invokeLater(this::initializeEditors);
    }
    
    /**
     * Initializes the complete GUI layout with all panels and components.
     * Creates a layout similar to Burp's HTTP History tab.
     */
    private void initializeGui() {
        setLayout(new BorderLayout());
        setBorder(null); // Remove any default border
        setOpaque(true);
        
        // Create main panels
        JPanel topPanel = createTopPanel();
        JPanel centerPanel = createCenterPanel();
        JPanel bottomPanel = createBottomPanel();
        
        // Create a main split pane to divide table and viewers
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, centerPanel, bottomPanel);
        mainSplitPane.setResizeWeight(0.4); // Give more space to bottom panel
        mainSplitPane.setOneTouchExpandable(true);
        mainSplitPane.setContinuousLayout(true);
        mainSplitPane.setDividerLocation(250);
        
        // Layout main panels
        add(topPanel, BorderLayout.NORTH);
        add(mainSplitPane, BorderLayout.CENTER);
        
        // Initialize with current statistics
        updateStatistics();
    }
    
    /**
     * Creates the top panel containing controls and statistics.
     * Uses a clean layout with proper separation between controls and stats.
     * 
     * @return The configured top panel
     */
    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        
        // Control panel on the left
        JPanel controlPanel = createControlPanel();
        
        // Statistics panel on the right - completely separate from controls
        JPanel statisticsPanel = createStatisticsPanel();
        
        topPanel.add(controlPanel, BorderLayout.WEST);
        topPanel.add(statisticsPanel, BorderLayout.CENTER);
        
        return topPanel;
    }
    
    /**
     * Creates the control panel with action buttons.
     * Uses FlowLayout for consistent horizontal button alignment.
     * Titled border groups the controls for better visual organization.
     * 
     * @return The configured control panel
     */
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Controls"));
        
        // Enable/Disable filtering button - primary action
        enableFilteringButton = new JButton("Disable Filtering");
        enableFilteringButton.setToolTipText("Toggle request filtering on/off");
        enableFilteringButton.addActionListener(e -> toggleFiltering());
        enableFilteringButton.setPreferredSize(new Dimension(130, 28));
        
        // Clear data button - destructive action, needs confirmation
        clearDataButton = new JButton("Clear All Data");
        clearDataButton.setToolTipText("Clear all stored requests and reset statistics");
        clearDataButton.addActionListener(e -> clearAllData());
        clearDataButton.setPreferredSize(new Dimension(110, 28));
        
        // Refresh button - utility action
        refreshButton = new JButton("Refresh");
        refreshButton.setToolTipText("Manually refresh the display");
        refreshButton.addActionListener(e -> refreshDisplay());
        refreshButton.setPreferredSize(new Dimension(80, 28));
        
        // Add buttons in logical order: Toggle → Refresh → Clear
        // Clear is last as it's the most destructive action
        panel.add(enableFilteringButton);
        panel.add(refreshButton);
        panel.add(clearDataButton);
        
        return panel;
    }
    
    /**
     * Creates the statistics panel with real-time counters.
     * Uses clean Swing labels with proper HTML rendering for colored text.
     * Each statistic is a separate label for easier updates and better spacing.
     * 
     * @return The configured statistics panel
     */
    /**
     * Creates the statistics panel displaying real-time metrics.
     * Uses individual JLabels with clean layout and proper color coding.
     * Avoids HTML rendering issues by using native Swing components.
     * 
     * @return The configured statistics panel
     */
    private JPanel createStatisticsPanel() {
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        statsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLoweredBevelBorder(),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        // Create individual labels for each statistic
        // Use composite labels with separate text and value components
        JLabel uniqueTitle = new JLabel("Unique: ");
        uniqueTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        uniqueLabel = new JLabel("0");
        uniqueLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        uniqueLabel.setForeground(new Color(0, 128, 0)); // Green
        
        JLabel duplicatesTitle = new JLabel("Duplicates: ");
        duplicatesTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        duplicatesLabel = new JLabel("0");
        duplicatesLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        duplicatesLabel.setForeground(Color.RED);
        
        JLabel storedTitle = new JLabel("Stored: ");
        storedTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        storedLabel = new JLabel("0");
        storedLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        storedLabel.setForeground(new Color(0, 0, 255)); // Blue
        
        JLabel filteringTitle = new JLabel("Filtering: ");
        filteringTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        filteringLabel = new JLabel("ENABLED");
        filteringLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        filteringLabel.setForeground(new Color(0, 128, 0)); // Green
        
        // Add tooltips
        uniqueTitle.setToolTipText("Number of unique HTTP requests captured");
        duplicatesTitle.setToolTipText("Number of duplicate requests filtered out");
        storedTitle.setToolTipText("Total requests stored in memory");
        filteringTitle.setToolTipText("Current filtering status");
        
        // Create separators
        JLabel sep1 = new JLabel(" | ");
        JLabel sep2 = new JLabel(" | ");
        JLabel sep3 = new JLabel(" | ");
        sep1.setForeground(Color.GRAY);
        sep2.setForeground(Color.GRAY);
        sep3.setForeground(Color.GRAY);
        
        // Add components to panel in order
        statsPanel.add(uniqueTitle);
        statsPanel.add(uniqueLabel);
        statsPanel.add(sep1);
        statsPanel.add(duplicatesTitle);
        statsPanel.add(duplicatesLabel);
        statsPanel.add(sep2);
        statsPanel.add(storedTitle);
        statsPanel.add(storedLabel);
        statsPanel.add(sep3);
        statsPanel.add(filteringTitle);
        statsPanel.add(filteringLabel);
        
        return statsPanel;
    }
    

    
    /**
     * Creates the center panel containing the request list table.
     * 
     * @return The configured center panel
     */
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Unique HTTP Requests"));
        
        // Initialize table model and table
        tableModel = new RequestTableModel();
        requestTable = new JTable(tableModel);
        
        // Configure table appearance and behavior
        configureRequestTable();
        
        // Add table to scroll pane
        tableScrollPane = new JScrollPane(requestTable);
        tableScrollPane.setPreferredSize(new Dimension(800, 180));
        tableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        panel.add(tableScrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Configures the request table appearance, selection, and event handling.
     */
    private void configureRequestTable() {
        // Set column widths
        requestTable.getColumnModel().getColumn(0).setPreferredWidth(80);  // Method
        requestTable.getColumnModel().getColumn(1).setPreferredWidth(400); // Path
        requestTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // Status
        requestTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Timestamp
        
        // Enable single row selection
        requestTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Add selection listener to update request/response viewers
        requestTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateViewersForSelectedRequest();
            }
        });
        
        // Configure row height and appearance
        requestTable.setRowHeight(25);
        requestTable.setShowGrid(true);
        requestTable.setGridColor(Color.LIGHT_GRAY);
        
        // Set custom cell renderer for status codes
        requestTable.getColumnModel().getColumn(2).setCellRenderer(new StatusCodeRenderer());
    }
    
    /**
     * Creates the bottom panel containing request/response viewers with tabbed interfaces.
     * Implements proper tabbed layout without scroll arrows and includes Target button.
     * 
     * @return The configured bottom panel
     */
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Create request viewer panel with tabbed interface
        requestViewerPanel = createRequestViewerPanel();
        
        // Create response viewer panel with tabbed interface and Target button
        responseViewerPanel = createResponseViewerPanel();
        
        // Create split pane for request/response viewers
        viewerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, requestViewerPanel, responseViewerPanel);
        viewerSplitPane.setResizeWeight(0.5); // Equal split
        viewerSplitPane.setDividerLocation(400);
        viewerSplitPane.setOneTouchExpandable(true);
        viewerSplitPane.setContinuousLayout(true);
        
        panel.add(viewerSplitPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Creates the request viewer panel with tabbed interface.
     * Uses WRAP_TAB_LAYOUT to prevent scroll arrows.
     * 
     * @return The configured request viewer panel
     */
    private JPanel createRequestViewerPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Request"));
        
        // Create tabbed pane for request viewing options
        requestTabbedPane = new JTabbedPane();
        requestTabbedPane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT); // Prevent scroll arrows
        
        // Add placeholder content initially
        JLabel placeholderLabel = new JLabel("Select a request to view details", SwingConstants.CENTER);
        placeholderLabel.setForeground(Color.GRAY);
        requestTabbedPane.addTab("Pretty", placeholderLabel);
        requestTabbedPane.addTab("Raw", new JLabel("Select a request to view raw content", SwingConstants.CENTER));
        requestTabbedPane.addTab("Hex", new JLabel("Select a request to view hex content", SwingConstants.CENTER));
        
        panel.add(requestTabbedPane, BorderLayout.CENTER);
        panel.setMinimumSize(new Dimension(300, 200));
        panel.setPreferredSize(new Dimension(400, 300));
        
        return panel;
    }
    
    /**
     * Creates the response viewer panel with tabbed interface and Target button.
     * Uses WRAP_TAB_LAYOUT to prevent scroll arrows and includes Target button in top-right.
     * 
     * @return The configured response viewer panel
     */
    private JPanel createResponseViewerPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Response"));
        
        // Create top panel with Target button
        JPanel topPanel = new JPanel(new BorderLayout());
        
        // Create Target button that displays the target URL
        targetButton = new JButton("Target: (none)");
        targetButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        targetButton.setPreferredSize(new Dimension(200, 25));
        targetButton.setToolTipText("Configure target details");
        targetButton.setEnabled(false); // Disabled until request is selected
        targetButton.addActionListener(e -> configureTarget());
        targetButton.setHorizontalAlignment(SwingConstants.LEFT);
        
        // Add Target button to top-right
        topPanel.add(targetButton, BorderLayout.EAST);
        
        // Create tabbed pane for response viewing options
        responseTabbedPane = new JTabbedPane();
        responseTabbedPane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT); // Prevent scroll arrows
        
        // Add placeholder content initially
        JLabel placeholderLabel = new JLabel("Select a request to view response", SwingConstants.CENTER);
        placeholderLabel.setForeground(Color.GRAY);
        responseTabbedPane.addTab("Pretty", placeholderLabel);
        responseTabbedPane.addTab("Raw", new JLabel("Select a request to view raw response", SwingConstants.CENTER));
        responseTabbedPane.addTab("Hex", new JLabel("Select a request to view hex response", SwingConstants.CENTER));
        responseTabbedPane.addTab("Render", new JLabel("Select a request to render response", SwingConstants.CENTER));
        
        // Layout: Target button at top, tabbed pane in center
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(responseTabbedPane, BorderLayout.CENTER);
        panel.setMinimumSize(new Dimension(300, 200));
        panel.setPreferredSize(new Dimension(400, 300));
        
        return panel;
    }
    
    /**
     * Initializes the HTTP request and response editors using Burp's native editors.
     * This method is called after the Montoya API is available.
     * Integrates editors into the tabbed interface.
     */
    private void initializeEditors() {
        if (api == null) {
            logging.logToError("Cannot initialize editors: Montoya API not available");
            return;
        }
        
        try {
            // Create Burp's native request and response editors
            requestEditor = api.userInterface().createHttpRequestEditor();
            responseEditor = api.userInterface().createHttpResponseEditor();
            
            // Get the editor components
            Component requestComponent = requestEditor.uiComponent();
            Component responseComponent = responseEditor.uiComponent();
            
            // Ensure components are properly sized
            requestComponent.setPreferredSize(new Dimension(400, 300));
            responseComponent.setPreferredSize(new Dimension(400, 300));
            
            // Replace placeholder tabs with actual editors in request tabbed pane
            requestTabbedPane.removeAll();
            requestTabbedPane.addTab("Pretty", requestComponent);
            requestTabbedPane.addTab("Raw", requestComponent);
            requestTabbedPane.addTab("Hex", requestComponent);
            
            // Replace placeholder tabs with actual editors in response tabbed pane
            responseTabbedPane.removeAll();
            responseTabbedPane.addTab("Pretty", responseComponent);
            responseTabbedPane.addTab("Raw", responseComponent);
            responseTabbedPane.addTab("Hex", responseComponent);
            responseTabbedPane.addTab("Render", responseComponent);
            
            // Update split pane divider location after adding editors
            SwingUtilities.invokeLater(() -> {
                viewerSplitPane.setDividerLocation(0.5);
                viewerSplitPane.revalidate();
            });
            
            // Refresh the display
            revalidate();
            repaint();
            
            logging.logToOutput("HTTP request/response editors initialized successfully with tabbed interface");
            
        } catch (Exception e) {
            logging.logToError("Failed to initialize editors: " + e.getMessage());
        }
    }
    
    /**
     * Updates the request/response viewers when a table row is selected.
     * Displays the selected request and its corresponding response (if available).
     * Updates the Target button to show the target URL.
     */
    private void updateViewersForSelectedRequest() {
        int selectedRow = requestTable.getSelectedRow();
        if (selectedRow < 0 || requestEditor == null || responseEditor == null) {
            // Reset Target button when no selection
            if (targetButton != null) {
                targetButton.setText("Target: (none)");
                targetButton.setEnabled(false);
            }
            return;
        }
        
        try {
            // Get the selected request entry
            RequestDeduplicator.RequestResponseEntry entry = tableModel.getEntryAt(selectedRow);
            if (entry == null) {
                if (targetButton != null) {
                    targetButton.setText("Target: (none)");
                    targetButton.setEnabled(false);
                }
                return;
            }
            
            // Update request editor
            HttpRequest request = entry.getRequest();
            if (request != null) {
                requestEditor.setRequest(request);
                
                // Update Target button to show the target URL
                if (targetButton != null) {
                    String targetUrl = request.url();
                    // Extract host from URL for display
                    try {
                        java.net.URL url = new java.net.URL(targetUrl);
                        String host = url.getHost();
                        targetButton.setText("Target: " + host);
                        targetButton.setEnabled(true);
                    } catch (Exception urlEx) {
                        targetButton.setText("Target: " + targetUrl);
                        targetButton.setEnabled(true);
                    }
                }
            }
            
            // Update response editor
            HttpResponse response = entry.getResponse();
            if (response != null) {
                responseEditor.setResponse(response);
            } else {
                // Clear response editor if no response is available
                responseEditor.setResponse(null);
            }
            
        } catch (Exception e) {
            logging.logToError("Error updating viewers: " + e.getMessage());
            if (targetButton != null) {
                targetButton.setText("Target: (error)");
                targetButton.setEnabled(false);
            }
        }
    }
    
    /**
     * Handles the Target button click event.
     * Shows a "Configure target details" dialog similar to Burp's Repeater target configuration.
     * Allows users to modify the host, port, and HTTPS settings for the selected request.
     */
    private void configureTarget() {
        int selectedRow = requestTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        
        try {
            RequestDeduplicator.RequestResponseEntry entry = tableModel.getEntryAt(selectedRow);
            if (entry == null || entry.getRequest() == null) {
                return;
            }
            
            HttpRequest selectedRequest = entry.getRequest();
            String currentUrl = selectedRequest.url();
            
            // Parse current URL
            java.net.URL url = new java.net.URL(currentUrl);
            String currentHost = url.getHost();
            int currentPort = url.getPort();
            boolean isHttps = "https".equalsIgnoreCase(url.getProtocol());
            
            // Use default ports if not specified
            if (currentPort == -1) {
                currentPort = isHttps ? 443 : 80;
            }
            
            // Show target configuration dialog
            TargetConfigDialog dialog = new TargetConfigDialog(
                SwingUtilities.getWindowAncestor(this),
                currentHost,
                currentPort,
                isHttps
            );
            
            dialog.setVisible(true);
            
            if (dialog.isConfirmed()) {
                // User clicked OK - could implement target modification here
                String newHost = dialog.getHost();
                int newPort = dialog.getPort();
                boolean newHttps = dialog.isHttps();
                
                logging.logToOutput(String.format("Target configured: %s:%d (HTTPS: %s)", 
                    newHost, newPort, newHttps));
                
                // Update the target button display
                targetButton.setText("Target: " + newHost);
                
                // TODO: Implement actual request modification with new target
                // This would involve creating a new HttpRequest with modified URL
            }
            
        } catch (Exception e) {
            logging.logToError("Error configuring target: " + e.getMessage());
        }
    }
    
    /**
     * Dialog for configuring target details (host, port, HTTPS).
     * Similar to Burp's "Configure target details" dialog in Repeater.
     */
    private static class TargetConfigDialog extends JDialog {
        private JTextField hostField;
        private JTextField portField;
        private JCheckBox httpsCheckbox;
        private boolean confirmed = false;
        
        public TargetConfigDialog(java.awt.Window parent, String host, int port, boolean https) {
            super(parent, "Configure target details", ModalityType.APPLICATION_MODAL);
            initializeDialog(host, port, https);
        }
        
        private void initializeDialog(String host, int port, boolean https) {
            setLayout(new BorderLayout());
            setSize(350, 200);
            setLocationRelativeTo(getParent());
            
            // Main panel
            JPanel mainPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            
            // Description label
            JLabel descLabel = new JLabel("Specify the details of the server to which the request will be sent:");
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(0, 0, 15, 0);
            mainPanel.add(descLabel, gbc);
            
            // Host field
            JLabel hostLabel = new JLabel("Host:");
            gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(5, 0, 5, 10);
            mainPanel.add(hostLabel, gbc);
            
            hostField = new JTextField(host, 20);
            gbc.gridx = 1; gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 0, 5, 0);
            mainPanel.add(hostField, gbc);
            
            // Port field
            JLabel portLabel = new JLabel("Port:");
            gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
            gbc.insets = new Insets(5, 0, 5, 10);
            mainPanel.add(portLabel, gbc);
            
            portField = new JTextField(String.valueOf(port), 20);
            gbc.gridx = 1; gbc.gridy = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 0, 5, 0);
            mainPanel.add(portField, gbc);
            
            // HTTPS checkbox
            httpsCheckbox = new JCheckBox("Use HTTPS", https);
            gbc.gridx = 1; gbc.gridy = 3; gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(10, 0, 10, 0);
            mainPanel.add(httpsCheckbox, gbc);
            
            // Button panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton okButton = new JButton("OK");
            JButton cancelButton = new JButton("Cancel");
            
            okButton.addActionListener(e -> {
                confirmed = true;
                setVisible(false);
            });
            
            cancelButton.addActionListener(e -> {
                confirmed = false;
                setVisible(false);
            });
            
            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);
            
            add(mainPanel, BorderLayout.CENTER);
            add(buttonPanel, BorderLayout.SOUTH);
            
            // Set default button
            getRootPane().setDefaultButton(okButton);
        }
        
        public boolean isConfirmed() { return confirmed; }
        public String getHost() { return hostField.getText().trim(); }
        public int getPort() {
            try {
                return Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException e) {
                return httpsCheckbox.isSelected() ? 443 : 80;
            }
        }
        public boolean isHttps() { return httpsCheckbox.isSelected(); }
    }
    
    /**
     * Sets up auto-refresh timer for real-time statistics updates.
     * Updates the display every 2 seconds to show current statistics and request list.
     */
    private void setupAutoRefresh() {
        refreshTimer = new Timer(REFRESH_INTERVAL_MS, e -> refreshDisplay());
        refreshTimer.start();
        logging.logToOutput("Auto-refresh timer started (interval: " + REFRESH_INTERVAL_MS + "ms)");
    }
    
    /**
     * Refreshes the entire display including statistics and request list.
     * This method is called by the auto-refresh timer and manual refresh button.
     */
    private void refreshDisplay() {
        SwingUtilities.invokeLater(() -> {
            updateStatistics();
            updateRequestTable();
        });
    }
    
    /**
     * Updates the statistics labels with current values from the deduplicator.
     * Uses clean Swing HTML for proper color rendering.
     * Called frequently by auto-refresh timer, so optimized for performance.
     */
    public void updateStatistics() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::updateStatistics);
            return;
        }
        
        try {
            // Get current statistics from deduplicator
            long uniqueRequests = deduplicator.getUniqueRequests();
            long duplicateRequests = deduplicator.getDuplicateRequests();
            int storedRequests = deduplicator.getStoredRequests().size();
            boolean filteringEnabled = deduplicator.isFilteringEnabled();
            
            // Update statistics using clean method for proper HTML rendering
            updateStats((int)uniqueRequests, (int)duplicateRequests, storedRequests, filteringEnabled);
            
            // Update button text to reflect current state
            enableFilteringButton.setText(filteringEnabled ? "Disable Filtering" : "Enable Filtering");
            
        } catch (Exception e) {
            logging.logToError("Error updating statistics: " + e.getMessage());
        }
    }
    
    /**
     * Updates the statistics display with clean HTML rendering.
     * This method ensures proper Swing HTML processing without raw HTML strings.
     * 
     * @param unique Number of unique requests
     * @param duplicates Number of duplicate requests  
     * @param stored Number of stored requests
     * @param filteringEnabled Whether filtering is currently enabled
     */
    /**
     * Updates the statistics display with current values.
     * Uses native Swing components with proper color coding.
     * 
     * @param unique Number of unique requests
     * @param duplicates Number of duplicate requests filtered
     * @param stored Number of requests stored in memory
     * @param filteringEnabled Whether filtering is currently enabled
     */
    private void updateStats(int unique, int duplicates, int stored, boolean filteringEnabled) {
        // Update unique requests (green)
        uniqueLabel.setText(String.valueOf(unique));
        uniqueLabel.setForeground(new Color(0, 128, 0)); // Green
        
        // Update duplicates (red)
        duplicatesLabel.setText(String.valueOf(duplicates));
        duplicatesLabel.setForeground(Color.RED);
        
        // Update stored requests (blue)
        storedLabel.setText(String.valueOf(stored));
        storedLabel.setForeground(new Color(0, 0, 255)); // Blue
        
        // Update filtering status with appropriate color
        String statusText = filteringEnabled ? "ENABLED" : "DISABLED";
        Color statusColor = filteringEnabled ? new Color(0, 128, 0) : Color.RED;
        filteringLabel.setText(statusText);
        filteringLabel.setForeground(statusColor);
    }
    

    
    /**
     * Updates the request table with current data from the deduplicator.
     * Preserves the current selection if possible.
     */
    private void updateRequestTable() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::updateRequestTable);
            return;
        }
        
        try {
            // Remember current selection
            int selectedRow = requestTable.getSelectedRow();
            
            // Update table data
            tableModel.updateData(deduplicator.getStoredRequests());
            
            // Restore selection if still valid
            if (selectedRow >= 0 && selectedRow < tableModel.getRowCount()) {
                requestTable.setRowSelectionInterval(selectedRow, selectedRow);
            }
            
        } catch (Exception e) {
            logging.logToError("Error updating request table: " + e.getMessage());
        }
    }
    
    /**
     * Toggles request filtering on/off.
     * Updates the deduplicator state and refreshes the display.
     */
    private void toggleFiltering() {
        try {
            boolean currentState = deduplicator.isFilteringEnabled();
            deduplicator.setFilteringEnabled(!currentState);
            
            String action = currentState ? "disabled" : "enabled";
            logging.logToOutput("Request filtering " + action + " by user");
            
            // Immediate refresh to show new state
            updateStatistics();
            
        } catch (Exception e) {
            logging.logToError("Error toggling filtering: " + e.getMessage());
        }
    }
    
    /**
     * Clears all stored data and resets statistics.
     * Shows confirmation dialog before proceeding.
     */
    private void clearAllData() {
        int result = JOptionPane.showConfirmDialog(
            this,
            "This will clear all stored requests and reset statistics.\nAre you sure?",
            "Clear All Data",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            try {
                deduplicator.clearFingerprints();
                
                // Clear viewers
                if (requestEditor != null) {
                    requestEditor.setRequest(null);
                }
                if (responseEditor != null) {
                    responseEditor.setResponse(null);
                }
                
                // Immediate refresh
                refreshDisplay();
                
                logging.logToOutput("All data cleared by user");
                
            } catch (Exception e) {
                logging.logToError("Error clearing data: " + e.getMessage());
            }
        }
    }
    
    /**
     * Returns the tab caption for Burp's interface.
     * 
     * @return The tab caption
     */
    public String getTabCaption() {
        return "UniReq";
    }
    
    /**
     * Returns the UI component for Burp's interface.
     * 
     * @return This panel as the UI component
     */
    public Component getUiComponent() {
        return this;
    }
    
    /**
     * Cleanup method called when the extension is unloaded.
     * Stops the auto-refresh timer and cleans up resources.
     */
    public void cleanup() {
        if (refreshTimer != null && refreshTimer.isRunning()) {
            refreshTimer.stop();
            logging.logToOutput("Auto-refresh timer stopped");
        }
    }
    
    /**
     * Custom table model for displaying HTTP request entries.
     * Provides a read-only table with columns for Method, Path, Status Code, and Timestamp.
     */
    private class RequestTableModel extends AbstractTableModel {
        private final String[] columnNames = {"Method", "Path", "Status", "Timestamp"};
        private List<RequestDeduplicator.RequestResponseEntry> entries = new ArrayList<>();
        
        /**
         * Updates the table data with new entries.
         * 
         * @param newEntries The new list of request entries
         */
        public void updateData(List<RequestDeduplicator.RequestResponseEntry> newEntries) {
            this.entries = new ArrayList<>(newEntries);
            fireTableDataChanged();
        }
        
        /**
         * Gets the entry at the specified row.
         * 
         * @param row The row index
         * @return The request entry or null if invalid
         */
        public RequestDeduplicator.RequestResponseEntry getEntryAt(int row) {
            if (row >= 0 && row < entries.size()) {
                return entries.get(row);
            }
            return null;
        }
        
        @Override
        public int getRowCount() {
            return entries.size();
        }
        
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex >= entries.size()) {
                return "";
            }
            
            RequestDeduplicator.RequestResponseEntry entry = entries.get(rowIndex);
            switch (columnIndex) {
                case 0: return entry.getMethod();
                case 1: return entry.getPath();
                case 2: return entry.getStatusCode();
                case 3: return entry.getFormattedTimestamp();
                default: return "";
            }
        }
        
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false; // Read-only table
        }
    }
    
    /**
     * Custom cell renderer for status codes with color coding.
     * Colors status codes based on HTTP status ranges (2xx = green, 4xx = orange, 5xx = red).
     */
    private class StatusCodeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (!isSelected && value != null) {
                String statusCode = value.toString();
                
                if (statusCode.startsWith("2")) {
                    // 2xx - Success (Green)
                    setForeground(new Color(0, 128, 0));
                } else if (statusCode.startsWith("3")) {
                    // 3xx - Redirection (Blue)
                    setForeground(Color.BLUE);
                } else if (statusCode.startsWith("4")) {
                    // 4xx - Client Error (Orange)
                    setForeground(new Color(255, 140, 0));
                } else if (statusCode.startsWith("5")) {
                    // 5xx - Server Error (Red)
                    setForeground(Color.RED);
                } else {
                    // Unknown or Pending (Black)
                    setForeground(Color.BLACK);
                }
            }
            
            return component;
        }
    }
} 