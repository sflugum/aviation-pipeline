package com.pipeline;

import com.pipeline.client.OpenSkyClient;
import com.pipeline.db.DatabaseManager;
import com.pipeline.ingestor.BronzeIngestor;
import com.pipeline.transformer.GoldTransformer;

import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point that runs the pipeline end to end: pull from OpenSky, land the
 * raw payload in MySQL (Bronze), then transform it into the PostgreSQL star
 * schema (Gold).
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Initializing Aviation Data Pipeline Components...");
        OpenSkyClient client = new OpenSkyClient();
        BronzeIngestor ingestor = new BronzeIngestor();
        GoldTransformer transformer = new GoldTransformer();
        DatabaseManager dbManager = new DatabaseManager();

        logger.info("1. Fetching raw data from OpenSky API...");
        String rawJSON = client.fetchRawFlights();

        if (rawJSON != null) {
            logger.info("2. Opening connection to MySQL and PostgreSQL databases...");

            // Two separate MySQL connections so the read cursor (streaming raw rows) and the
            // write statements (marking rows processed) don't interfere with each other
            try (Connection mysqlReadConn = dbManager.connectToBronze();
                 Connection mysqlWriteConn = dbManager.connectToBronze();
                 Connection pgConn = dbManager.connectToGold()) {

                logger.info("3. Executing parsing and batch insertion into MySql...");
                ingestor.parseAndInsert(rawJSON, mysqlWriteConn);

                logger.info("4. Transforming and loading data into PostgreSQL...");
                transformer.transformAndLoad(mysqlReadConn, mysqlWriteConn, pgConn);

                logger.info("5. Pipeline run complete successfully.");

            } catch (Exception e) {
                logger.error("Pipeline execution failed during database operations", e);
            }
        } else {
            logger.error("Pipeline execution aborted: Abrupt failure or empty payload from OpenSky API.");
        }
    }
}