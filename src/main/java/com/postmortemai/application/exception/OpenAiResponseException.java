package com.postmortemai.application.exception;

/**
 * Business exception thrown when the external OpenAI service fails.
 * Prevents leaking low-level HTTP client exceptions (like RestClientException)
 * into the business layer.
 */
public class OpenAiResponseException extends RuntimeException {

    public OpenAiResponseException(String message) {
        super(message);
    }

    public OpenAiResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
