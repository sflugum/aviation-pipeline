package com.aviation.model;

import java.time.*;
import lombok.Data;

@Data
public class MaintenanceLog {

    private String icao24;
    private String serviceType;
    private String status;
    private LocalDate logDate;


    public MaintenanceLog(String icao24, String serviceType, String status, LocalDate logDate) {
        this.icao24 = icao24;
        this.serviceType = serviceType;
        this.status = status;
        this.logDate = logDate;
    }
}
