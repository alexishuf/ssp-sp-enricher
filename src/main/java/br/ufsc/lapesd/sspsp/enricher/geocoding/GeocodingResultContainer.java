package br.ufsc.lapesd.sspsp.enricher.geocoding;

import com.google.maps.model.GeocodingResult;

public class GeocodingResultContainer {
    public final GeocodingResult[] results;

    public GeocodingResultContainer(GeocodingResult[] results) {
        this.results = results;
    }
}
