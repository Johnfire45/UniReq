package com.burp.unireq.ui.components;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
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
            // Create request and response editors
            requestEditor = api.userInterface().createHttpRequestEditor();
            responseEditor = api.userInterface().createHttpResponseEditor();
            
            // Create panels with titles
            requestPanel = new JPanel(new BorderLayout());
            requestPanel.setBorder(BorderFactory.createTitledBorder("Request"));
            requestPanel.add(requestEditor.uiComponent(), BorderLayout.CENTER);
            
            responsePanel = new JPanel(new BorderLayout());
            responsePanel.setBorder(BorderFactory.createTitledBorder("Response"));
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
            System.err.println("Failed to initialize request/response viewers: " + e.getMessage());
            e.printStackTrace();
            
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
                    
                    // Update response viewer
                    if (entry.getResponse() != null) {
                        responseEditor.setResponse(entry.getResponse());
                    } else {
                        responseEditor.setResponse(null);
                    }
                } else {
                    // Clear both viewers
                    requestEditor.setRequest(null);
                    responseEditor.setResponse(null);
                }
                
            } catch (Exception e) {
                System.err.println("Error updating viewers: " + e.getMessage());
                e.printStackTrace();
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
} 