package com.xiyu.bid.dto;

/**
 * DTO for email verification response
 */
public record EmailVerificationResponse(
    String message
) {
    public EmailVerificationResponse(String message) {
        this.message = message != null ? message : "Verification email sent";
    }
}
