package com.pipeline.client;

import com.pipeline.config.ConfigManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenSkyClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenSkyClient.class);

    // TODO: remove hard code and rewrite in applications.properties
    //  see DatabaseManager for Docker Secret info
    private static final String API_URL = "https://opensky-network.org/api/states/all";

    public String fetchRawFlights() {
        try{
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();

        } catch (Exception e) {
            logger.error("Network request failed", e);
            return null;
        }
    }

}
