DROP TABLE IF EXISTS opensky_raw_data;

CREATE TABLE opensky_raw_data (
    raw_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    raw_data JSON NOT NULL,
    ingestion_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);