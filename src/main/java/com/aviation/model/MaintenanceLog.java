package com.aviation.model;

import lombok.Data;

@Data
public class MaintenanceLog {

    private String icao24;
    private String serviceType;
    private String status;
    private String logDate;

}
