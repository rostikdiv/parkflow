package com.parkflow.inventory.api.dto;

import java.util.Map;

public record GeoJsonFeature(
        String type,
        GeoJsonGeometry geometry,
        Map<String, Object> properties
) {
    public static GeoJsonFeature of(GeoJsonGeometry geometry, Map<String, Object> properties) {
        return new GeoJsonFeature("Feature", geometry, properties);
    }
}
