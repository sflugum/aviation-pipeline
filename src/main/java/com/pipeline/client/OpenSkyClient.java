package com.pipeline.client;

import com.pipeline.config.ConfigManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps the OpenSky states API call using java.net.http.HttpClient directly,
 * rather than a framework HTTP client, to keep this layer dependency-free.
 */
public class OpenSkyClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenSkyClient.class);
    private static final String API_URL = ConfigManager.get("opensky.api.url");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Fetches the current raw flight state JSON from OpenSky.
     * Returns null on any failure (non-200 response, network error, interruption),
     * which lets the caller decide whether to abort the pipeline run.
     */
    public String fetchRawFlights() {
        try{
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                logger.error("Opensky API returned an error status code: {} - Body: {}",
                        response.statusCode(), response.body());
                return null;
            }

        } catch (InterruptedException e) {
            // Interrupt flag restored here since the exception is caught rather than propagated
            Thread.currentThread().interrupt();
            logger.error("API request was interrupted", e);
            return null;
        } catch (Exception e) {
            logger.error("Network request failed due to an exception", e);
            return null;
        }
    }
}
