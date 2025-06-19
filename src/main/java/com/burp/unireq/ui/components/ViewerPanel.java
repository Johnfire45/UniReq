package com.burp.unireq.ui.components;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
import burp.api.montoya.ui.editor.EditorOptions;
import com.burp.unireq.model.RequestResponseEntry;

import javax.swing.*;
import java.awt.*;

/**
 * ViewerPanel - Request/Response viewer component for UniReq extension
 * 
 * This component displays HTTP request and response content using Burp's
 * native editors in a horizontal split pane layout. It provides read-only
 * viewing of HTTP transactions with proper error handling.
 * 
 * Features:
 * - Burp's native HttpRequestEditor and HttpResponseEditor
 * - Horizontal split pane with resizable divider
 * - Titled borders for clear section identification
 * - Thread-safe viewer updates
 * - Graceful handling of missing responses
 * - Proper cleanup of resources
 * 
 * @author Harshit Shah
 */
public class ViewerPanel extends JPanel {
    
    // Burp API reference
    private MontoyaApi api;
    
    // Viewer components
    private HttpRequestEditor requestEditor;
    private HttpResponseEditor responseEditor;
    private JSplitPane viewerSplitPane;
    
    // Panels for viewers
    private JPanel requestPanel;
    private JPanel responsePanel;
    private JLabel placeholderLabel;
    
    // Target host label for response panel
    private JLabel targetHostLabel;
    
    // Initialization state
    private boolean initialized = false;
    
    /**
     * Constructor initializes the viewer panel with a placeholder.
     * The actual viewers will be created when the API is set.
     */
    public ViewerPanel() {
        initializePlaceholder();
    }
    
    /**
     * Constructor that immediately initializes viewers if API is available.
     * 
     * @param api The Montoya API instance for creating editors
     */
    public ViewerPanel(MontoyaApi api) {
        this.api = api;
        if (api != null) {
            initializeViewers();
        } else {
            initializePlaceholder();
        }
    }
    
    /**
     * Initializes the panel with a placeholder message.
     */
    private void initializePlaceholder() {
        setLayout(new BorderLayout());
        placeholderLabel = new JLabel("Request/Response viewers will be available once extension is loaded", SwingConstants.CENTER);
        placeholderLabel.setFont(placeholderLabel.getFont().deriveFont(Font.ITALIC));
        add(placeholderLabel, BorderLayout.CENTER);
    }
    
    /**
     * Sets the Montoya API and initializes the viewers.
     * This method is thread-safe and can be called from any thread.
     * 
     * @param api The Montoya API instance
     */
    public void setApi(MontoyaApi api) {
        this.api = api;
        
        if (api != null) {
            SwingUtilities.invokeLater(() -> {
                initializeViewers();
            });
        }
    }
    
    /**
     * Initializes the request and response viewers using Burp's API.
     * This method must be called from the EDT.
     */
    private void initializeViewers() {
        if (api == null || initialized) {
            return;
        }
        
        try {
            // Create request and response editors (read-only mode)
            requestEditor = api.userInterface().createHttpRequestEditor(EditorOptions.READ_ONLY);
            responseEditor = api.userInterface().createHttpResponseEditor(EditorOptions.READ_ONLY);
            
            // Create panels with titles
            requestPanel = new JPanel(new BorderLayout());
            requestPanel.setBorder(BorderFactory.createTitledBorder("Request"));
            requestPanel.add(requestEditor.uiComponent(), BorderLayout.CENTER);
            
            // Create target host label
            targetHostLabel = new JLabel("Target: (no request selected)", SwingConstants.CENTER);
            targetHostLabel.setFont(targetHostLabel.getFont().deriveFont(Font.BOLD));
            targetHostLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            
            responsePanel = new JPanel(new BorderLayout());
            responsePanel.setBorder(BorderFactory.createTitledBorder("Response"));
            responsePanel.add(targetHostLabel, BorderLayout.NORTH); // Add target host label at top
            responsePanel.add(responseEditor.uiComponent(), BorderLayout.CENTER);
            
            // Create viewer split pane (request on left, response on right)
            viewerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            viewerSplitPane.setLeftComponent(requestPanel);
            viewerSplitPane.setRightComponent(responsePanel);
            viewerSplitPane.setDividerLocation(0.5);
            viewerSplitPane.setResizeWeight(0.5);
            
            // Replace placeholder with actual viewers
            removeAll();
            setLayout(new BorderLayout());
            add(viewerSplitPane, BorderLayout.CENTER);
            
            // Mark as initialized
            initialized = true;
            
            // Refresh the display
            revalidate();
            repaint();
            
        } catch (Exception e) {
            // Log error silently - don't expose internal errors to user
            // Error: Failed to initialize request/response viewers
            
            // Show error message
            removeAll();
            setLayout(new BorderLayout());
            JLabel errorLabel = new JLabel("Failed to initialize viewers: " + e.getMessage(), SwingConstants.CENTER);
            errorLabel.setForeground(Color.RED);
            add(errorLabel, BorderLayout.CENTER);
            revalidate();
            repaint();
        }
    }
    
