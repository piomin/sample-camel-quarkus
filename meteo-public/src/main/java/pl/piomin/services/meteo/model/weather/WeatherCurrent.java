package pl.piomin.services.meteo.model.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherCurrent {

    private String time;

    @JsonProperty("temperature_2m")
    private double temperature2m;

    @JsonProperty("apparent_temperature")
    private double apparentTemperature;

    @JsonProperty("wind_speed_10m")
    private double windSpeed10m;

    private double precipitation;

    @JsonProperty("weather_code")
    private int weatherCode;

    @JsonProperty("cloud_cover")
    private int cloudCover;

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public double getTemperature2m() { return temperature2m; }
    public void setTemperature2m(double temperature2m) { this.temperature2m = temperature2m; }

    public double getApparentTemperature() { return apparentTemperature; }
    public void setApparentTemperature(double apparentTemperature) { this.apparentTemperature = apparentTemperature; }

    public double getWindSpeed10m() { return windSpeed10m; }
    public void setWindSpeed10m(double windSpeed10m) { this.windSpeed10m = windSpeed10m; }

    public double getPrecipitation() { return precipitation; }
    public void setPrecipitation(double precipitation) { this.precipitation = precipitation; }

    public int getWeatherCode() { return weatherCode; }
    public void setWeatherCode(int weatherCode) { this.weatherCode = weatherCode; }

    public int getCloudCover() { return cloudCover; }
    public void setCloudCover(int cloudCover) { this.cloudCover = cloudCover; }
}
