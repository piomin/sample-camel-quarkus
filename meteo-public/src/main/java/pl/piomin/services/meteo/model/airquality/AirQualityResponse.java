package pl.piomin.services.meteo.model.airquality;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AirQualityResponse {

    private double latitude;
    private double longitude;
    private AirQualityCurrent current;

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public AirQualityCurrent getCurrent() { return current; }
    public void setCurrent(AirQualityCurrent current) { this.current = current; }
}
