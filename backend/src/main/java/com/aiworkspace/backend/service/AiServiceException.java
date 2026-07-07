package com.aiworkspace.backend.service;

/**
 * Raised when the ai-service call fails or errors — connection refused, timeout, non-2xx
 * response, or a response that couldn't be parsed. Callers should surface this as a clear
 * error rather than letting the underlying HTTP exception propagate.
 */
public class AiServiceException extends RuntimeException {

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public AiServiceException(String message) {
        super(message);
    }
}
