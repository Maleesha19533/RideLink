
package com.ridelink.ride_management_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode; 
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class DriverClient {

    private final RestClient restClient;

    public DriverClient(
            @Value("${driver.service.url}") String driverServiceUrl) {

        this.restClient = RestClient.builder()
                .baseUrl(driverServiceUrl)
                .build();
    }

    public boolean isDriverAvailable(
            Long driverId,
            String authorizationHeader) {

        List<Map> drivers = restClient.get()
                .uri("/api/drivers/available")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new IllegalStateException(
                            "Driver Service returned HTTP "
                                    + response.getStatusCode().value());
                })
                .body(new org.springframework.core.ParameterizedTypeReference<>() {});

        if (drivers == null) {
            return false;
        }

        return drivers.stream().anyMatch(driver ->
                driverId.toString().equals(
                        String.valueOf(driver.get("id")))
                        && Boolean.TRUE.equals(driver.get("available"))
        );
    }
}
