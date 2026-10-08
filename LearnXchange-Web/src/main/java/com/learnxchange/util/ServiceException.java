package com.learnxchange.util;

/** Business-rule / validation failure. The message is safe to show to the user. */
public class ServiceException extends RuntimeException {
    public ServiceException(String message) { super(message); }
    public ServiceException(String message, Throwable cause) { super(message, cause); }
}
