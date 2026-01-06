/*
 * MCSets SetStore Plugin
 * Copyright (c) 2025-2026 MCSets
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 *
 * https://mcsets.com
 */
package com.mcsets.setstore.models;

import com.google.gson.annotations.SerializedName;

/**
 * Response model for the /connect API endpoint.
 *
 * @author MCSets
 * @version 1.0.0
 */
public class ConnectResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("server")
    private ServerInfo server;

    @SerializedName("pending_deliveries")
    private int pendingDeliveries;

    @SerializedName("websocket")
    private WebSocketConfig websocket;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public ServerInfo getServer() {
        return server;
    }

    public int getPendingDeliveries() {
        return pendingDeliveries;
    }

    public WebSocketConfig getWebsocket() {
        return websocket;
    }

    public String getMessage() {
        return message;
    }
}
