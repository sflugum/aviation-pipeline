package com.pipeline.model;

import tools.jackson.databind.JsonNode;

public class FlightRecord {

    private String icao24;
    private String callsign;
    private String originCountry;
    private Long timePosition; // Unix timestamp
    private Long lastContact; // Unix timestamp
    private Double longitude;
    private Double latitude;
    private Double baroAltitude;
    private Boolean onGround;
    private Double velocity;
    private Double trueTrack;
    private Double verticalRate;
    private Double geoAltitude;
    private String squawk;
    private Boolean spi;
    private Integer positionSource;

    public Integer getPositionSource() {
        return positionSource;
    }

    public Boolean getSpi() {
        return spi;
    }

    public String getSquawk() {
        return squawk;
    }

    public Double getGeoAltitude() {
        return geoAltitude;
    }

    public Double getVerticalRate() {
        return verticalRate;
    }

    public Double getTrueTrack() {
        return trueTrack;
    }

    public Double getVelocity() {
        return velocity;
    }

    public Boolean getOnGround() {
        return onGround;
    }

    public Double getBaroAltitude() {
        return baroAltitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Long getLastContact() {
        return lastContact;
    }

    public Long getTimePosition() {
        return timePosition;
    }

    public String getOriginCountry() {
        return originCountry;
    }

    public String getCallsign() {
        return callsign;
    }

    public String getIcao24() {
        return icao24;
    }

    public FlightRecord(JsonNode node) {
        this.icao24 = parseString(node.get(0));
        this.callsign = parseString(node.get(1));
        this.originCountry = parseString(node.get(2));
        this.timePosition = parseLong(node.get(3));
        this.lastContact = parseLong(node.get(4));
        this.longitude = parseDouble(node.get(5));
        this.latitude = parseDouble(node.get(6));
        this.baroAltitude = parseDouble(node.get(7));
        this.onGround = parseBoolean(node.get(8));
        this.velocity = parseDouble(node.get(9));
        this.trueTrack = parseDouble(node.get(10));
        this.verticalRate = parseDouble(node.get(11));
        // Index 12 is sensors (int array), skipping for now unless needed
        this.geoAltitude = parseDouble(node.get(13));
        this.squawk = parseString(node.get(14));
        this.spi = parseBoolean(node.get(15));
        this.positionSource = parseInteger(node.get(16));
    }

    private String parseString(JsonNode node) {
        return (node == null || node.isNull()) ? null : node.asText().trim();
    }

    private Long parseLong(JsonNode node) {
        return (node == null || node.isNull()) ? null : node.asLong();
    }

    private Double parseDouble(JsonNode node)  {
        return (node == null || node.isNull()) ? null : node.asDouble();
    }

    private Boolean parseBoolean(JsonNode node) {
        return (node != null && !node.isNull()) && node.asBoolean();
    }

    private Integer parseInteger(JsonNode node) {
        return (node == null || node.isNull()) ? null : node.asInt();
    }

}
