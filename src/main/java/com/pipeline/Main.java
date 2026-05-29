package com.pipeline;

import com.pipeline.client.OpenSkyClient;
import com.pipeline.db.DatabaseManager;
import com.pipeline.ingestor.BronzeIngestor;

import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        OpenSkyClient client = new OpenSkyClient();
        BronzeIngestor ingestor = new BronzeIngestor();
        DatabaseManager dbManager = new DatabaseManager();



        logger.info("1. Fetching raw data from OpenSky API...");
        String rawJSON = client.fetchRawFlights();

        if (rawJSON != null) {
            logger.info("2. Opening connection to Bronze MySQL...");

            try (Connection conn = dbManager.getBronzeConnection()) {

                logger.info("3. Executing parsing and batch insertion...");
                ingestor.parseAndInsert(rawJSON, conn);

            } catch (Exception e) {
                logger.error("Pipeline failed", e);
            }

            logger.info("4. Pipeline run complete. Connection closed.");
        }
    }
}