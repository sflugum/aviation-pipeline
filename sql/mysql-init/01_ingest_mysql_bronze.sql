-- Raw landing table for OpenSky payloads. Data is kept as a single JSON blob here
-- instead of parsed into columns, so ingestion can't fail from a schema mismatch.
-- Parsing/validation happens later in the Gold transform step.
-- Using IF NOT EXISTS instead of DROP so re-running init doesn't wipe existing data.
CREATE TABLE IF NOT EXISTS opensky_raw_data (
    raw_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    raw_data JSON NOT NULL,
    ingestion_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed BOOLEAN DEFAULT FALSE -- flips to TRUE once GoldTransformer has read and loaded this row
);