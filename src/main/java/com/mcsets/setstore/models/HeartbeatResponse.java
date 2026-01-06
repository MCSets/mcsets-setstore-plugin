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
 * Response model for the /heartbeat API endpoint.
 *
 * @author MCSets
 * @version 1.0.0
 */
public class HeartbeatResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("pending_deliveries")
    private int pendingDeliveries;

    @SerializedName("timestamp")
    private String timestamp;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public int getPendingDeliveries() {
        return pendingDeliveries;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }
}
