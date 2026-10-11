package com.ridelink.farepayment.client;

import com.ridelink.farepayment.client.dto.PaymentCompletedNotification;
import com.ridelink.farepayment.client.dto.RideSummaryResponse;
import com.ridelink.farepayment.exception.ResourceNotFoundException;
import com.ridelink.farepayment.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Handles the two interservice interactions this service has with Ride Management Service:
 *
 * 1. getRide()                - synchronous, blocking lookup used to confirm a ride exists, is
 *                                COMPLETED, who its passenger is, and how far it travelled. It has
 *                                to be synchronous: the caller cannot be answered until we know.
 * 2. notifyPaymentCompleted() - best-effort "fire and forget" call telling Ride Management the ride
 *                                is paid. A candidate for asynchronous messaging instead (see the
 *                                report's communication-method comparison).
 *
 * Both calls go to Ride Management's INTERNAL endpoints (/api/internal/rides/**) and are
 * authenticated with a shared internal API key in the X-Internal-Api-Key header - NOT the end
 * user's bearer token. Forwarding a passenger's own token would mean Ride Management had to let
 * passengers call "mark this ride paid", which would let a passenger mark their own ride as paid.
 *
 * Timeouts are set so a slow or hung Ride Management Service cannot hang this service's threads.
 */
@Component
public class RideManagementClient {

    private static final Logger log = LoggerFactory.getLogger(RideManagementClient.class);
    private static final String API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;
    private final boolean integrationEnabled;
    private final String internalApiKey;

    public RideManagementClient(
            @Value("${ridelink.services.ride-management.base-url}") String baseUrl,
            @Value("${ridelink.services.ride-management.enabled:true}") boolean integrationEnabled,
            @Value("${ridelink.internal.api-key}") String internalApiKey
    ) {
        // JDK HttpClient (not HttpURLConnection) because PATCH is not supported by the latter.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.integrationEnabled = integrationEnabled;
        this.internalApiKey = internalApiKey;
    }

    public boolean isEnabled() {
        return integrationEnabled;
    }

    /**
     * Fetches ride details. Throws ResourceNotFoundException if Ride Management doesn't know the
     * ride, or ServiceUnavailableException (HTTP 503) if it can't be reached or rejects our key.
     */
    public RideSummaryResponse getRide(String rideId) {
        try {
            RideSummaryResponse ride = restClient.get()
                    .uri("/api/internal/rides/{rideId}", rideId)
                    .header(API_KEY_HEADER, internalApiKey)
                    .retrieve()
                    .body(RideSummaryResponse.class);
            if (ride == null) {
                throw new ServiceUnavailableException(
                        "Ride Management Service returned an empty response for ride " + rideId);
            }
            return ride;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Ride not found in Ride Management Service: " + rideId);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            throw new ServiceUnavailableException(
                    "Ride Management Service rejected this service's internal API key - check that "
                            + "RIDELINK_INTERNAL_API_KEY is identical on both services");
        } catch (RestClientException e) {
            throw new ServiceUnavailableException(
                    "Unable to reach Ride Management Service to validate ride " + rideId
                            + ". Try again once it is running, or set "
                            + "RIDE_MANAGEMENT_INTEGRATION_ENABLED=false for isolated testing.");
        }
    }

    /**
     * Best-effort notification that a payment completed. Deliberately does NOT throw - the payment
     * is already durably recorded in this service's own database, which is the source of truth. A
     * failure to notify is logged; a production system would retry or use a message queue with
     * at-least-once delivery so ride status becomes eventually consistent.
     */
    public void notifyPaymentCompleted(String rideId, String paymentId, BigDecimal amount) {
        try {
            restClient.patch()
                    .uri("/api/internal/rides/{rideId}/payment-status", rideId)
                    .header(API_KEY_HEADER, internalApiKey)
                    .body(new PaymentCompletedNotification(paymentId, amount, "PAID"))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Ride Management Service could not be notified of payment {} for ride {}: {}",
                    paymentId, rideId, e.getMessage());
        }
    }
}
