DROP TABLE IF EXISTS opensky_data_raw;

CREATE TABLE opensky_raw_data (
    raw_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    raw_data JSON NOT NULL,
    ingestion_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);





