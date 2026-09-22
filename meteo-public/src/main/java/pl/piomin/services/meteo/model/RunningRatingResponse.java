package pl.piomin.services.meteo.model;

public class RunningRatingResponse {

    private String city;
    private String country;
    private double latitude;
    private double longitude;
    private String rating;
    private int airQualityIndex;
    private double temperature;
    private double apparentTemperature;
    private double windSpeed;
    private double precipitation;
    private int weatherCode;
    private int cloudCover;
    private String description;

    public RunningRatingResponse() {
    }

    public RunningRatingResponse(String city, String country, double latitude, double longitude,
                                  String rating, int airQualityIndex, double temperature,
                                  double apparentTemperature, double windSpeed, double precipitation,
                                  int weatherCode, int cloudCover, String description) {
        this.city = city;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.rating = rating;
        this.airQualityIndex = airQualityIndex;
        this.temperature = temperature;
        this.apparentTemperature = apparentTemperature;
        this.windSpeed = windSpeed;
        this.precipitation = precipitation;
        this.weatherCode = weatherCode;
        this.cloudCover = cloudCover;
        this.description = description;
    }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }

    public int getAirQualityIndex() { return airQualityIndex; }
    public void setAirQualityIndex(int airQualityIndex) { this.airQualityIndex = airQualityIndex; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getApparentTemperature() { return apparentTemperature; }
    public void setApparentTemperature(double apparentTemperature) { this.apparentTemperature = apparentTemperature; }

    public double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(double windSpeed) { this.windSpeed = windSpeed; }

    public double getPrecipitation() { return precipitation; }
    public void setPrecipitation(double precipitation) { this.precipitation = precipitation; }

    public int getWeatherCode() { return weatherCode; }
    public void setWeatherCode(int weatherCode) { this.weatherCode = weatherCode; }

    public int getCloudCover() { return cloudCover; }
    public void setCloudCover(int cloudCover) { this.cloudCover = cloudCover; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
