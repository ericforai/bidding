package com.xiyu.bid.organization.domain;

/**
 * Thrown when an incoming organization event fails structural validation.
 */
public class EventValidationException extends RuntimeException {

    public EventValidationException(String message) {
        super(message);
    }

    public EventValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
