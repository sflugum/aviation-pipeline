DROP TABLE IF EXISTS maintenance_events;
DROP TABLE IF EXISTS flight_logs;
DROP TABLE IF EXISTS aircraft;

CREATE TABLE aircraft (
  aircraft_id INT NOT NULL AUTO_INCREMENT,
  tail_number VARCHAR(10) NOT NULL, 
  model VARCHAR(50) NOT NULL,
  total_flight_hours DECIMAL(10,2) NOT NULL,
  PRIMARY KEY(aircraft_id)
);

CREATE TABLE flight_logs (
  flight_id INT NOT NULL AUTO_INCREMENT,
  aircraft_id INT NOT NULL,
  departure_airport CHAR(3) NOT NULL,
  arrival_airport CHAR(3) NOT NULL,
  departure_time DATETIME NOT NULL,
  arrival_time DATETIME NOT NULL,
  PRIMARY KEY(flight_id),
  FOREIGN KEY(aircraft_id) REFERENCES aircraft (aircraft_id) ON DELETE CASCADE
);

CREATE TABLE maintenance_events (
  event_id INT NOT NULL AUTO_INCREMENT,
  aircraft_id INT NOT NULL,
  event_date DATE NOT NULL,
  description TEXT NOT NULL,
  cost DECIMAL(12,2),
  PRIMARY KEY(event_id),
  FOREIGN KEY(aircraft_id) REFERENCES aircraft (aircraft_id) ON DELETE CASCADE
);