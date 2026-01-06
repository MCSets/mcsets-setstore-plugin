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
import java.util.List;

/**
 * Response model for the /queue API endpoint.
 *
 * @author MCSets
 * @version 1.0.0
 */
public class QueueResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("count")
    private int count;

    @SerializedName("deliveries")
    private List<Delivery> deliveries;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public int getCount() {
        return count;
    }

    public List<Delivery> getDeliveries() {
        return deliveries;
    }

    public String getMessage() {
        return message;
    }
}
