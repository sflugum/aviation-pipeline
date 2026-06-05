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
        logger.info("Initializing Aviation Data Pipeline Components...");
        OpenSkyClient client = new OpenSkyClient();
        BronzeIngestor ingestor = new BronzeIngestor();
        DatabaseManager dbManager = new DatabaseManager();

        logger.info("1. Fetching raw data from OpenSky API...");
        String rawJSON = client.fetchRawFlights();

        if (rawJSON != null) {
            logger.info("2. Opening connection to MySQL...");

            try (Connection conn = dbManager.connectToBronze()) {
                logger.info("3. Executing parsing and batch insertion...");
                ingestor.parseAndInsert(rawJSON, conn);
                logger.info("4. Pipeline run complete successfully.");

            } catch (Exception e) {
                logger.error("Pipeline execution failed during database operations", e);
            }
        } else {
            logger.error("Pipeline execution aborted: Abrupt failure or empty payload from OpenSky API.");
        }
    }
}