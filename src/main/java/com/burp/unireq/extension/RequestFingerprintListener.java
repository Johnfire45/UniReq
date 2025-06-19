package com.burp.unireq.extension;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.proxy.http.InterceptedRequest;
import burp.api.montoya.proxy.http.InterceptedResponse;
import burp.api.montoya.proxy.http.ProxyRequestHandler;
import burp.api.montoya.proxy.http.ProxyResponseHandler;
import burp.api.montoya.proxy.http.ProxyRequestReceivedAction;
import burp.api.montoya.proxy.http.ProxyRequestToBeSentAction;
import burp.api.montoya.proxy.http.ProxyResponseReceivedAction;
import burp.api.montoya.proxy.http.ProxyResponseToBeSentAction;

import com.burp.unireq.core.RequestDeduplicator;
import com.burp.unireq.ui.UniReqGui;

/**
 * RequestFingerprintListener - HTTP Request/Response Interceptor for Deduplication
 * 
 * This class implements ProxyRequestHandler and ProxyResponseHandler to intercept 
 * HTTP requests and responses passing through Burp's proxy. It works with the 
 * RequestDeduplicator to identify and handle duplicate requests based on computed 
 * fingerprints, and stores request/response pairs for GUI display.
 * 
 * Purpose:
 * - Intercepts all HTTP requests before they are sent
 * - Intercepts HTTP responses to complete request/response pairs
 * - Applies deduplication logic using RequestDeduplicator
 * - Stores unique request/response pairs for HTTP History-style GUI
 * - Updates GUI statistics and request list in real-time
 * - Decides whether to allow, block, or annotate requests
 * 
 * Integration:
 * - Registered with Burp's proxy via api.proxy().registerRequestHandler()
 * - Registered with Burp's proxy via api.proxy().registerResponseHandler()
 * - Communicates with RequestDeduplicator for duplicate detection and storage
 * - Updates UniReqGui for real-time statistics and request list display
 */
public class RequestFingerprintListener implements ProxyRequestHandler, ProxyResponseHandler {
    
    // Core components
    private final RequestDeduplicator deduplicator;
    private final UniReqGui gui;
    private final Logging logging;
    
    /**
     * Constructor initializes the listener with required dependencies.
     * 
     * @param deduplicator The deduplication engine for fingerprint computation and duplicate detection
     * @param gui The GUI component for real-time statistics updates
     * @param logging Burp's logging interface for debug output
     */
    public RequestFingerprintListener(RequestDeduplicator deduplicator, UniReqGui gui, Logging logging) {
        this.deduplicator = deduplicator;
        this.gui = gui;
        this.logging = logging;
        
        logging.logToOutput("RequestFingerprintListener initialized and ready to intercept requests");
    }
    
