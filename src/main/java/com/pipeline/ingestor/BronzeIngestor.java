package com.pipeline.ingestor;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BronzeIngestor {

    private static final Logger logger = LoggerFactory.getLogger(BronzeIngestor.class);

    public void parseAndInsert(String jsonPayload, Connection conn) {

        String insertSql = "INSERT INTO opensky_raw_data (raw_data) VALUES (?)";

        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonPayload);
            JsonNode statesArray = rootNode.get("states");

            if (statesArray != null && statesArray.isArray()) {
                logger.info("Queueing {} records for batch insert...", statesArray.size());
                int count = 0;

                for (JsonNode flightNode : statesArray) {
                    pstmt.setString(1, flightNode.toString());
                    pstmt.addBatch();
                    count++;

                    if (count % 1000 == 0) {
                        pstmt.executeBatch();
                    }
                }
                pstmt.executeBatch();
                logger.info("Successfully inserted {} raw flight records into Bronze DB.", count);
            }
        } catch (Exception e) {
            logger.error("Database execution failed", e);
        }
    }
}