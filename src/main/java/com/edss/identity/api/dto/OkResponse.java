package com.edss.identity.api.dto;

public record OkResponse(boolean ok) {

    private static final OkResponse OK = new OkResponse(true);

    public static OkResponse instance() {
        return OK;
    }
}
