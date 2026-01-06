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
 * WebSocket configuration received from the /connect API endpoint.
 *
 * @author MCSets
 * @version 1.0.0
 */
public class WebSocketConfig {
    @SerializedName("host")
    private String host;

    @SerializedName("port")
    private int port;

    @SerializedName("app_key")
    private String appKey;

    @SerializedName("channel")
    private String channel;

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getAppKey() {
        return appKey;
    }

    public String getChannel() {
        return channel;
    }

    public String getWebSocketUrl() {
        String protocol = port == 443 ? "wss" : "ws";
        return protocol + "://" + host + ":" + port + "/app/" + appKey;
    }

    @Override
    public String toString() {
        return "WebSocketConfig{host='" + host + "', port=" + port + ", channel='" + channel + "'}";
    }
}