    /**
     * Handles HTTP requests when they are received by the proxy.
     * 
     * This method is called by Burp for every HTTP request passing through the proxy.
     * It performs the following steps:
     * 
     * 1. Extracts the HTTP request from the intercepted request
     * 2. Uses RequestDeduplicator to check if the request is unique
     * 3. Updates GUI statistics in real-time
     * 4. Decides the appropriate action based on deduplication result
     * 
     * Actions:
     * - CONTINUE: Allow unique requests to proceed normally
     * - DROP: Block duplicate requests (optional - currently allows all for visibility)
     * - INTERCEPT: Hold requests for manual review (not used in this implementation)
     * 
     * @param interceptedRequest The intercepted HTTP request from Burp's proxy
     * @return ProxyRequestReceivedAction indicating how to handle the request
     */
    @Override
    public ProxyRequestReceivedAction handleRequestReceived(InterceptedRequest interceptedRequest) {
        try {
            // DEBUG: Add debug print to verify interception is working
            System.out.println(">>> UniReq: Intercepted HTTP request");
            
            // InterceptedRequest extends HttpRequest, so we can use it directly
            // Log request details for debugging (without sensitive content)
            String requestInfo = String.format("%s %s", interceptedRequest.method(), interceptedRequest.path());
            logging.logToOutput("Processing request: " + requestInfo);
            
            // Check if this request is unique using the deduplicator
            boolean isUnique = deduplicator.isUniqueRequest(interceptedRequest);
            
            // Update GUI statistics in real-time
            // This ensures the user sees current statistics immediately
            if (gui != null) {
                gui.updateStatistics();
            }
            
            // Determine action based on uniqueness and filtering settings
            if (isUnique) {
                // Allow unique requests to proceed
                logging.logToOutput("Allowing unique request: " + requestInfo);
                return ProxyRequestReceivedAction.continueWith(interceptedRequest);
            } else {
                // Handle duplicate request
                logging.logToOutput("Duplicate request detected: " + requestInfo);
                
                // For now, we'll allow duplicates through but annotate them
                // This provides visibility while maintaining functionality
                // In a more aggressive implementation, you could return:
                // return ProxyRequestReceivedAction.drop();
                
                // Add annotation to help identify duplicates in Burp's interface
                HttpRequest annotatedRequest = interceptedRequest.withAddedHeader("X-UniReq-Status", "DUPLICATE");
                return ProxyRequestReceivedAction.continueWith(annotatedRequest);
            }
            
        } catch (Exception e) {
            // Handle any errors gracefully to prevent extension crashes
            logging.logToError("Error in request handler: " + e.getMessage());
            e.printStackTrace();
            
            // In case of errors, allow the request through to maintain functionality
            return ProxyRequestReceivedAction.continueWith(interceptedRequest);
        }
    }
    
    /**
     * Handles HTTP requests before they are sent to the target server.
     * 
     * This method is called after the request has been processed but before
     * it's sent to the target server. Currently, we don't need to do any
     * additional processing at this stage.
     * 
     * @param interceptedRequest The intercepted HTTP request
     * @return ProxyRequestToBeSentAction indicating how to handle the request
     */
    @Override
    public ProxyRequestToBeSentAction handleRequestToBeSent(InterceptedRequest interceptedRequest) {
        // No additional processing needed at this stage
        return ProxyRequestToBeSentAction.continueWith(interceptedRequest);
    }
    
    /**
     * Handles HTTP responses when they are received from the target server.
     * 
     * This method is called when a response is received for a previously sent request.
     * It updates the stored request entry with the response data for complete 
     * request/response pairs in the GUI.
     * 
     * @param interceptedResponse The intercepted HTTP response
     * @return ProxyResponseReceivedAction indicating how to handle the response
     */
    @Override
    public ProxyResponseReceivedAction handleResponseReceived(InterceptedResponse interceptedResponse) {
        try {
            // Get the request that this response corresponds to
            HttpRequest originalRequest = interceptedResponse.initiatingRequest();
            
            // Update the stored request entry with this response
            deduplicator.updateResponse(originalRequest, interceptedResponse);
            
            // Refresh GUI to show updated request/response pairs
            if (gui != null) {
                gui.updateStatistics();
            }
            
            logging.logToOutput("Response received for: " + originalRequest.method() + " " + originalRequest.path());
            
        } catch (Exception e) {
            logging.logToError("Error handling response: " + e.getMessage());
        }
        
        // Always allow responses through
        return ProxyResponseReceivedAction.continueWith(interceptedResponse);
    }
    
    /**
     * Handles HTTP responses before they are sent back to the client.
     * 
     * This method is called after the response has been processed but before
     * it's sent back to the client. Currently, we don't need to do any
     * additional processing at this stage.
     * 
     * @param interceptedResponse The intercepted HTTP response
     * @return ProxyResponseToBeSentAction indicating how to handle the response
     */
    @Override
    public ProxyResponseToBeSentAction handleResponseToBeSent(InterceptedResponse interceptedResponse) {
        // No additional processing needed at this stage
        return ProxyResponseToBeSentAction.continueWith(interceptedResponse);
    }
    
    /**
     * Creates a safe description of a request for logging purposes.
     * Removes sensitive information like authorization headers.
     * 
     * @param request The HTTP request to describe
     * @return A safe description string
     */
    private String createSafeRequestDescription(HttpRequest request) {
        return String.format("%s %s%s", 
            request.method(),
            request.httpService().host(),
            request.path()
        );
    }
} 