package com.aviation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/maintenance")
public class RawMaintenanceController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/import")
    public String importLogs() {
        String csvFile = "raw_maintenance_logs.csv";
        String line;
        String csvSplitBy = ",";
        String sql = "INSERT INTO raw_maintenance (icao24, service_type, status, log_date) VALUES (?, ?, ?, ?)";

        // Lines counts for incomplete lines due to missing data
        int processedCount = 0;
        int skippedCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            br.readLine();

            List<Object[]> batchArgs = new ArrayList<>();

            while ((line = br.readLine()) != null) {

                if (line.trim().isEmpty())
                    continue;

                String[] data = line.split(csvSplitBy);

                // Mapping CSV columns: 0:icao24, 1:service_type, 2: status, 3:log_date
                // But first ensure icao24 is present to satisfy NOT NULL
                if (data.length >= 1 && !data[0].trim().isEmpty()) {
                    String icao24 = data[0];
                    String service_type = (data.length > 1) ? data[1] : null;
                    String status = (data.length > 2) ? data[2] : null;

                    Object log_date = null;
                    if (data.length > 3 && data[3].isEmpty()) {
                        try {
                            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
                                    .ofPattern("yyyy-MM-dd HH:mm:ss");
                            log_date = java.time.LocalDateTime.parse(data[3], formatter);
                        } catch (Exception e) {
                            System.err.println("Date parsing failed for: " + data[3]);
                        }
                    }
                    batchArgs.add(new Object[] { icao24, service_type, status, log_date });
                    processedCount++;
                } else {
                    System.err.println("Skipping invalid row (missing ICAO24):  Line " + line);
                }
            }
            if (!batchArgs.isEmpty()) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
            }
            return String.format("Import finished. Success: %d, Skipped: %d", processedCount, skippedCount);
        } catch (Exception e) {
            return "Critical error during import: " + e.getMessage();
        }
    }
}
