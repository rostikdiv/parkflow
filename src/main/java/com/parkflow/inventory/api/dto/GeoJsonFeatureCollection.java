package com.parkflow.inventory.api.dto;

import java.util.List;

public record GeoJsonFeatureCollection(
        String type,
        List<GeoJsonFeature> features
) {
    public static GeoJsonFeatureCollection of(List<GeoJsonFeature> features) {
        return new GeoJsonFeatureCollection("FeatureCollection", features);
    }
}
