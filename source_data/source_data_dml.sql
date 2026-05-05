-- Insert new aircraft information
INSERT INTO aircraft (aircraft_id, tail_number, model, total_flight_hours)
VALUES
    (1, 'QF7333', 'Boeing 747', 3455.86);
    (2, 'SA9931', 'Bombardier CRJ900', 2657.24);
    (2, 'SA9931', 'Bombardier CRJ900', 2657.24);
    (3, 'AI6358', 'Airbus A320', 2291.07);
    (4, 'KL1090', 'Embraer E190', 2098.82);
    (5, 'DL5162', 'Bombardier CRJ900', 2838.22);
    (6, 'UA8290', 'Cessna 172', 2268.76);
    (7, 'KE7799', 'Boeing 747', 3470.25);
    (8, 'CX5047', 'Bombardier CRJ900', 2252.4);
    (9, 'QF4565', 'Cessna 172', 3195.39);
    (10, 'KE2765', 'Airbus A320', 2708.03);

INSERT INTO flight_logs (flight_id, aircraft_id, departure_airport, arrival_airport, departure_time, arrival_time)
VALUES
    (1, 1, 'JNB', 'CDG', '2026-04-04 11:47:06', '2026-04-04 23:15:06');
    (2, 1, 'SYD', 'BNE', '2026-04-06 02:33:26', '2026-04-06 18:55:26');
    (3, 1, 'LHR', 'CDG', '2026-04-17 17:57:01', '2026-04-17 23:42:01');
    (4, 1, 'FRA', 'CDG', '2026-04-28 19:36:29', '2026-04-28 21:35:29');
    (5, 1, 'ICN', 'HND', '2026-04-13 21:54:56', '2026-04-14 03:39:56');
    (6, 1, 'SIN', 'MEL', '2026-04-26 14:15:33', '2026-04-27 05:55:33');
    (7, 1, 'ICN', 'EZE', '2026-04-29 14:20:42', '2026-04-30 14:53:42');
    (8, 1, 'ADD', 'SFO', '2026-04-27 03:15:07', '2026-04-27 13:28:07');
    (9, 1, 'JFK', 'ATL', '2026-04-21 17:54:18', '2026-04-22 12:08:18');
    (10, 1, 'FCO', 'CDG', '2026-04-28 14:13:56', '2026-04-28 21:14:56');

INSERT INTO maintenance_events (event_id, aircraft_id, event_date, description, cost)
VALUES
    ()


-- Update an existing record
-- UPDATE null
-- SET null = null
-- WHERE null = null;

-- Delete a specific record
-- DELETE FROM null
-- WHERE null = null;

-- Query data
-- SELECT * FROM null
-- WHERE null = null;