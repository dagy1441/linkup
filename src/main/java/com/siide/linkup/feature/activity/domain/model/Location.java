package com.siide.linkup.feature.activity.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/**
 * Geographic location value object. Coordinates are optional (organizers may only
 * provide a city + address line). City is required and normalised to lower-case for
 * case-insensitive filtering.
 */
@Embeddable
public class Location {

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "address_line", length = 250)
    private String addressLine;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    protected Location() {
        // JPA
    }

    private Location(String city, String addressLine, Double latitude, Double longitude) {
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("Location.city must not be blank");
        }
        if (latitude != null && (latitude < -90 || latitude > 90)) {
            throw new IllegalArgumentException("Location.latitude must be in [-90, 90]");
        }
        if (longitude != null && (longitude < -180 || longitude > 180)) {
            throw new IllegalArgumentException("Location.longitude must be in [-180, 180]");
        }
        this.city = city.trim();
        this.addressLine = addressLine == null || addressLine.isBlank() ? null : addressLine.trim();
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static Location of(String city, String addressLine, Double latitude, Double longitude) {
        return new Location(city, addressLine, latitude, longitude);
    }

    public static Location ofCity(String city) {
        return new Location(city, null, null, null);
    }

    public String getCity() { return city; }
    public String getAddressLine() { return addressLine; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location other)) return false;
        return Objects.equals(city, other.city)
                && Objects.equals(addressLine, other.addressLine)
                && Objects.equals(latitude, other.latitude)
                && Objects.equals(longitude, other.longitude);
    }

    @Override
    public int hashCode() {
        return Objects.hash(city, addressLine, latitude, longitude);
    }
}
