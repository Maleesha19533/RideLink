package com.ridelink.farepayment.exception;

/**
 * Thrown when another RideLink service this one depends on cannot be reached or rejects our
 * internal credentials. Mapped to HTTP 503 - the caller did nothing wrong, a dependency is
 * unavailable - instead of a misleading 400.
 */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}
