package com.burp.unireq.extension;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.extension.ExtensionUnloadingHandler;
import burp.api.montoya.logging.Logging;

import com.burp.unireq.core.RequestDeduplicator;
import com.burp.unireq.ui.UniReqGui;

/**
 * UniReq HTTP Request Deduplicator - Burp Suite Extension entry point.
 *
 * @author Harshit Shah
 */
public class UniReqExtension implements BurpExtension {

    private static final String EXTENSION_NAME = "UniReq - HTTP Request Deduplicator";
    private static final String EXTENSION_VERSION = "1.0.0";

    private MontoyaApi api;
    private Logging logging;
    private RequestDeduplicator deduplicator;
    private UniReqGui gui;
    private RequestFingerprintListener proxyListener;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        this.logging = api.logging();

        api.extension().setName(EXTENSION_NAME);
        logging.logToOutput("Initializing " + EXTENSION_NAME + " v" + EXTENSION_VERSION);

        try {
            this.deduplicator = new RequestDeduplicator(logging);
            this.gui = new UniReqGui(deduplicator, logging);

            api.userInterface().registerSuiteTab("UniReq", gui.getUiComponent());
            gui.setApi(api);

            this.proxyListener = new RequestFingerprintListener(deduplicator, gui, logging);
            api.proxy().registerRequestHandler(proxyListener);
            api.proxy().registerResponseHandler(proxyListener);

            api.extension().registerUnloadingHandler(new ExtensionUnloadingHandler() {
                @Override
                public void extensionUnloaded() {
                    cleanup();
                }
            });

            logging.logToOutput("UniReq extension initialized successfully");

        } catch (Exception e) {
            logging.logToError("Failed to initialize UniReq extension: " + e.getMessage());
        }
    }

    private void cleanup() {
        try {
            if (gui != null) {
                gui.cleanup();
            }
            if (deduplicator != null) {
                deduplicator.clearFingerprints();
            }
            logging.logToOutput("UniReq extension unloaded successfully");
        } catch (Exception e) {
            logging.logToError("Error during extension cleanup: " + e.getMessage());
        }
    }
}
