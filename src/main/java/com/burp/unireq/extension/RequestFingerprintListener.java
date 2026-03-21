package com.burp.unireq.extension;

import burp.api.montoya.http.message.requests.HttpRequest;
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
 * Intercepts HTTP requests and responses through Burp's proxy, applies
 * deduplication logic, and updates the GUI in real-time.
 *
 * @author Harshit Shah
 */
public class RequestFingerprintListener implements ProxyRequestHandler, ProxyResponseHandler {

    private final RequestDeduplicator deduplicator;
    private final UniReqGui gui;
    private final Logging logging;

    public RequestFingerprintListener(RequestDeduplicator deduplicator, UniReqGui gui, Logging logging) {
        this.deduplicator = deduplicator;
        this.gui = gui;
        this.logging = logging;
    }

    @Override
    public ProxyRequestReceivedAction handleRequestReceived(InterceptedRequest interceptedRequest) {
        try {
            boolean isUnique = deduplicator.isUniqueRequest(interceptedRequest);

            if (gui != null) {
                gui.scheduleRefresh();
            }

            if (isUnique) {
                return ProxyRequestReceivedAction.continueWith(interceptedRequest);
            } else {
                HttpRequest annotatedRequest = interceptedRequest.withAddedHeader("X-UniReq-Status", "DUPLICATE");
                return ProxyRequestReceivedAction.continueWith(annotatedRequest);
            }

        } catch (Exception e) {
            logging.logToError("Error in request handler: " + e.getMessage());
            return ProxyRequestReceivedAction.continueWith(interceptedRequest);
        }
    }

    @Override
    public ProxyRequestToBeSentAction handleRequestToBeSent(InterceptedRequest interceptedRequest) {
        return ProxyRequestToBeSentAction.continueWith(interceptedRequest);
    }

    @Override
    public ProxyResponseReceivedAction handleResponseReceived(InterceptedResponse interceptedResponse) {
        try {
            HttpRequest originalRequest = interceptedResponse.initiatingRequest();
            deduplicator.updateResponse(originalRequest, interceptedResponse);

            if (gui != null) {
                gui.scheduleRefresh();
            }
        } catch (Exception e) {
            logging.logToError("Error handling response: " + e.getMessage());
        }

        return ProxyResponseReceivedAction.continueWith(interceptedResponse);
    }

    @Override
    public ProxyResponseToBeSentAction handleResponseToBeSent(InterceptedResponse interceptedResponse) {
        return ProxyResponseToBeSentAction.continueWith(interceptedResponse);
    }
}
