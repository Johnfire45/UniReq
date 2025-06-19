package com.burp.extension.unireq;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
import com.burp.extension.unireq.model.RequestResponseEntry;

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
    
    // GUI Components - Filter Panel
    private JPanel filterPanel;
    private JComboBox<String> methodFilterCombo;
    private JComboBox<String> statusFilterCombo;
    private JTextField hostFilterField;
    private JTextField pathFilterField;
    private JCheckBox showOnlyWithResponsesCheckbox;
    private JCheckBox showOnlyHighlightedCheckbox;
    private JCheckBox caseSensitiveCheckbox;
    private JCheckBox regexModeCheckbox;
    private JButton showFilterButton;
    private JButton hideFilterButton;
    private boolean filterPanelVisible = false;
    
    // GUI Components - Context Menu and Export
    private JPopupMenu contextMenu;
    private JButton exportButton;
    
    // GUI Components - Multi-select support
    private List<RequestResponseEntry> selectedEntries = new ArrayList<>();
    private int currentSelectedIndex = 0; // For cycling through selected entries
    private JLabel selectionInfoLabel;
    
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
    
    // Viewer state tracking to prevent unnecessary updates and scroll resets
    private HttpRequest currentDisplayedRequest = null;
    private HttpResponse currentDisplayedResponse = null;
    private boolean editorsReadOnly = true; // Track read-only state
    
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
     * Creates a layout similar to Burp's HTTP History tab with integrated filter panel.
     */
    private void initializeGui() {
        setLayout(new BorderLayout());
        setBorder(null); // Remove any default border
        setOpaque(true);
        
        // Create main panels
        JPanel topPanel = createTopPanel();
        JPanel centerPanel = createCenterPanel();
        JPanel bottomPanel = createBottomPanel();
        
        // Create filter panel (initially hidden)
        filterPanel = createFilterPanel();
        
        // Create a combined center panel that includes filter and table
        JPanel combinedCenterPanel = new JPanel(new BorderLayout());
        combinedCenterPanel.add(filterPanel, BorderLayout.NORTH);
        combinedCenterPanel.add(centerPanel, BorderLayout.CENTER);
        
        // Create a main split pane to divide table and viewers
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, combinedCenterPanel, bottomPanel);
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
        
        // Filter button - toggle filter panel visibility
        showFilterButton = new JButton("Filter");
        showFilterButton.setToolTipText("Show/hide request filter panel");
        showFilterButton.addActionListener(e -> toggleFilterPanel());
        showFilterButton.setPreferredSize(new Dimension(70, 28));
        
        // Export button - export filtered requests
        exportButton = new JButton("Export");
        exportButton.setToolTipText("Export filtered requests to various formats");
        exportButton.addActionListener(e -> showExportDialog());
        exportButton.setPreferredSize(new Dimension(80, 28));
        
        // Add buttons in logical order: Toggle → Filter → Export → Refresh → Clear
        // Clear is last as it's the most destructive action
        panel.add(enableFilteringButton);
        panel.add(showFilterButton);
        panel.add(exportButton);
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
     * Creates the filter panel for filtering HTTP requests.
     * Similar to Burp's HTTP history filter with comprehensive filtering options.
     * 
     * @return The configured filter panel
     */
    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("HTTP History Filter"));
        panel.setVisible(false); // Initially hidden
        
        // Main filter content panel
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Row 1: Method and Status filters
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; gbc.insets = new Insets(5, 5, 5, 10);
        contentPanel.add(new JLabel("Method:"), gbc);
        
        methodFilterCombo = new JComboBox<>(new String[]{"All", "GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"});
        methodFilterCombo.setPreferredSize(new Dimension(100, 25));
        methodFilterCombo.addActionListener(e -> applyFilters());
        gbc.gridx = 1; gbc.gridy = 0;
        contentPanel.add(methodFilterCombo, gbc);
        
        gbc.gridx = 2; gbc.gridy = 0; gbc.insets = new Insets(5, 20, 5, 10);
        contentPanel.add(new JLabel("Status:"), gbc);
        
        statusFilterCombo = new JComboBox<>(new String[]{"All", "2xx", "3xx", "4xx", "5xx", "200", "302", "404", "500"});
        statusFilterCombo.setPreferredSize(new Dimension(80, 25));
        statusFilterCombo.addActionListener(e -> applyFilters());
        gbc.gridx = 3; gbc.gridy = 0;
        contentPanel.add(statusFilterCombo, gbc);
        
        // Row 2: Host and Path filters
        gbc.gridx = 0; gbc.gridy = 1; gbc.insets = new Insets(5, 5, 5, 10);
        contentPanel.add(new JLabel("Host:"), gbc);
        
        hostFilterField = new JTextField(15);
        hostFilterField.setToolTipText("Filter by hostname (e.g., example.com)");
        hostFilterField.getDocument().addDocumentListener(createDocumentListener());
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPanel.add(hostFilterField, gbc);
        
        gbc.gridx = 3; gbc.gridy = 1; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.insets = new Insets(5, 20, 5, 10);
        contentPanel.add(new JLabel("Path:"), gbc);
        
        pathFilterField = new JTextField(15);
        pathFilterField.setToolTipText("Filter by request path (e.g., /api/users)");
        pathFilterField.getDocument().addDocumentListener(createDocumentListener());
        gbc.gridx = 4; gbc.gridy = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPanel.add(pathFilterField, gbc);
        
        // Row 3: Checkboxes
        showOnlyWithResponsesCheckbox = new JCheckBox("Show only items with responses");
        showOnlyWithResponsesCheckbox.addActionListener(e -> applyFilters());
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.WEST; gbc.insets = new Insets(10, 5, 5, 10);
        contentPanel.add(showOnlyWithResponsesCheckbox, gbc);
        
        showOnlyHighlightedCheckbox = new JCheckBox("Show only highlighted items");
        showOnlyHighlightedCheckbox.addActionListener(e -> applyFilters());
        showOnlyHighlightedCheckbox.setEnabled(false); // TODO: Implement highlighting feature
        gbc.gridx = 2; gbc.gridy = 2; gbc.gridwidth = 2;
        contentPanel.add(showOnlyHighlightedCheckbox, gbc);
        
        // Row 4: Advanced options
        caseSensitiveCheckbox = new JCheckBox("Case sensitive");
        caseSensitiveCheckbox.addActionListener(e -> applyFilters());
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; gbc.insets = new Insets(5, 5, 5, 10);
        contentPanel.add(caseSensitiveCheckbox, gbc);
        
        regexModeCheckbox = new JCheckBox("Regex");
        regexModeCheckbox.addActionListener(e -> applyFilters());
        gbc.gridx = 1; gbc.gridy = 3;
        contentPanel.add(regexModeCheckbox, gbc);
        
        // Control buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton clearFiltersButton = new JButton("Clear");
        clearFiltersButton.setToolTipText("Clear all filters");
        clearFiltersButton.addActionListener(e -> clearFilters());
        clearFiltersButton.setPreferredSize(new Dimension(70, 25));
        
        hideFilterButton = new JButton("Hide");
        hideFilterButton.setToolTipText("Hide filter panel");
        hideFilterButton.addActionListener(e -> toggleFilterPanel());
        hideFilterButton.setPreferredSize(new Dimension(70, 25));
        
        buttonPanel.add(clearFiltersButton);
        buttonPanel.add(hideFilterButton);
        
        gbc.gridx = 4; gbc.gridy = 3; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.EAST;
        contentPanel.add(buttonPanel, gbc);
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
        return panel;
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
     * Includes context menu setup for right-click functionality.
     */
    private void configureRequestTable() {
        // Set column widths
        requestTable.getColumnModel().getColumn(0).setPreferredWidth(80);  // Method
        requestTable.getColumnModel().getColumn(1).setPreferredWidth(400); // Path
        requestTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // Status
        requestTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Timestamp
        
        // Enable multiple row selection with Ctrl/Cmd and Shift support
        requestTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        requestTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        // Add selection listener to update request/response viewers
        requestTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateViewersForSelectedRequests();
            }
        });
        
        // Configure row height and appearance
        requestTable.setRowHeight(25);
        requestTable.setShowGrid(true);
        requestTable.setGridColor(Color.LIGHT_GRAY);
        
        // Set custom cell renderer for status codes
        requestTable.getColumnModel().getColumn(2).setCellRenderer(new StatusCodeRenderer());
        
        // Setup context menu for right-click functionality
        setupContextMenu();
        
        // Add mouse listener for context menu and double-click
        requestTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                // Handle right-click context menu
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }
            
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                // Handle right-click context menu (for cross-platform compatibility)
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }
        });
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
        
        // Create top panel with navigation controls and Target button
        JPanel topPanel = new JPanel(new BorderLayout());
        
        // Left panel for navigation controls (for multi-select)
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        
        JButton prevButton = new JButton("◀ Prev");
        prevButton.setToolTipText("Navigate to previous selected request");
        prevButton.addActionListener(e -> navigateToPreviousRequest());
        prevButton.setPreferredSize(new Dimension(80, 25));
        prevButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        
        JButton nextButton = new JButton("Next ▶");
        nextButton.setToolTipText("Navigate to next selected request");
        nextButton.addActionListener(e -> navigateToNextRequest());
        nextButton.setPreferredSize(new Dimension(80, 25));
        nextButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        
        // Selection info label
        selectionInfoLabel = new JLabel("No selection");
        selectionInfoLabel.setFont(selectionInfoLabel.getFont().deriveFont(Font.ITALIC, 10f));
        selectionInfoLabel.setForeground(Color.GRAY);
        selectionInfoLabel.setVisible(false); // Initially hidden
        
        navPanel.add(prevButton);
        navPanel.add(nextButton);
        navPanel.add(Box.createHorizontalStrut(10));
        navPanel.add(selectionInfoLabel);
        
        // Right panel for Target button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 2));
        
        // Create Target button that displays the target URL
        targetButton = new JButton("Target: (none)");
        targetButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        targetButton.setPreferredSize(new Dimension(200, 25));
        targetButton.setToolTipText("Configure target details");
        targetButton.setEnabled(false); // Disabled until request is selected
        targetButton.addActionListener(e -> configureTarget());
        targetButton.setHorizontalAlignment(SwingConstants.LEFT);
        
        buttonPanel.add(targetButton);
        
        // Add both panels to top panel
        topPanel.add(navPanel, BorderLayout.WEST);
        topPanel.add(buttonPanel, BorderLayout.EAST);
        
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
     * Integrates editors into the tabbed interface. Burp's native editors are read-only by default.
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
            
            // Note: Burp's native editors are read-only by default - users cannot edit the content
            
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
            
            logging.logToOutput("HTTP request/response editors initialized successfully (read-only by default)");
            
        } catch (Exception e) {
            logging.logToError("Failed to initialize editors: " + e.getMessage());
        }
    }
    
    /**
     * Updates the request and response viewers with the currently selected requests.
     * Handles both single and multiple selection modes with navigation controls.
     * Resets viewer state tracking to ensure fresh content is loaded.
     */
    private void updateViewersForSelectedRequests() {
        int[] selectedRows = requestTable.getSelectedRows();
        
        // Update selected entries list
        selectedEntries.clear();
        for (int row : selectedRows) {
            if (row >= 0 && row < tableModel.getRowCount()) {
                RequestResponseEntry entry = tableModel.getEntryAt(row);
                if (entry != null) {
                    selectedEntries.add(entry);
                }
            }
        }
        
        // Reset current index when selection changes
        currentSelectedIndex = 0;
        
        // Reset viewer state tracking to force fresh content loading
        // This ensures that when selection changes, content is properly updated
        currentDisplayedRequest = null;
        currentDisplayedResponse = null;
        
        // Update viewers with current selection
        updateCurrentViewer();
        
        // Update selection info label
        updateSelectionInfo();
    }
    
    /**
     * Updates the current viewer with the entry at currentSelectedIndex.
     * Handles single entry display and navigation through multiple selections.
     * Only updates editors when content actually changes to preserve scroll position.
     */
    private void updateCurrentViewer() {
        if (selectedEntries.isEmpty() || requestEditor == null || responseEditor == null) {
            // Clear viewers and reset Target button when no selection
            if (targetButton != null) {
                targetButton.setText("Target: (none)");
                targetButton.setEnabled(false);
            }
            
            // Clear editors only if they currently have content
            if (currentDisplayedRequest != null) {
                requestEditor.setRequest(null);
                currentDisplayedRequest = null;
            }
            if (currentDisplayedResponse != null) {
                responseEditor.setResponse(null);
                currentDisplayedResponse = null;
            }
            
            return;
        }
        
        // Ensure currentSelectedIndex is within bounds
        if (currentSelectedIndex >= selectedEntries.size()) {
            currentSelectedIndex = 0;
        }
        
        try {
            RequestResponseEntry entry = selectedEntries.get(currentSelectedIndex);
            
            // Update request editor only if content has changed
            HttpRequest request = entry.getRequest();
            if (request != null && !request.equals(currentDisplayedRequest)) {
                requestEditor.setRequest(request);
                currentDisplayedRequest = request;
                
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
            } else if (request == null && currentDisplayedRequest != null) {
                // Clear request if entry has no request but editor currently shows one
                requestEditor.setRequest(null);
                currentDisplayedRequest = null;
                if (targetButton != null) {
                    targetButton.setText("Target: (none)");
                    targetButton.setEnabled(false);
                }
            }
            
            // Update response editor only if content has changed
            HttpResponse response = entry.getResponse();
            if (response != null && !response.equals(currentDisplayedResponse)) {
                responseEditor.setResponse(response);
                currentDisplayedResponse = response;
            } else if (response == null && currentDisplayedResponse != null) {
                // Clear response if entry has no response but editor currently shows one
                responseEditor.setResponse(null);
                currentDisplayedResponse = null;
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
     * Updates the selection info label to show current selection status.
     * Displays information about multiple selections and navigation.
     */
    private void updateSelectionInfo() {
        if (selectionInfoLabel == null) return;
        
        if (selectedEntries.isEmpty()) {
            selectionInfoLabel.setText("No selection");
            selectionInfoLabel.setVisible(false);
        } else if (selectedEntries.size() == 1) {
            selectionInfoLabel.setText("1 request selected");
            selectionInfoLabel.setVisible(true);
        } else {
            selectionInfoLabel.setText(String.format("Showing %d of %d selected requests", 
                currentSelectedIndex + 1, selectedEntries.size()));
            selectionInfoLabel.setVisible(true);
        }
    }
    
    /**
     * Navigates to the previous request in the multi-selection.
     * Preserves scroll position by not resetting viewer state tracking.
     */
    private void navigateToPreviousRequest() {
        if (selectedEntries.size() <= 1) return;
        
        currentSelectedIndex--;
        if (currentSelectedIndex < 0) {
            currentSelectedIndex = selectedEntries.size() - 1; // Wrap to last
        }
        
        // Don't reset viewer state tracking here - let updateCurrentViewer handle content changes
        updateCurrentViewer();
        updateSelectionInfo();
    }
    
    /**
     * Navigates to the next request in the multi-selection.
     * Preserves scroll position by not resetting viewer state tracking.
     */
    private void navigateToNextRequest() {
        if (selectedEntries.size() <= 1) return;
        
        currentSelectedIndex++;
        if (currentSelectedIndex >= selectedEntries.size()) {
            currentSelectedIndex = 0; // Wrap to first
        }
        
        // Don't reset viewer state tracking here - let updateCurrentViewer handle content changes
        updateCurrentViewer();
        updateSelectionInfo();
    }
    
    /**
     * Updates the request/response viewers when a table row is selected.
     * Displays the selected request and its corresponding response (if available).
     * Updates the Target button to show the target URL.
     * 
     * @deprecated Use updateViewersForSelectedRequests() instead for multi-select support
     */
    @Deprecated
    private void updateViewersForSelectedRequest() {
        // Delegate to new multi-select method for backward compatibility
        updateViewersForSelectedRequests();
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
            RequestResponseEntry entry = tableModel.getEntryAt(selectedRow);
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
     * Refreshes the display with updated data from the deduplicator.
     * Called by auto-refresh timer and manual refresh button.
     * Avoids updating viewers to preserve scroll position during auto-refresh.
     */
    private void refreshDisplay() {
        SwingUtilities.invokeLater(() -> {
            // Always update statistics and table data
            updateStatistics();
            updateRequestTable();
            
            // Note: We don't call updateViewersForSelectedRequests() here to avoid
            // scroll position resets during auto-refresh. Viewers are updated only
            // when user explicitly changes selection.
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
     * Applies any active filters and preserves the current selection if possible.
     */
    private void updateRequestTable() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::updateRequestTable);
            return;
        }
        
        try {
            // Remember current multi-selection before updating table
            int[] selectedRows = requestTable.getSelectedRows();
            
            // If filter panel is visible and has active filters, apply them
            if (filterPanelVisible && hasActiveFilters()) {
                applyFilters();
            } else {
                // Update table data directly without filters
                tableModel.updateData(deduplicator.getStoredRequests());
            }
            
            // Restore multi-selection if any rows were previously selected
            // Only restore selections that are still valid after the update
            if (selectedRows.length > 0) {
                for (int row : selectedRows) {
                    if (row >= 0 && row < tableModel.getRowCount()) {
                        // Use addRowSelectionInterval to preserve existing selections
                        requestTable.addRowSelectionInterval(row, row);
                    }
                }
            }
            
        } catch (Exception e) {
            logging.logToError("Error updating request table: " + e.getMessage());
        }
    }
    
    /**
     * Checks if any filters are currently active.
     * 
     * @return true if any filter is set to a non-default value
     */
    private boolean hasActiveFilters() {
        if (methodFilterCombo == null) return false;
        
        return !methodFilterCombo.getSelectedItem().equals("All") ||
               !statusFilterCombo.getSelectedItem().equals("All") ||
               !hostFilterField.getText().trim().isEmpty() ||
               !pathFilterField.getText().trim().isEmpty() ||
               showOnlyWithResponsesCheckbox.isSelected() ||
               showOnlyHighlightedCheckbox.isSelected();
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
     * Toggles the visibility of the filter panel.
     * Updates the filter button text accordingly.
     */
    private void toggleFilterPanel() {
        filterPanelVisible = !filterPanelVisible;
        filterPanel.setVisible(filterPanelVisible);
        showFilterButton.setText(filterPanelVisible ? "Hide Filter" : "Filter");
        
        // Revalidate and repaint to update the layout
        revalidate();
        repaint();
        
        logging.logToOutput("Filter panel " + (filterPanelVisible ? "shown" : "hidden"));
    }
    
    /**
     * Creates a DocumentListener for real-time filtering as user types.
     * 
     * @return DocumentListener that triggers filter application
     */
    private javax.swing.event.DocumentListener createDocumentListener() {
        return new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                // Add slight delay to avoid excessive filtering while typing
                SwingUtilities.invokeLater(() -> applyFilters());
            }
            
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                SwingUtilities.invokeLater(() -> applyFilters());
            }
            
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                SwingUtilities.invokeLater(() -> applyFilters());
            }
        };
    }
    
    /**
     * Applies all active filters to the request table.
     * Filters the stored requests based on method, status, host, path, and checkboxes.
     * Preserves multi-selection during filtering operations.
     */
    private void applyFilters() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::applyFilters);
            return;
        }
        
        try {
            // Remember current multi-selection before applying filters
            int[] selectedRows = requestTable.getSelectedRows();
            List<RequestResponseEntry> previouslySelectedEntries = new ArrayList<>();
            
            // Store the actual entry objects for selected rows
            for (int row : selectedRows) {
                if (row >= 0 && row < tableModel.getRowCount()) {
                    RequestResponseEntry entry = tableModel.getEntryAt(row);
                    if (entry != null) {
                        previouslySelectedEntries.add(entry);
                    }
                }
            }
            
            // Get all stored requests from deduplicator
            List<RequestResponseEntry> allRequests = deduplicator.getStoredRequests();
            List<RequestResponseEntry> filteredRequests = new ArrayList<>();
            
            // Get filter criteria
            String selectedMethod = (String) methodFilterCombo.getSelectedItem();
            String selectedStatus = (String) statusFilterCombo.getSelectedItem();
            String hostFilter = hostFilterField.getText().trim();
            String pathFilter = pathFilterField.getText().trim();
            boolean onlyWithResponses = showOnlyWithResponsesCheckbox.isSelected();
            boolean caseSensitive = caseSensitiveCheckbox.isSelected();
            boolean regexMode = regexModeCheckbox.isSelected();
            
            // Apply filters to each request
            for (RequestResponseEntry entry : allRequests) {
                if (matchesFilters(entry, selectedMethod, selectedStatus, hostFilter, pathFilter, 
                                 onlyWithResponses, caseSensitive, regexMode)) {
                    filteredRequests.add(entry);
                }
            }
            
            // Update table with filtered results
            tableModel.updateData(filteredRequests);
            
            // Restore selection for entries that are still visible after filtering
            if (!previouslySelectedEntries.isEmpty()) {
                for (RequestResponseEntry selectedEntry : previouslySelectedEntries) {
                    // Find the new row index for this entry in the filtered results
                    for (int i = 0; i < filteredRequests.size(); i++) {
                        if (filteredRequests.get(i).equals(selectedEntry)) {
                            requestTable.addRowSelectionInterval(i, i);
                            break;
                        }
                    }
                }
            }
            
            logging.logToOutput(String.format("Applied filters: %d/%d requests shown", 
                filteredRequests.size(), allRequests.size()));
            
        } catch (Exception e) {
            logging.logToError("Error applying filters: " + e.getMessage());
        }
    }
    
    /**
     * Checks if a request entry matches the current filter criteria.
     * 
     * @param entry The request entry to check
     * @param methodFilter Selected method filter
     * @param statusFilter Selected status filter
     * @param hostFilter Host filter text
     * @param pathFilter Path filter text
     * @param onlyWithResponses Whether to show only requests with responses
     * @param caseSensitive Whether text filtering is case sensitive
     * @param regexMode Whether to use regex for text filtering
     * @return true if the entry matches all filters
     */
    private boolean matchesFilters(RequestResponseEntry entry, 
                                 String methodFilter, String statusFilter, String hostFilter, 
                                 String pathFilter, boolean onlyWithResponses, 
                                 boolean caseSensitive, boolean regexMode) {
        try {
            // Method filter
            if (!"All".equals(methodFilter) && !methodFilter.equals(entry.getMethod())) {
                return false;
            }
            
            // Status filter
            if (!"All".equals(statusFilter)) {
                String statusCode = String.valueOf(entry.getStatusCode());
                if (statusFilter.endsWith("xx")) {
                    // Range filter (2xx, 3xx, etc.)
                    String prefix = statusFilter.substring(0, 1);
                    if (!statusCode.startsWith(prefix)) {
                        return false;
                    }
                } else {
                    // Exact status code
                    if (!statusFilter.equals(statusCode)) {
                        return false;
                    }
                }
            }
            
            // Response filter
            if (onlyWithResponses && entry.getResponse() == null) {
                return false;
            }
            
            // Host filter
            if (!hostFilter.isEmpty()) {
                String requestUrl = entry.getRequest().url();
                try {
                    java.net.URL url = new java.net.URL(requestUrl);
                    String host = url.getHost();
                    if (!matchesTextFilter(host, hostFilter, caseSensitive, regexMode)) {
                        return false;
                    }
                } catch (Exception e) {
                    // If URL parsing fails, skip host filtering for this entry
                }
            }
            
            // Path filter
            if (!pathFilter.isEmpty()) {
                String path = entry.getPath();
                if (!matchesTextFilter(path, pathFilter, caseSensitive, regexMode)) {
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            logging.logToError("Error checking filter match: " + e.getMessage());
            return true; // Include entry if filter check fails
        }
    }
    
    /**
     * Checks if a text value matches a filter pattern.
     * 
     * @param text The text to check
     * @param filter The filter pattern
     * @param caseSensitive Whether matching is case sensitive
     * @param regexMode Whether to use regex matching
     * @return true if the text matches the filter
     */
    private boolean matchesTextFilter(String text, String filter, boolean caseSensitive, boolean regexMode) {
        if (text == null || filter.isEmpty()) {
            return true;
        }
        
        try {
            if (regexMode) {
                // Use regex matching
                int flags = caseSensitive ? 0 : java.util.regex.Pattern.CASE_INSENSITIVE;
                return java.util.regex.Pattern.compile(filter, flags).matcher(text).find();
            } else {
                // Use simple substring matching
                String searchText = caseSensitive ? text : text.toLowerCase();
                String searchFilter = caseSensitive ? filter : filter.toLowerCase();
                return searchText.contains(searchFilter);
            }
        } catch (java.util.regex.PatternSyntaxException e) {
            // If regex is invalid, fall back to substring matching
            String searchText = caseSensitive ? text : text.toLowerCase();
            String searchFilter = caseSensitive ? filter : filter.toLowerCase();
            return searchText.contains(searchFilter);
        }
    }
    
    /**
     * Clears all filter settings and shows all requests.
     */
    private void clearFilters() {
        methodFilterCombo.setSelectedIndex(0); // "All"
        statusFilterCombo.setSelectedIndex(0); // "All"
        hostFilterField.setText("");
        pathFilterField.setText("");
        showOnlyWithResponsesCheckbox.setSelected(false);
        showOnlyHighlightedCheckbox.setSelected(false);
        caseSensitiveCheckbox.setSelected(false);
        regexModeCheckbox.setSelected(false);
        
        // Apply filters to show all requests
        applyFilters();
        
        logging.logToOutput("All filters cleared");
    }
    
    /**
     * Sets up the context menu for the request table.
     * Creates menu items similar to Burp's HTTP History context menu.
     */
    private void setupContextMenu() {
        contextMenu = new JPopupMenu();
        
        // Send to submenu
        JMenu sendToMenu = new JMenu("Send to");
        
        JMenuItem sendToRepeater = new JMenuItem("Repeater");
        sendToRepeater.addActionListener(e -> sendSelectedToRepeater());
        sendToMenu.add(sendToRepeater);
        
        JMenuItem sendToIntruder = new JMenuItem("Intruder");
        sendToIntruder.addActionListener(e -> sendSelectedToIntruder());
        sendToMenu.add(sendToIntruder);
        
        JMenuItem sendToComparer = new JMenuItem("Comparer");
        sendToComparer.addActionListener(e -> sendSelectedToComparer());
        sendToMenu.add(sendToComparer);
        
        contextMenu.add(sendToMenu);
        contextMenu.addSeparator();
        
        // Copy options
        JMenuItem copyUrl = new JMenuItem("Copy URL");
        copyUrl.addActionListener(e -> copySelectedRequestUrl());
        contextMenu.add(copyUrl);
        
        JMenuItem copyCurl = new JMenuItem("Copy as cURL");
        copyCurl.addActionListener(e -> copySelectedRequestAsCurl());
        contextMenu.add(copyCurl);
        
        contextMenu.addSeparator();
        
        // Highlight and comment options
        JMenuItem highlightRequest = new JMenuItem("Highlight");
        highlightRequest.addActionListener(e -> highlightSelectedRequest());
        contextMenu.add(highlightRequest);
        
        JMenuItem addComment = new JMenuItem("Add comment");
        addComment.addActionListener(e -> addCommentToSelectedRequest());
        contextMenu.add(addComment);
        
        contextMenu.addSeparator();
        
        // Delete option
        JMenuItem deleteRequest = new JMenuItem("Delete item");
        deleteRequest.addActionListener(e -> deleteSelectedRequest());
        contextMenu.add(deleteRequest);
    }
    
    /**
     * Shows the context menu at the mouse position if a valid row is selected.
     * 
     * @param e The mouse event that triggered the context menu
     */
    private void showContextMenu(java.awt.event.MouseEvent e) {
        // Get the row at the mouse position
        int row = requestTable.rowAtPoint(e.getPoint());
        
        if (row >= 0 && row < requestTable.getRowCount()) {
            // Only change selection if the right-clicked row is not already selected
            // This preserves multi-selection when right-clicking on selected rows
            int[] selectedRows = requestTable.getSelectedRows();
            boolean rowAlreadySelected = false;
            
            for (int selectedRow : selectedRows) {
                if (selectedRow == row) {
                    rowAlreadySelected = true;
                    break;
                }
            }
            
            // If the clicked row is not selected, select only that row
            // If it is selected, keep the current multi-selection
            if (!rowAlreadySelected) {
                requestTable.setRowSelectionInterval(row, row);
            }
            
            // Show context menu
            contextMenu.show(requestTable, e.getX(), e.getY());
        }
    }
    
    /**
     * Sends the selected requests to Burp's Repeater tab.
     * Handles multiple selections by sending each request individually.
     */
    private void sendSelectedToRepeater() {
        List<RequestResponseEntry> entries = getSelectedEntries();
        if (entries.isEmpty() || api == null) return;
        
        int successCount = 0;
        for (RequestResponseEntry entry : entries) {
            if (entry.getRequest() != null) {
                try {
                    api.repeater().sendToRepeater(entry.getRequest());
                    successCount++;
                } catch (Exception e) {
                    logging.logToError("Failed to send request to Repeater: " + entry.getMethod() + " " + entry.getPath() + " - " + e.getMessage());
                }
            }
        }
        
        if (successCount > 0) {
            logging.logToOutput(String.format("Sent %d request(s) to Repeater", successCount));
        }
    }
    
    /**
     * Sends the selected requests to Burp's Intruder tab.
     * Handles multiple selections by sending each request individually.
     */
    private void sendSelectedToIntruder() {
        List<RequestResponseEntry> entries = getSelectedEntries();
        if (entries.isEmpty() || api == null) return;
        
        int successCount = 0;
        for (RequestResponseEntry entry : entries) {
            if (entry.getRequest() != null) {
                try {
                    api.intruder().sendToIntruder(entry.getRequest());
                    successCount++;
                } catch (Exception e) {
                    logging.logToError("Failed to send request to Intruder: " + entry.getMethod() + " " + entry.getPath() + " - " + e.getMessage());
                }
            }
        }
        
        if (successCount > 0) {
            logging.logToOutput(String.format("Sent %d request(s) to Intruder", successCount));
        }
    }
    
    /**
     * Sends the selected requests to Burp's Comparer tab.
     * Handles multiple selections by sending each request individually.
     */
    private void sendSelectedToComparer() {
        List<RequestResponseEntry> entries = getSelectedEntries();
        if (entries.isEmpty() || api == null) return;
        
        int successCount = 0;
        for (RequestResponseEntry entry : entries) {
            if (entry.getRequest() != null) {
                try {
                    api.comparer().sendToComparer(entry.getRequest().toByteArray());
                    successCount++;
                } catch (Exception e) {
                    logging.logToError("Failed to send request to Comparer: " + entry.getMethod() + " " + entry.getPath() + " - " + e.getMessage());
                }
            }
        }
        
        if (successCount > 0) {
            logging.logToOutput(String.format("Sent %d request(s) to Comparer", successCount));
        }
    }
    
    /**
     * Copies the selected request URL to the clipboard.
     */
    private void copySelectedRequestUrl() {
        RequestResponseEntry entry = getSelectedEntry();
        if (entry != null && entry.getRequest() != null) {
            try {
                String url = entry.getRequest().url();
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(url), null);
                logging.logToOutput("URL copied to clipboard: " + url);
            } catch (Exception e) {
                logging.logToError("Failed to copy URL: " + e.getMessage());
            }
        }
    }
    
    /**
     * Copies the selected request as a cURL command to the clipboard.
     */
    private void copySelectedRequestAsCurl() {
        RequestResponseEntry entry = getSelectedEntry();
        if (entry != null && entry.getRequest() != null) {
            try {
                String curl = generateCurlCommand(entry.getRequest());
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(curl), null);
                logging.logToOutput("cURL command copied to clipboard");
            } catch (Exception e) {
                logging.logToError("Failed to copy cURL command: " + e.getMessage());
            }
        }
    }
    
    /**
     * Highlights the selected request (placeholder implementation).
     */
    private void highlightSelectedRequest() {
        RequestResponseEntry entry = getSelectedEntry();
        if (entry != null) {
            // TODO: Implement highlighting functionality
            logging.logToOutput("Highlight functionality not yet implemented for: " + entry.getPath());
            JOptionPane.showMessageDialog(this, "Highlight functionality will be implemented in a future version.", 
                "Feature Not Available", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * Adds a comment to the selected request.
     */
    private void addCommentToSelectedRequest() {
        RequestResponseEntry entry = getSelectedEntry();
        if (entry != null) {
            String comment = JOptionPane.showInputDialog(this, 
                "Enter comment for " + entry.getMethod() + " " + entry.getPath() + ":",
                "Add Comment", JOptionPane.PLAIN_MESSAGE);
            
            if (comment != null && !comment.trim().isEmpty()) {
                // TODO: Store comments with requests
                logging.logToOutput("Comment added: " + comment);
                JOptionPane.showMessageDialog(this, "Comment saved: " + comment, 
                    "Comment Added", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
    
    /**
     * Deletes the selected request from the table.
     */
    private void deleteSelectedRequest() {
        RequestResponseEntry entry = getSelectedEntry();
        if (entry != null) {
            int result = JOptionPane.showConfirmDialog(this,
                "Delete request: " + entry.getMethod() + " " + entry.getPath() + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (result == JOptionPane.YES_OPTION) {
                // TODO: Implement request deletion from deduplicator
                logging.logToOutput("Delete functionality not yet implemented for: " + entry.getPath());
                JOptionPane.showMessageDialog(this, "Delete functionality will be implemented in a future version.", 
                    "Feature Not Available", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
    
    /**
     * Gets the currently selected request entries.
     * 
     * @return List of selected entries (empty if none selected)
     */
    private List<RequestResponseEntry> getSelectedEntries() {
        int[] selectedRows = requestTable.getSelectedRows();
        List<RequestResponseEntry> entries = new ArrayList<>();
        
        for (int row : selectedRows) {
            if (row >= 0 && row < tableModel.getRowCount()) {
                RequestResponseEntry entry = tableModel.getEntryAt(row);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }
        return entries;
    }
    
    /**
     * Gets the currently selected request entry (first selected if multiple).
     * 
     * @return The selected entry or null if none selected
     * @deprecated Use getSelectedEntries() for multi-select support
     */
    @Deprecated
    private RequestResponseEntry getSelectedEntry() {
        List<RequestResponseEntry> entries = getSelectedEntries();
        return entries.isEmpty() ? null : entries.get(0);
    }
    
    /**
     * Generates a cURL command for the given HTTP request.
     * 
     * @param request The HTTP request to convert
     * @return cURL command string
     */
    private String generateCurlCommand(HttpRequest request) {
        StringBuilder curl = new StringBuilder("curl -X ");
        curl.append(request.method());
        
        // Add URL
        curl.append(" '").append(request.url()).append("'");
        
        // Add headers
        for (HttpHeader header : request.headers()) {
            String headerString = header.name() + ": " + header.value();
            if (!headerString.toLowerCase().startsWith("host:") && 
                !headerString.toLowerCase().startsWith("content-length:")) {
                curl.append(" \\\n  -H '").append(headerString).append("'");
            }
        }
        
        // Add body if present
        if (request.body().length() > 0) {
            String body = request.bodyToString();
            // Escape single quotes in body
            body = body.replace("'", "'\"'\"'");
            curl.append(" \\\n  -d '").append(body).append("'");
        }
        
        return curl.toString();
    }
    
    /**
     * Shows the export dialog for exporting requests to various formats.
     */
    private void showExportDialog() {
        ExportDialog dialog = new ExportDialog(SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            String format = dialog.getSelectedFormat();
            boolean includeFullData = dialog.isIncludeFullDataSelected();
            
            // Show file chooser
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Export Requests");
            
            // Set file extension based on format
            String extension = getFileExtension(format);
            fileChooser.setSelectedFile(new java.io.File("unireq_export." + extension));
            
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                java.io.File file = fileChooser.getSelectedFile();
                exportToFile(file, format, includeFullData);
            }
        }
    }
    
    /**
     * Gets the file extension for the given export format.
     * 
     * @param format The export format
     * @return File extension without dot
     */
    private String getFileExtension(String format) {
        switch (format.toLowerCase()) {
            case "csv": return "csv";
            case "html": return "html";
            case "markdown": return "md";
            case "json": return "json";
            default: return "txt";
        }
    }
    
    /**
     * Exports the current table data to a file in the specified format.
     * 
     * @param file The target file
     * @param format The export format
     * @param includeFullData Whether to include full request/response data
     */
    private void exportToFile(java.io.File file, String format, boolean includeFullData) {
        try {
            List<RequestResponseEntry> entries = getCurrentTableEntries();
            
            switch (format.toLowerCase()) {
                case "csv":
                    exportToCsv(file, entries, includeFullData);
                    break;
                case "html":
                    exportToHtml(file, entries, includeFullData);
                    break;
                case "markdown":
                    exportToMarkdown(file, entries, includeFullData);
                    break;
                case "json":
                    exportToJson(file, entries, includeFullData);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported format: " + format);
            }
            
            logging.logToOutput("Successfully exported " + entries.size() + " requests to " + file.getName());
            JOptionPane.showMessageDialog(this, 
                "Successfully exported " + entries.size() + " requests to:\n" + file.getAbsolutePath(),
                "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            
        } catch (Exception e) {
            logging.logToError("Export failed: " + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "Export failed: " + e.getMessage(),
                "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Gets the current entries to export based on selection.
     * If entries are selected, exports only selected entries.
     * Otherwise, exports all currently visible (filtered) entries.
     * 
     * @return List of entries to export
     */
    private List<RequestResponseEntry> getCurrentTableEntries() {
        int[] selectedRows = requestTable.getSelectedRows();
        
        // If there are selected rows, export only selected entries
        if (selectedRows.length > 0) {
            List<RequestResponseEntry> selectedEntries = new ArrayList<>();
            for (int row : selectedRows) {
                if (row >= 0 && row < tableModel.getRowCount()) {
                    RequestResponseEntry entry = tableModel.getEntryAt(row);
                    if (entry != null) {
                        selectedEntries.add(entry);
                    }
                }
            }
            return selectedEntries;
        }
        
        // Otherwise, export all visible entries (after filtering)
        List<RequestResponseEntry> entries = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            RequestResponseEntry entry = tableModel.getEntryAt(i);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }
    
    /**
     * Exports entries to CSV format.
     * 
     * @param file The target file
     * @param entries The entries to export
     * @param includeFullData Whether to include full request/response data
     */
    private void exportToCsv(java.io.File file, List<RequestResponseEntry> entries, boolean includeFullData) throws Exception {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
            // Write header
            if (includeFullData) {
                writer.println("Method,URL,Status,Timestamp,Request,Response");
            } else {
                writer.println("Method,URL,Status,Timestamp");
            }
            
            // Write data
            for (RequestResponseEntry entry : entries) {
                writer.print(escapeCSV(entry.getMethod()));
                writer.print(",");
                writer.print(escapeCSV(entry.getRequest() != null ? entry.getRequest().url() : ""));
                writer.print(",");
                writer.print(entry.getStatusCode());
                writer.print(",");
                writer.print(escapeCSV(entry.getFormattedTimestamp()));
                
                if (includeFullData) {
                    writer.print(",");
                    writer.print(escapeCSV(entry.getRequest() != null ? entry.getRequest().toString() : ""));
                    writer.print(",");
                    writer.print(escapeCSV(entry.getResponse() != null ? entry.getResponse().toString() : ""));
                }
                writer.println();
            }
        }
    }
    
    /**
     * Exports entries to HTML format.
     * 
     * @param file The target file
     * @param entries The entries to export
     * @param includeFullData Whether to include full request/response data
     */
    private void exportToHtml(java.io.File file, List<RequestResponseEntry> entries, boolean includeFullData) throws Exception {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
            writer.println("<!DOCTYPE html>");
            writer.println("<html><head>");
            writer.println("<title>UniReq Export</title>");
            writer.println("<style>");
            writer.println("body { font-family: Arial, sans-serif; margin: 20px; }");
            writer.println("table { border-collapse: collapse; width: 100%; }");
            writer.println("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
            writer.println("th { background-color: #f2f2f2; }");
            writer.println("tr:nth-child(even) { background-color: #f9f9f9; }");
            writer.println(".status-2xx { color: green; font-weight: bold; }");
            writer.println(".status-3xx { color: orange; font-weight: bold; }");
            writer.println(".status-4xx { color: red; font-weight: bold; }");
            writer.println(".status-5xx { color: darkred; font-weight: bold; }");
            writer.println("pre { background: #f4f4f4; padding: 10px; overflow-x: auto; }");
            writer.println("</style>");
            writer.println("</head><body>");
            writer.println("<h1>UniReq Export Report</h1>");
            writer.println("<p>Generated: " + new java.util.Date() + "</p>");
            writer.println("<p>Total Requests: " + entries.size() + "</p>");
            
            writer.println("<table>");
            writer.println("<thead><tr>");
            writer.println("<th>Method</th><th>URL</th><th>Status</th><th>Timestamp</th>");
            if (includeFullData) {
                writer.println("<th>Request</th><th>Response</th>");
            }
            writer.println("</tr></thead><tbody>");
            
            for (RequestResponseEntry entry : entries) {
                writer.println("<tr>");
                writer.println("<td>" + escapeHtml(entry.getMethod()) + "</td>");
                writer.println("<td>" + escapeHtml(entry.getRequest() != null ? entry.getRequest().url() : "") + "</td>");
                
                String statusClass = getStatusClass(parseStatusCode(entry.getStatusCode()));
                writer.println("<td class=\"" + statusClass + "\">" + entry.getStatusCode() + "</td>");
                writer.println("<td>" + escapeHtml(entry.getFormattedTimestamp()) + "</td>");
                
                if (includeFullData) {
                    writer.println("<td><pre>" + escapeHtml(entry.getRequest() != null ? entry.getRequest().toString() : "") + "</pre></td>");
                    writer.println("<td><pre>" + escapeHtml(entry.getResponse() != null ? entry.getResponse().toString() : "") + "</pre></td>");
                }
                writer.println("</tr>");
            }
            
            writer.println("</tbody></table>");
            writer.println("</body></html>");
        }
    }
    
    /**
     * Exports entries to Markdown format.
     * 
     * @param file The target file
     * @param entries The entries to export
     * @param includeFullData Whether to include full request/response data
     */
    private void exportToMarkdown(java.io.File file, List<RequestResponseEntry> entries, boolean includeFullData) throws Exception {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
            writer.println("# UniReq Export Report");
            writer.println();
            writer.println("**Generated:** " + new java.util.Date());
            writer.println("**Total Requests:** " + entries.size());
            writer.println();
            
            // Table header
            if (includeFullData) {
                writer.println("| Method | URL | Status | Timestamp | Request | Response |");
                writer.println("|--------|-----|--------|-----------|---------|----------|");
            } else {
                writer.println("| Method | URL | Status | Timestamp |");
                writer.println("|--------|-----|--------|-----------|");
            }
            
            // Table data
            for (RequestResponseEntry entry : entries) {
                writer.print("| " + escapeMarkdown(entry.getMethod()));
                writer.print(" | " + escapeMarkdown(entry.getRequest() != null ? entry.getRequest().url() : ""));
                writer.print(" | " + entry.getStatusCode());
                writer.print(" | " + escapeMarkdown(entry.getFormattedTimestamp()));
                
                if (includeFullData) {
                    writer.print(" | ```\n" + escapeMarkdown(entry.getRequest() != null ? entry.getRequest().toString() : "") + "\n```");
                    writer.print(" | ```\n" + escapeMarkdown(entry.getResponse() != null ? entry.getResponse().toString() : "") + "\n```");
                }
                writer.println(" |");
            }
        }
    }
    
    /**
     * Exports entries to JSON format with validation.
     * 
     * @param file The target file
     * @param entries The entries to export
     * @param includeFullData Whether to include full request/response data
     */
    private void exportToJson(java.io.File file, List<RequestResponseEntry> entries, boolean includeFullData) throws Exception {
        // First, generate JSON in memory for validation
        StringBuilder jsonBuilder = new StringBuilder();
        generateJsonContent(jsonBuilder, entries, includeFullData);
        
        // Validate JSON structure before writing to file
        String jsonContent = jsonBuilder.toString();
        validateJsonStructure(jsonContent);
        
        // Write validated JSON to file
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
            writer.write(jsonContent);
        }
        
        logging.logToOutput("JSON export validated and written successfully");
    }
    
    /**
     * Generates JSON content into a StringBuilder.
     * 
     * @param jsonBuilder The StringBuilder to append JSON content to
     * @param entries The entries to export
     * @param includeFullData Whether to include full request/response data
     */
    private void generateJsonContent(StringBuilder jsonBuilder, List<RequestResponseEntry> entries, boolean includeFullData) {
        jsonBuilder.append("{\n");
        jsonBuilder.append("  \"export_info\": {\n");
        jsonBuilder.append("    \"tool\": \"UniReq\",\n");
        jsonBuilder.append("    \"generated\": \"").append(escapeJson(new java.util.Date().toString())).append("\",\n");
        jsonBuilder.append("    \"total_requests\": ").append(entries.size()).append(",\n");
        jsonBuilder.append("    \"include_full_data\": ").append(includeFullData).append("\n");
        jsonBuilder.append("  },\n");
        jsonBuilder.append("  \"requests\": [\n");
        
        for (int i = 0; i < entries.size(); i++) {
            RequestResponseEntry entry = entries.get(i);
            jsonBuilder.append("    {\n");
            jsonBuilder.append("      \"method\": \"").append(escapeJson(entry.getMethod())).append("\",\n");
            jsonBuilder.append("      \"url\": \"").append(escapeJson(entry.getRequest() != null ? entry.getRequest().url() : "")).append("\",\n");
            jsonBuilder.append("      \"status\": ").append(entry.getStatusCode()).append(",\n");
            jsonBuilder.append("      \"timestamp\": \"").append(escapeJson(entry.getFormattedTimestamp())).append("\"");
            
            // Add full data fields if requested - fix comma placement
            if (includeFullData) {
                jsonBuilder.append(",\n");  // Add comma to previous field
                jsonBuilder.append("      \"request\": \"").append(escapeJson(entry.getRequest() != null ? entry.getRequest().toString() : "")).append("\",\n");
                jsonBuilder.append("      \"response\": \"").append(escapeJson(entry.getResponse() != null ? entry.getResponse().toString() : "")).append("\"");
            }
            
            jsonBuilder.append("\n    }");
            
            // Add comma between entries, but not after the last entry
            if (i < entries.size() - 1) {
                jsonBuilder.append(",");
            }
            jsonBuilder.append("\n");
        }
        
        jsonBuilder.append("  ]\n");
        jsonBuilder.append("}\n");
    }
    
    /**
     * Validates JSON structure by checking for common syntax errors.
     * This is a lightweight validation to catch obvious issues before file write.
     * 
     * @param jsonContent The JSON content to validate
     * @throws Exception If validation fails
     */
    private void validateJsonStructure(String jsonContent) throws Exception {
        // Basic structural validation
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            throw new Exception("JSON content is empty");
        }
        
        // Check balanced braces and brackets
        int braceCount = 0;
        int bracketCount = 0;
        boolean inString = false;
        boolean escapeNext = false;
        
        for (int i = 0; i < jsonContent.length(); i++) {
            char c = jsonContent.charAt(i);
            
            if (escapeNext) {
                escapeNext = false;
                continue;
            }
            
            if (c == '\\') {
                escapeNext = true;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                switch (c) {
                    case '{':
                        braceCount++;
                        break;
                    case '}':
                        braceCount--;
                        break;
                    case '[':
                        bracketCount++;
                        break;
                    case ']':
                        bracketCount--;
                        break;
                }
            }
        }
        
        if (braceCount != 0) {
            throw new Exception("JSON validation failed: Unbalanced braces (missing " + Math.abs(braceCount) + " " + (braceCount > 0 ? "closing" : "opening") + " braces)");
        }
        
        if (bracketCount != 0) {
            throw new Exception("JSON validation failed: Unbalanced brackets (missing " + Math.abs(bracketCount) + " " + (bracketCount > 0 ? "closing" : "opening") + " brackets)");
        }
        
        // Check for trailing commas (basic check)
        String[] lines = jsonContent.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.endsWith(",")) {
                // Check if next non-empty line closes an object or array
                for (int j = i + 1; j < lines.length; j++) {
                    String nextLine = lines[j].trim();
                    if (!nextLine.isEmpty()) {
                        if (nextLine.startsWith("}") || nextLine.startsWith("]")) {
                            throw new Exception("JSON validation failed: Trailing comma detected at line " + (i + 1) + ": " + line);
                        }
                        break;
                    }
                }
            }
        }
        
        logging.logToOutput("JSON structure validation passed");
    }
    
    /**
     * Escapes special characters for CSV format.
     */
    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    
    /**
     * Escapes special characters for HTML format.
     */
    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
    
    /**
     * Escapes special characters for Markdown format.
     */
    private String escapeMarkdown(String value) {
        if (value == null) return "";
        return value.replace("|", "\\|")
                   .replace("\n", " ")
                   .replace("\r", "");
    }
    
    /**
     * Escapes special characters for JSON format.
     * Handles all JSON control characters and ensures valid UTF-8 encoding.
     */
    private String escapeJson(String value) {
        if (value == null) return "";
        
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                default:
                    // Handle control characters (0x00-0x1F)
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                    break;
            }
        }
        return escaped.toString();
    }
    
    /**
     * Parses status code string to integer.
     */
    private int parseStatusCode(String statusCode) {
        try {
            return Integer.parseInt(statusCode);
        } catch (NumberFormatException e) {
            return 0; // Default for unparseable status codes
        }
    }
    
    /**
     * Gets CSS class for status code styling.
     */
    private String getStatusClass(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) return "status-2xx";
        if (statusCode >= 300 && statusCode < 400) return "status-3xx";
        if (statusCode >= 400 && statusCode < 500) return "status-4xx";
        if (statusCode >= 500) return "status-5xx";
        return "";
    }
    
    /**
     * Export dialog for selecting format and options.
     */
    private static class ExportDialog extends JDialog {
        private JComboBox<String> formatCombo;
        private JCheckBox includeFullDataCheckbox;
        private boolean confirmed = false;
        
        public ExportDialog(java.awt.Window parent) {
            super(parent, "Export Requests", ModalityType.APPLICATION_MODAL);
            initializeDialog();
        }
        
        private void initializeDialog() {
            setLayout(new BorderLayout());
            setSize(400, 200);
            setLocationRelativeTo(getParent());
            
            // Main panel
            JPanel mainPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            
            // Format selection
            gbc.gridx = 0; gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.WEST;
            mainPanel.add(new JLabel("Export Format:"), gbc);
            
            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
            formatCombo = new JComboBox<>(new String[]{"CSV", "HTML", "Markdown", "JSON"});
            formatCombo.setSelectedIndex(0);
            mainPanel.add(formatCombo, gbc);
            
            // Include full data option
            gbc.gridx = 0; gbc.gridy = 1;
            gbc.gridwidth = 2;
            includeFullDataCheckbox = new JCheckBox("Include full request/response data");
            includeFullDataCheckbox.setToolTipText("Include complete HTTP request and response content in export");
            mainPanel.add(includeFullDataCheckbox, gbc);
            
            // Button panel
            JPanel buttonPanel = new JPanel(new FlowLayout());
            JButton exportButton = new JButton("Export");
            JButton cancelButton = new JButton("Cancel");
            
            exportButton.addActionListener(e -> {
                confirmed = true;
                setVisible(false);
            });
            
            cancelButton.addActionListener(e -> {
                confirmed = false;
                setVisible(false);
            });
            
            buttonPanel.add(exportButton);
            buttonPanel.add(cancelButton);
            
            add(mainPanel, BorderLayout.CENTER);
            add(buttonPanel, BorderLayout.SOUTH);
            
            // Set default button
            getRootPane().setDefaultButton(exportButton);
        }
        
        public boolean isConfirmed() { return confirmed; }
        public String getSelectedFormat() { return (String) formatCombo.getSelectedItem(); }
        public boolean isIncludeFullDataSelected() { return includeFullDataCheckbox.isSelected(); }
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
        private List<RequestResponseEntry> entries = new ArrayList<>();
        
        /**
         * Updates the table data with new entries.
         * Selection preservation is handled by the calling methods to avoid
         * interference with multi-selection behavior.
         * 
         * @param newEntries The new list of request entries
         */
        public void updateData(List<RequestResponseEntry> newEntries) {
            this.entries = new ArrayList<>(newEntries);
            // Fire table data changed - selection preservation is handled by caller
            fireTableDataChanged();
        }
        
        /**
         * Gets the entry at the specified row.
         * 
         * @param row The row index
         * @return The request entry or null if invalid
         */
        public RequestResponseEntry getEntryAt(int row) {
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
            
            RequestResponseEntry entry = entries.get(rowIndex);
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