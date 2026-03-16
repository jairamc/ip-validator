package com.personal.ipvalidator.model;

import java.time.Instant;

public record RequestResult(
        int requestNumber,
        String ip,
        boolean success,
        String error,
        Instant timestamp
) {

    public static RequestResult success(int requestNumber, String ip) {
        return new RequestResult(requestNumber, ip, true, null, Instant.now());
    }

    public static RequestResult failure(int requestNumber, String error) {
        return new RequestResult(requestNumber, null, false, error, Instant.now());
    }
}