    /**
     * Updates the viewers with a new request/response entry.
     * This method is thread-safe and can be called from any thread.
     * 
     * @param entry The request/response entry to display, or null to clear viewers
     */
    public void updateViewers(RequestResponseEntry entry) {
        SwingUtilities.invokeLater(() -> {
            if (!initialized || requestEditor == null || responseEditor == null) {
                return;
            }
            
            try {
                if (entry != null) {
                    // Update request viewer
                    requestEditor.setRequest(entry.getRequest());
                    
                    // Update target host label
                    updateTargetHostLabel(entry);
                    
                    // Update response viewer
                    if (entry.getResponse() != null) {
                        responseEditor.setResponse(entry.getResponse());
                    } else {
                        responseEditor.setResponse(null);
                    }
                } else {
                    // Clear both viewers and target host label
                    requestEditor.setRequest(null);
                    responseEditor.setResponse(null);
                    updateTargetHostLabel(null);
                }
                
            } catch (Exception e) {
                // Log error silently - don't expose internal errors to user
                // Error: Error updating viewers
            }
        });
    }
    
    /**
     * Clears both request and response viewers.
     * This method is thread-safe and can be called from any thread.
     */
    public void clearViewers() {
        updateViewers(null);
    }
    
    /**
     * Checks if the viewers have been initialized.
     * 
     * @return true if viewers are initialized and ready to use
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Gets the request editor instance.
     * 
     * @return The HttpRequestEditor, or null if not initialized
     */
    public HttpRequestEditor getRequestEditor() {
        return requestEditor;
    }
    
    /**
     * Gets the response editor instance.
     * 
     * @return The HttpResponseEditor, or null if not initialized
     */
    public HttpResponseEditor getResponseEditor() {
        return responseEditor;
    }
    
    /**
     * Gets the split pane component for advanced layout operations.
     * 
     * @return The JSplitPane containing the viewers, or null if not initialized
     */
    public JSplitPane getSplitPane() {
        return viewerSplitPane;
    }
    
    /**
     * Sets the divider location of the split pane.
     * This method is thread-safe and can be called from any thread.
     * 
     * @param location The divider location (0.0 to 1.0 for proportional, or pixel value)
     */
    public void setDividerLocation(double location) {
        SwingUtilities.invokeLater(() -> {
            if (viewerSplitPane != null) {
                viewerSplitPane.setDividerLocation(location);
            }
        });
    }
    
    /**
     * Sets the resize weight of the split pane.
     * This method is thread-safe and can be called from any thread.
     * 
     * @param weight The resize weight (0.0 to 1.0)
     */
    public void setResizeWeight(double weight) {
        SwingUtilities.invokeLater(() -> {
            if (viewerSplitPane != null) {
                viewerSplitPane.setResizeWeight(weight);
            }
        });
    }
    
    /**
     * Cleanup method to release resources.
     * Should be called when the component is no longer needed.
     */
    public void cleanup() {
        SwingUtilities.invokeLater(() -> {
            if (requestEditor != null) {
                requestEditor = null;
            }
            if (responseEditor != null) {
                responseEditor = null;
            }
            
            initialized = false;
        });
    }
    
    /**
     * Updates the target host label based on the given entry.
     * This method is thread-safe and can be called from any thread.
     * 
     * @param entry The request/response entry to update the target host label with, or null to clear the label
     */
    private void updateTargetHostLabel(RequestResponseEntry entry) {
        if (targetHostLabel != null) {
            if (entry != null && entry.getRequest() != null) {
                try {
                    // Get host information from the request
                    String host = entry.getRequest().httpService().host();
                    int port = entry.getRequest().httpService().port();
                    boolean isSecure = entry.getRequest().httpService().secure();
                    
                    // Build the target URL string
                    String protocol = isSecure ? "https" : "http";
                    String targetUrl;
                    
                    // Only show port if it's not the default port for the protocol
                    if ((isSecure && port == 443) || (!isSecure && port == 80)) {
                        targetUrl = String.format("%s://%s", protocol, host);
                    } else {
                        targetUrl = String.format("%s://%s:%d", protocol, host, port);
                    }
                    
                    targetHostLabel.setText("Target: " + targetUrl);
                } catch (Exception e) {
                    // Fallback to simple host display if there's any error
                    targetHostLabel.setText("Target: " + entry.getRequest().httpService().host());
                }
            } else {
                targetHostLabel.setText("Target: (no request selected)");
            }
        }
    }
} 