package com.burp.unireq.extension;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.extension.ExtensionUnloadingHandler;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.ui.UserInterface;

import com.burp.unireq.core.RequestDeduplicator;
import com.burp.unireq.ui.UniReqGui;

/**
 * UniReq HTTP Request Deduplicator Extension
 * 
 * This is the main entry point for the Burp Suite extension. It implements BurpExtension
 * which is the modern Montoya API interface (replacing the legacy IBurpExtender).
 * 
 * Purpose:
 * - Initializes the extension when loaded by Burp Suite
 * - Sets up the HTTP request filtering system
 * - Registers GUI components and event listeners
 * - Provides a clean shutdown mechanism
 * 
 * The extension filters duplicate HTTP requests by computing unique fingerprints
 * based on method, path, and request content, then maintaining a thread-safe
 * set of seen fingerprints to identify and handle duplicates.
 */
public class UniReqExtension implements BurpExtension {
    
    // Extension metadata constants
    private static final String EXTENSION_NAME = "UniReq - HTTP Request Deduplicator";
    private static final String EXTENSION_VERSION = "1.0.0";
    
    // Core components
    private MontoyaApi api;
    private Logging logging;
    private RequestDeduplicator deduplicator;
    private UniReqGui gui;
    private RequestFingerprintListener proxyListener;

    /**
     * Extension initialization method called by Burp Suite when the extension is loaded.
     * 
     * This method:
     * 1. Stores the Montoya API reference for use throughout the extension
     * 2. Sets up logging for debugging and monitoring
     * 3. Initializes the request deduplication system
     * 4. Creates and registers the GUI components
     * 5. Registers HTTP proxy listeners to intercept requests
     * 6. Sets up proper cleanup handlers
     * 
     * @param api The Montoya API instance provided by Burp Suite
     */
    @Override
    public void initialize(MontoyaApi api) {
        // Store API reference and set up logging
        this.api = api;
        this.logging = api.logging();
        
        // Set extension name in Burp's Extensions tab
        api.extension().setName(EXTENSION_NAME);
        
        // Log successful initialization start
        logging.logToOutput("Initializing " + EXTENSION_NAME + " v" + EXTENSION_VERSION);
        
        try {
            // Initialize the core deduplication engine
            // This handles fingerprint computation and duplicate detection
            this.deduplicator = new RequestDeduplicator(logging);
            
            // Create the GUI tab for user interaction
            // This provides controls for enabling/disabling filtering and viewing statistics
            this.gui = new UniReqGui(deduplicator, logging);
            
            // Register the GUI tab with Burp's interface
            api.userInterface().registerSuiteTab("UniReq", gui.getUiComponent());
            
            // Provide API access to GUI for request/response editors
            gui.setApi(api);
            
            // Create and register the proxy listener
            // This intercepts HTTP requests and responses and applies deduplication logic
            this.proxyListener = new RequestFingerprintListener(deduplicator, gui, logging);
            api.proxy().registerRequestHandler(proxyListener);
            api.proxy().registerResponseHandler(proxyListener);
            
            // Register cleanup handler for proper shutdown
            api.extension().registerUnloadingHandler(new ExtensionUnloadingHandler() {
                @Override
                public void extensionUnloaded() {
                    cleanup();
                }
            });
            
            logging.logToOutput("UniReq extension initialized successfully!");
            logging.logToOutput("- Request deduplication: Ready");
            logging.logToOutput("- GUI tab: Registered");
            logging.logToOutput("- Proxy listener: Active");
            
        } catch (Exception e) {
            // Log any initialization errors
            logging.logToError("Failed to initialize UniReq extension: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Cleanup method called when the extension is unloaded.
     * 
     * This ensures proper resource cleanup including:
     * - Clearing stored fingerprints from memory
     * - Unregistering listeners (handled automatically by Burp)
     * - Logging shutdown status
     */
    private void cleanup() {
        try {
            if (gui != null) {
                // Stop auto-refresh timer and cleanup GUI resources
                gui.cleanup();
            }
            if (deduplicator != null) {
                // Clear all stored fingerprints to free memory
                deduplicator.clearFingerprints();
            }
            logging.logToOutput("UniReq extension unloaded successfully");
        } catch (Exception e) {
            logging.logToError("Error during extension cleanup: " + e.getMessage());
        }
    }
} 