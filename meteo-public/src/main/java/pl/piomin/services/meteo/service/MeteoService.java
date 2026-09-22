package pl.piomin.services.meteo.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Body;
import org.apache.camel.ExchangeProperty;
import pl.piomin.services.meteo.model.RunningRatingRequest;
import pl.piomin.services.meteo.model.RunningRatingResponse;
import pl.piomin.services.meteo.model.weather.WeatherCurrent;
import pl.piomin.services.meteo.model.weather.WeatherResponse;

@ApplicationScoped
public class MeteoService {

    public RunningRatingResponse buildResponse(
            @Body WeatherResponse weather,
            @ExchangeProperty("request") RunningRatingRequest req,
            @ExchangeProperty("aqi") int aqi,
            @ExchangeProperty("lat") double lat,
            @ExchangeProperty("lon") double lon,
            @ExchangeProperty("cityName") String cityName) {
        WeatherCurrent wc = weather.getCurrent();
        int score = Math.max(0, aqiScore(aqi)
            + tempScore(wc.getApparentTemperature())
            + windScore(wc.getWindSpeed10m())
            + precipScore(wc.getPrecipitation())
            + heatSunPenalty(wc.getTemperature2m(), wc.getCloudCover())
            + weatherCodePenalty(wc.getWeatherCode()));
        String rating = toRating(score);
        return new RunningRatingResponse(
            cityName, req.getCountry(), lat, lon, rating, aqi,
            wc.getTemperature2m(), wc.getApparentTemperature(),
            wc.getWindSpeed10m(), wc.getPrecipitation(),
            wc.getWeatherCode(), wc.getCloudCover(),
            describeRating(rating));
    }

    // Max 35 pts — air quality is the most important factor for outdoor running
    private int aqiScore(int aqi) {
        if (aqi <= 15) return 35;
        if (aqi <= 25) return 30;
        if (aqi <= 35) return 24;
        if (aqi <= 50) return 16;
        if (aqi <= 65) return 8;
        if (aqi <= 80) return 2;
        return -5;
    }

    // Max 30 pts — uses apparent temperature (feels-like), optimal 8-18°C for running
    private int tempScore(double t) {
        if (t >= 8  && t <= 18) return 30;
        if (t >= 5  && t <= 22) return 24;
        if (t >= 2  && t <= 27) return 16;
        if (t >= 0  && t <= 32) return 8;
        if (t >= -2 && t <= 37) return 2;
        return 0;
    }

    // Max 15 pts — wind above 40 km/h is dangerous for running
    private int windScore(double wind) {
        if (wind < 12) return 15;
        if (wind < 20) return 11;
        if (wind < 30) return 6;
        if (wind < 40) return 1;
        return -5;
    }

    // Max 20 pts
    private int precipScore(double precip) {
        if (precip == 0.0) return 20;
        if (precip <= 0.1) return 13;
        if (precip <= 0.5) return 5;
        if (precip <= 2.0) return -2;
        if (precip <= 5.0) return -8;
        return -15;
    }

    // Penalty for running in direct heat and sunshine — even if AQI and temp are ok individually,
    // solar radiation significantly raises perceived effort and dehydration risk
    private int heatSunPenalty(double temp, int cloudCover) {
        if (temp > 32 && cloudCover < 40) return -15;
        if (temp > 28 && cloudCover < 25) return -10;
        if (temp > 25 && cloudCover < 20) return -5;
        return 0;
    }

    // Penalty for dangerous or visibility-reducing conditions not captured by precipitation
    private int weatherCodePenalty(int code) {
        if (code >= 95) return -10; // thunderstorm — dangerous regardless of current precipitation
        if (code >= 45 && code <= 48) return -5; // fog — low visibility
        return 0;
    }

    // 13-level rating from 0-100 score
    private String toRating(int score) {
        if (score >= 92) return "A+";
        if (score >= 84) return "A";
        if (score >= 76) return "A-";
        if (score >= 68) return "B+";
        if (score >= 60) return "B";
        if (score >= 52) return "B-";
        if (score >= 44) return "C+";
        if (score >= 36) return "C";
        if (score >= 28) return "C-";
        if (score >= 22) return "D+";
        if (score >= 16) return "D";
        if (score >= 8)  return "D-";
        return "E";
    }

    private String describeRating(String rating) {
        return switch (rating) {
            case "A+" -> "Perfect conditions for running - enjoy!";
            case "A"  -> "Excellent conditions for running";
            case "A-" -> "Very good conditions, minor factors to consider";
            case "B+" -> "Good conditions for running";
            case "B"  -> "Decent conditions, some discomfort possible";
            case "B-" -> "Acceptable, but not ideal - pace yourself";
            case "C+" -> "Moderate conditions - consider a shorter run";
            case "C"  -> "Moderate conditions - extra effort required";
            case "C-" -> "Below average - hydrate well and go slow";
            case "D+" -> "Poor conditions - not recommended";
            case "D"  -> "Poor conditions - consider indoor training";
            case "D-" -> "Very poor - indoor training strongly advised";
            default   -> "Extreme conditions - do not run outside";
        };
    }
}
