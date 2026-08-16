-- Using IF NOT EXISTS instead of DROP so re-running init doesn't wipe existing data.

-- SCD Type 2: effective_from/effective_to/is_current track history instead of
-- overwriting in place. When an aircraft's callsign or origin_country changes,
-- GoldTransformer.resolveAircraftDimension() expires the old row and inserts a
-- new one rather than updating the existing record.
CREATE TABLE IF NOT EXISTS dim_aircraft (
    aircraft_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY ,
    icao24 VARCHAR(6) NOT NULL,
    callsign VARCHAR(8),
    origin_country VARCHAR(100),
    category VARCHAR(10),
    effective_from TIMESTAMP DEFAULT NOW(),
    effective_to TIMESTAMP DEFAULT '9999-12-31',
    is_current BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS dim_time (
    time_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_timestamp TIMESTAMP UNIQUE,
    date DATE,
    year INT,
    month INT,
    day INT,
    hour INT,
    minute INT,
    second INT,
    day_of_week VARCHAR(9)
);

CREATE TABLE IF NOT EXISTS fact_flight_state (
    state_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    aircraft_id BIGINT NOT NULL REFERENCES dim_aircraft(aircraft_id),
    time_id BIGINT NOT NULL REFERENCES dim_time(time_id),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    baro_altitude DOUBLE PRECISION,
    geo_altitude DOUBLE PRECISION,
    velocity DOUBLE PRECISION,
    true_track DOUBLE PRECISION,
    vertical_rate DOUBLE PRECISION,
    on_ground BOOLEAN,
    time_position TIMESTAMP,
    last_contact TIMESTAMP,
    squawk VARCHAR(4),
    spi BOOLEAN,
    position_source INT
);