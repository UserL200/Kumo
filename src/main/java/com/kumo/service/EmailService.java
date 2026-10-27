package com.kumo.service;

public interface EmailService {
    /**
     * Sends the "you're on the waiting list" confirmation email.
     * Implementations should not throw on failure — log it and let the
     * caller decide whether to retry, so a flaky mail provider never
     * fails the whole signup request.
     */
    boolean sendWaitlistConfirmation(String toEmail);
}
