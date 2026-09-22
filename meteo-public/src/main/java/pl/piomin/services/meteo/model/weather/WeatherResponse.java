package pl.piomin.services.meteo.model.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherResponse {

    private double latitude;
    private double longitude;
    private WeatherCurrent current;

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public WeatherCurrent getCurrent() { return current; }
    public void setCurrent(WeatherCurrent current) { this.current = current; }
}
