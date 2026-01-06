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
 * Response model for the /verify API endpoint.
 *
 * @author MCSets
 * @version 1.0.0
 */
public class VerifyResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("code")
    private String code;

    @SerializedName("expires_in")
    private int expiresIn;

    @SerializedName("store_url")
    private String storeUrl;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public String getCode() {
        return code;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public String getStoreUrl() {
        return storeUrl;
    }

    public String getMessage() {
        return message;
    }
}
