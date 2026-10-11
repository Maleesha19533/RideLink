package com.ridelink.farepayment.client;

import com.ridelink.farepayment.client.dto.RideSummaryResponse;
import com.ridelink.farepayment.exception.ResourceNotFoundException;
import com.ridelink.farepayment.exception.ServiceUnavailableException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the real HTTP behaviour of RideManagementClient against a tiny local fake Ride Management
 * Service (JDK built-in HttpServer) - no Spring context, no mocks of the client itself. Proves the
 * client uses the internal API key header and internal paths, and turns each failure kind into
 * the right exception.
 */
class RideManagementClientTest {

    private static final String KEY = "unit-test-internal-key";

    private HttpServer server;
    private int port;
    private boolean stopped;
    private final AtomicReference<String> lastApiKey = new AtomicReference<>();
    private final AtomicReference<String> lastRequest = new AtomicReference<>();
    private final AtomicReference<String> lastAuthorizationHeader = new AtomicReference<>();

    @BeforeEach
    void startFakeRideManagement() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/internal/rides/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            String path = exchange.getRequestURI().getPath();
            lastApiKey.set(exchange.getRequestHeaders().getFirst("X-Internal-Api-Key"));
            lastAuthorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            lastRequest.set(exchange.getRequestMethod() + " " + path);

            if (!KEY.equals(lastApiKey.get())) {
                respond(exchange, 401, null);
            } else if (path.endsWith("/payment-status")) {
                respond(exchange, 204, null);
            } else if (path.endsWith("/RIDE-MISSING")) {
                respond(exchange, 404, null);
            } else {
                respond(exchange, 200,
                        "{\"rideId\":\"RIDE-1\",\"passengerId\":\"p-1\",\"driverId\":\"d-1\","
                                + "\"status\":\"COMPLETED\",\"distanceKm\":12.5}");
            }
        });
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterEach
    void stopFakeRideManagement() {
        stopServer();
    }

    private void stopServer() {
        if (!stopped) {
            server.stop(0);
            stopped = true;
        }
    }

    private static void respond(HttpExchange exchange, int status, String jsonBody) throws IOException {
        if (jsonBody == null) {
            exchange.sendResponseHeaders(status, -1);
        } else {
            byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    private RideManagementClient clientWithKey(String key) {
        return new RideManagementClient("http://localhost:" + port, true, key);
    }

    @Test
    void getRide_sendsTheInternalApiKeyToTheInternalPath_andParsesTheRide() {
        RideSummaryResponse ride = clientWithKey(KEY).getRide("RIDE-1");

        assertEquals("p-1", ride.passengerId());
        assertEquals("COMPLETED", ride.status());
        assertEquals(0, new BigDecimal("12.5").compareTo(ride.distanceKm()));
        assertEquals(KEY, lastApiKey.get());
        assertEquals("GET /api/internal/rides/RIDE-1", lastRequest.get());
    }

    @Test
    void getRide_neverForwardsAUserBearerToken() {
        clientWithKey(KEY).getRide("RIDE-1");

        assertNull(lastAuthorizationHeader.get());
    }

    @Test
    void getRide_throwsNotFoundForAnUnknownRide() {
        assertThrows(ResourceNotFoundException.class, () -> clientWithKey(KEY).getRide("RIDE-MISSING"));
    }

    @Test
    void getRide_throwsServiceUnavailableWhenTheInternalKeyIsRejected() {
        ServiceUnavailableException ex = assertThrows(ServiceUnavailableException.class,
                () -> clientWithKey("wrong-key").getRide("RIDE-1"));

        assertTrue(ex.getMessage().contains("internal API key"));
    }

    @Test
    void getRide_throwsServiceUnavailableWhenRideManagementIsDown() {
        stopServer();

        ServiceUnavailableException ex = assertThrows(ServiceUnavailableException.class,
                () -> clientWithKey(KEY).getRide("RIDE-1"));

        assertTrue(ex.getMessage().contains("Unable to reach"));
    }

    @Test
    void notifyPaymentCompleted_sendsAPatchWithTheInternalApiKey() {
        clientWithKey(KEY).notifyPaymentCompleted("RIDE-1", "PAY-1", new BigDecimal("900"));

        assertEquals("PATCH /api/internal/rides/RIDE-1/payment-status", lastRequest.get());
        assertEquals(KEY, lastApiKey.get());
        assertNull(lastAuthorizationHeader.get());
    }

    @Test
    void notifyPaymentCompleted_doesNotThrowWhenRideManagementIsDown() {
        stopServer();

        assertDoesNotThrow(() ->
                clientWithKey(KEY).notifyPaymentCompleted("RIDE-1", "PAY-1", new BigDecimal("900")));
    }
}
