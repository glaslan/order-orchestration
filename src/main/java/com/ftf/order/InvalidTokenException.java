package com.ftf.order;

public class InvalidTokenException extends RuntimeException {

    public enum Reason { EXPIRED, INVALID_SIGNATURE, MALFORMED, MISSING }

    private final Reason reason;

    public InvalidTokenException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() { return reason; }
}
