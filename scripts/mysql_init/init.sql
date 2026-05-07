DROP TABLE IF EXISTS raw_maintenance

CREATE TABLE raw_maintenance (
  log_id INT AUTO_INCREMENT PRIMARY KEY,
  icao24 VARCHAR(10) NOT NULL,
  service_type VARCHAR(100),
  status VARCHAR(50),
  log_date DATETIME
);