package com.parkflow.inventory.api.dto;

import java.util.List;

public record GeoJsonGeometry(
        String type,
        List<Double> coordinates
) {
    public static GeoJsonGeometry point(Double longitude, Double latitude) {
        return new GeoJsonGeometry("Point", List.of(longitude, latitude));
    }
}
