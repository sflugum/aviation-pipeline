package com.pipeline.transformer;

import com.pipeline.model.FlightRecord;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoldTransformer {

    private static final Logger logger = LoggerFactory.getLogger(GoldTransformer.class);

    private final Map<String, Long> aircraftCache = new HashMap<>();
    private final Map<Long, Long> timeCache = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public void transformAndLoad(Connection mysqlConn, Connection pgConn) {
        String extractSql = "SELECT raw_data FROM opensky_raw_data";

        String factInsertSql = """
                INSERT INTO fact_flight_state (
                    aircraft_id, time_id, latitude, longitude, baro_altitude,
                    geo_altitude, velocity, true_track, vertical_rate, on_ground,
                    time_position, last_contact, squawk, spi, position_source
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement extractStmt = mysqlConn.prepareStatement(extractSql);
             ResultSet rs = extractStmt.executeQuery();
             PreparedStatement factStmt = pgConn.prepareStatement(factInsertSql)) {

            pgConn.setAutoCommit(false);
            int batchCount = 0;

            while (rs.next()) {
                String rawJson = rs.getString("raw_data");
                JsonNode rootNode = mapper.readTree(rawJson);
                FlightRecord flight = new FlightRecord(rootNode);

                // 1. Skip records missing critical identifiers
                if (flight.getIcao24() == null || flight.getTimePosition() == null) {
                    continue;
                }

                // 2. Resolve Dimension IDs (Upsert logic)
                long aircraftId = resolveAircraftDimension(pgConn, flight);
                long timeId = resolveTimeDimension(pgConn, flight.getTimePosition());

                // 3. Prepare Fact Table Batch
                factStmt.setLong(1, aircraftId);
                factStmt.setLong(2, timeId);
                setDoubleSafe(factStmt, 3, flight.getLatitude());
                setDoubleSafe(factStmt, 4, flight.getLongitude());
                setDoubleSafe(factStmt, 5, flight.getBaroAltitude());
                setDoubleSafe(factStmt, 6, flight.getGeoAltitude());
                setDoubleSafe(factStmt, 7, flight.getVelocity());
                setDoubleSafe(factStmt, 8, flight.getTrueTrack());
                setDoubleSafe(factStmt, 9, flight.getVerticalRate());
                setBooleanSafe(factStmt, 10, flight.getOnGround());

                LocalDateTime timePos = LocalDateTime.ofInstant(Instant.ofEpochSecond(flight.getTimePosition()), ZoneId.of("UTC"));
                factStmt.setObject(11, timePos);

                LocalDateTime lastContact = flight.getLastContact() != null ?
                        LocalDateTime.ofInstant(Instant.ofEpochSecond(flight.getLastContact()), ZoneId.of("UTC")) : null;
                factStmt.setObject(12, lastContact);

                factStmt.setString(13, flight.getSquawk());
                setBooleanSafe(factStmt, 14, flight.getSpi());
                setIntegerSafe(factStmt, 15, flight.getPositionSource());

                factStmt.addBatch();
                batchCount++;

                // Execute batch every 1000 records
                if (batchCount % 1000 == 0) {
                    factStmt.executeBatch();
                }
            }

            // Execute any remaining records in the batch
            factStmt.executeBatch();
            pgConn.commit();

            logger.info("Successfully transformed and loaded {} flight records into PostgreSQL Gold Layer", batchCount);
        } catch (Exception e) {
            logger.error("Error during transformation and loading. Rolling back transaction", e);
            try {
                if (pgConn != null) pgConn.rollback();
            } catch (SQLException ex) {
                logger.error("Failed to rollback transaction", ex);
            }
        } finally {
            try {
                if (pgConn != null) pgConn.setAutoCommit(true);
            } catch (SQLException e) {
                logger.error("Failed to reset auto-commit", e);
            }
        }
    }

    private long resolveAircraftDimension(Connection pgConn, FlightRecord flight) throws SQLException {
        String icao = flight.getIcao24();
        if (aircraftCache.containsKey(icao)) {
            return aircraftCache.get(icao);
        }

        String sql = """
                INSERT INTO dim_aircraft (icao24, callsign, origin_country)
                VALUES (?, ?, ?)
                ON CONFLICT (icao24) DO UPDATE SET
                    callsign = EXCLUDED.callsign,
                    origin_country = EXCLUDED.origin_country
                RETURNING aircraft_id
                """;

        try (PreparedStatement pstmt = pgConn.prepareStatement(sql)) {
            pstmt.setString(1, icao);
            pstmt.setString(2, flight.getCallsign());
            pstmt.setString(3, flight.getOriginCountry());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                long id = rs.getLong(1);
                aircraftCache.put(icao, id);
                return id;
            }
        }
        throw new SQLException("Failed to resolve aircraft_id for ICAO: " + icao);
    }

    private long resolveTimeDimension(Connection pgConn, Long unixTimestamp) throws SQLException {
        if (timeCache.containsKey(unixTimestamp)) {
            return timeCache.get(unixTimestamp);
        }

        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(unixTimestamp), ZoneId.of("UTC"));

        String insertSQL = """
                INSERT INTO dim_time (full_timestamp, date, year, month, day, hour, minute, second, day_of_week)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (full_timestamp) DO NOTHING RETURNING time_id
                """;

        try (PreparedStatement pstmt = pgConn.prepareStatement(insertSQL)) {
            pstmt.setObject(1, dateTime);
            pstmt.setObject(2, dateTime.toLocalDate());
            pstmt.setInt(3, dateTime.getYear());
            pstmt.setInt(4, dateTime.getMonthValue());
            pstmt.setInt(5, dateTime.getDayOfMonth());
            pstmt.setInt(6, dateTime.getHour());
            pstmt.setInt(7, dateTime.getMinute());
            pstmt.setInt(8, dateTime.getSecond());
            pstmt.setString(9, dateTime.getDayOfWeek().name());

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                long id = rs.getLong(1);
                timeCache.put(unixTimestamp, id);
                return id;
            }
        }

        // Fallback: If it already exists, select the ID
        String selectSql = "SELECT time_id FROM dim_time WHERE full_timestamp = ?";
        try (PreparedStatement pstmt = pgConn.prepareStatement(selectSql)) {
            pstmt.setObject(1, dateTime);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                long id = rs.getLong(1);
                timeCache.put(unixTimestamp, id);
                return id;
            }
        }
        throw new SQLException("Failed to resolve time_id for timestamp: " + unixTimestamp);
    }

    private void setDoubleSafe(PreparedStatement pstmt, int index, Double value) throws SQLException {
        if (value == null) pstmt.setNull(index, Types.DOUBLE);
        else pstmt.setDouble(index, value);
    }

    private void setBooleanSafe(PreparedStatement pstmt, int index, Boolean value) throws SQLException {
        if (value == null) pstmt.setNull(index, Types.BOOLEAN);
        else pstmt.setBoolean(index, value);
    }

    private void setIntegerSafe(PreparedStatement pstmt, int index, Integer value) throws SQLException {
        if (value == null) pstmt.setNull(index, Types.INTEGER);
        else pstmt.setInt(index, value);
    }

}
