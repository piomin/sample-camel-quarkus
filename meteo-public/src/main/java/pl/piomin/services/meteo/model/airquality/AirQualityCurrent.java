package pl.piomin.services.meteo.model.airquality;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AirQualityCurrent {

    private String time;

    @JsonProperty("european_aqi")
    private int europeanAqi;

    @JsonProperty("pm2_5")
    private double pm25;

    private double pm10;

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public int getEuropeanAqi() { return europeanAqi; }
    public void setEuropeanAqi(int europeanAqi) { this.europeanAqi = europeanAqi; }

    public double getPm25() { return pm25; }
    public void setPm25(double pm25) { this.pm25 = pm25; }

    public double getPm10() { return pm10; }
    public void setPm10(double pm10) { this.pm10 = pm10; }
}
