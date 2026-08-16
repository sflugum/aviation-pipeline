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
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads unprocessed raw rows from the MySQL Bronze table, resolves them against
 * the PostgreSQL star schema (dim_aircraft, dim_time), and loads the results
 * into fact_flight_state. Runs as a single batched transaction per pipeline run.
 */
public class GoldTransformer {

    private static final Logger logger = LoggerFactory.getLogger(GoldTransformer.class);

    private record AircraftState(long id, String callsign, String originCountry) {}

    // In-memory caches so repeated aircraft/timestamps within one run don't hit the
    // database again after the first lookup. This class is instantiated fresh in Main
    // each run, so caches start empty each time. If this were to be updated in the future to
    // run in a continuous loop, the caches would need to be cleared between runs.
    private final Map<String, AircraftState> aircraftCache = new HashMap<>();
    private final Map<Long, Long> timeCache = new HashMap<>();

    private final ObjectMapper mapper = new ObjectMapper();

    public void transformAndLoad(Connection mysqlReadConn, Connection mysqlWriteConn, Connection pgConn) {
        String extractSql = "SELECT raw_id, raw_data FROM opensky_raw_data WHERE processed = FALSE";
        String updateBronzeSql = "UPDATE opensky_raw_data SET processed = TRUE WHERE raw_id = ?";

        String factInsertSql = """
                INSERT INTO fact_flight_state (
                    aircraft_id, time_id, latitude, longitude, baro_altitude,
                    geo_altitude, velocity, true_track, vertical_rate, on_ground,
                    time_position, last_contact, squawk, spi, position_source
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        String selectAircraftSql = "SELECT aircraft_id, callsign, origin_country FROM dim_aircraft WHERE icao24 = ? AND is_current = TRUE";
        String expireAircraftSql = "UPDATE dim_aircraft SET is_current = FALSE, effective_to = NOW() WHERE aircraft_id = ?";
        String insertAircraftSql = "INSERT INTO dim_aircraft (icao24, callsign, origin_country) VALUES (?, ?, ?) RETURNING aircraft_id";

        String timeInsertSql = """
                INSERT INTO dim_time (full_timestamp, date, year, month, day, hour, minute, second, day_of_week)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (full_timestamp) DO NOTHING RETURNING time_id
                """;

        String timeSelectSql = "SELECT time_id FROM dim_time WHERE full_timestamp = ?";

        try (PreparedStatement extractStmt = mysqlReadConn.prepareStatement(extractSql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
             PreparedStatement updateBronzeStmt = mysqlWriteConn.prepareStatement(updateBronzeSql);
             PreparedStatement factStmt = pgConn.prepareStatement(factInsertSql);
             PreparedStatement selectAircraftStmt = pgConn.prepareStatement(selectAircraftSql);
             PreparedStatement expireAircraftStmt = pgConn.prepareStatement(expireAircraftSql);
             PreparedStatement insertAircraftStmt = pgConn.prepareStatement(insertAircraftSql);
             PreparedStatement timeInsertStmt = pgConn.prepareStatement(timeInsertSql);
             PreparedStatement timeSelectStmt = pgConn.prepareStatement(timeSelectSql)) {

            pgConn.setAutoCommit(false);
            mysqlWriteConn.setAutoCommit(false);

            // Forces MySQL's JDBC driver to stream results row by row instead of loading
            // the whole result set into memory. Needed since this table can grow large
            // and MySQL's default behavior otherwise pulls the entire query result at once
            extractStmt.setFetchSize(Integer.MIN_VALUE);

            try (ResultSet rs = extractStmt.executeQuery()) {

                int batchCount = 0;
                int totalProcessed = 0;

                while (rs.next()) {
                    long rawId = rs.getLong("raw_id");
                    String rawJson = rs.getString("raw_data");

                    updateBronzeStmt.setLong(1, rawId);
                    updateBronzeStmt.addBatch();
                    totalProcessed++;

                    JsonNode rootNode = mapper.readTree(rawJson);
                    FlightRecord flight = new FlightRecord(rootNode);

                    // Rows missing either of these can't be placed in the fact table since
                    // both are needed to resolve dimension keys, so they're skipped rather
                    // than failing the whole batch
                    if (flight.getIcao24() == null || flight.getTimePosition() == null) {
                        continue;
                    }

                    long aircraftId = resolveAircraftDimension(selectAircraftStmt, expireAircraftStmt, insertAircraftStmt, flight);
                    long timeId = resolveTimeDimension(timeInsertStmt, timeSelectStmt, flight.getTimePosition());

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

                    // Flushing both batches together every 1000 rows keeps the fact insert
                    // and the bronze "processed" flag update in sync with each other
                    if (totalProcessed % 1000 == 0) {
                        factStmt.executeBatch();
                        updateBronzeStmt.executeBatch();
                    }
                }

                factStmt.executeBatch();
                updateBronzeStmt.executeBatch();

                pgConn.commit();
                mysqlWriteConn.commit();

                logger.info("Successfully processed {} raw records and loaded {} flight records into Postgres", totalProcessed, batchCount);
            }
        } catch (Exception e) {
            // Rolling back both connections on any failure so a partial run doesn't leave
            // Postgres and MySQL out of sync with each other (e.g. rows marked processed
            // in MySQL but never actually landed in Postgres, or vice versa)
            logger.error("Error during transformation. Rolling back transactions", e);
            try {
                if (pgConn != null) pgConn.rollback();
                if (mysqlWriteConn != null) mysqlWriteConn.rollback();
            } catch (SQLException ex) {
                logger.error("Failed to rollback transactions", ex);
            }
        } finally {
            try {
                if (pgConn != null) pgConn.setAutoCommit(true);
                if (mysqlWriteConn != null) mysqlWriteConn.setAutoCommit(true);
            } catch (SQLException e) {
                logger.error("Failed to reset auto-commit", e);
            }
        }
    }

    /**
     * Resolves the current dim_aircraft surrogate key for a flight, checking the
     * in-memory cache first, then the database. If the aircraft's callsign or
     * origin country has changed since the last known record, the old row is
     * expired and a new one is inserted rather than updated in place.
     */
    private long resolveAircraftDimension(PreparedStatement selectStmt, PreparedStatement expireStmt, PreparedStatement insertStmt, FlightRecord flight) throws SQLException {
        String icao = flight.getIcao24();
        String newCallsign = flight.getCallsign();
        String newCountry = flight.getOriginCountry();

        // 1. Check cache first
        if (aircraftCache.containsKey(icao)) {
            AircraftState cached = aircraftCache.get(icao);

            // If data matches perfectly, return cached ID
            if (Objects.equals(cached.callsign(), newCallsign) && Objects.equals(cached.originCountry(), newCountry)) {
                return cached.id();
            }

            // Expire old record in database if callsign / origin country change
            expireStmt.setLong(1, cached.id());
            expireStmt.executeUpdate();
        } else {
            // Not in cache, check database for an existing active record
            selectStmt.setString(1, icao);
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    long existingId = rs.getLong("aircraft_id");
                    String existingCallsign = rs.getString("callsign");
                    String existingCountry = rs.getString("origin_country");

                    if (Objects.equals(existingCallsign, newCallsign) && Objects.equals(existingCountry, newCountry)) {
                        // New record matches existing record. Cache it and return.
                        aircraftCache.put(icao, new AircraftState(existingId, existingCallsign, existingCountry));
                        return existingId;
                    }

                    // Record exists but changed. Expire the old record.
                    expireStmt.setLong(1, existingId);
                    expireStmt.executeUpdate();
                }
            }
        }

        // 2. Insert the new record state
        insertStmt.setString(1, icao);
        insertStmt.setString(2, newCallsign);
        insertStmt.setString(3, newCountry);

        try (ResultSet rs = insertStmt.executeQuery()) {
            if (rs.next()) {
                long newId = rs.getLong(1);
                aircraftCache.put(icao, new AircraftState(newId, newCallsign, newCountry));
                return newId;
            }
        }
        throw new SQLException("Failed to insert and resolve aircraft_id for ICAO: " + icao);
    }

    /**
     * Resolves the dim_time surrogate key for a given Unix timestamp, inserting a
     * new row if one doesn't exist yet. The ON CONFLICT DO NOTHING means a
     * duplicate timestamp returns no row from the insert, so there's a fallback
     * select to fetch the id in that case.
     */
    private long resolveTimeDimension(PreparedStatement insertStmt, PreparedStatement selectStmt, Long unixTimestamp) throws SQLException {
        if (timeCache.containsKey(unixTimestamp)) {
            return timeCache.get(unixTimestamp);
        }

        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(unixTimestamp), ZoneId.of("UTC"));

        insertStmt.setObject(1, dateTime);
        insertStmt.setObject(2, dateTime.toLocalDate());
        insertStmt.setInt(3, dateTime.getYear());
        insertStmt.setInt(4, dateTime.getMonthValue());
        insertStmt.setInt(5, dateTime.getDayOfMonth());
        insertStmt.setInt(6, dateTime.getHour());
        insertStmt.setInt(7, dateTime.getMinute());
        insertStmt.setInt(8, dateTime.getSecond());
        insertStmt.setString(9, dateTime.getDayOfWeek().name());

        try (ResultSet rs = insertStmt.executeQuery()) {
            if (rs.next()) {
                long id = rs.getLong(1);
                timeCache.put(unixTimestamp, id);
                return id;
            }
        }

        // Fallback: If it already exists, select the ID
        selectStmt.setObject(1, dateTime);
        try (ResultSet rs = selectStmt.executeQuery()) {
            if (rs.next()) {
                long id = rs.getLong(1);
                timeCache.put(unixTimestamp, id);
                return id;
            }
        }

        throw new SQLException("Failed to resolve time_id for timestamp: " + unixTimestamp);
    }

    // The three setXSafe helpers below exist because PreparedStatement.setDouble/setBoolean/
    // setInt don't accept null directly. OpenSky fields are frequently missing/null, so each
    // needs its own explicit setNull(Types.X) branch instead of one generic helper.
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
