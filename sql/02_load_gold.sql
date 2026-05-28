DROP TABLE IF EXISTS fact_flight_state;
DROP TABLE IF EXISTS dim_aircraft;
DROP TABLE IF EXISTS dim_time;

CREATE TABLE dim_aircraft (
    aircraft_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY ,
    icao24 VARCHAR(6) NOT NULL ,
    callsign VARCHAR(8),
    origin_country VARCHAR(100),
    category VARCHAR(10),
    effective_from TIMESTAMP DEFAULT NOW(),
    effective_to TIMESTAMP DEFAULT '9999-12-31',
    is_current BOOLEAN DEFAULT TRUE
);

CREATE TABLE dim_time (
    time_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_timestamp TIMESTAMP,
    date DATE,
    year INT,
    month INT,
    day INT,
    hour INT,
    minute INT,
    second INT,
    day_of_week VARCHAR(9)
);

CREATE TABLE fact_flight_state (
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
    sensors INT[],
    position_source INT
);


