package com.edss.identity.api.dto;

public record OkResponse(boolean ok) {

    public static OkResponse ok() {
        return new OkResponse(true);
    }
}
