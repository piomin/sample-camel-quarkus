package pl.piomin.services.meteo.route;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.model.rest.RestBindingMode;
import pl.piomin.services.meteo.model.RunningRatingRequest;
import pl.piomin.services.meteo.model.RunningRatingResponse;
import pl.piomin.services.meteo.model.airquality.AirQualityResponse;
import pl.piomin.services.meteo.model.geocoding.GeocodingResponse;
import pl.piomin.services.meteo.model.weather.WeatherResponse;
import pl.piomin.services.meteo.service.MeteoService;

@ApplicationScoped
public class MeteoRoute extends RouteBuilder {

    @Inject
    MeteoService meteoService;

    @Override
    public void configure() {
        restConfiguration()
            .component("platform-http")
            .bindingMode(RestBindingMode.json);

        rest("/rating")
            .post()
            .type(RunningRatingRequest.class)
            .outType(RunningRatingResponse.class)
            .to("direct:getRating");

        from("direct:getRating")
            .routeId("get-running-rating")
            .setProperty("request", simple("${body}"))
            .to("direct:geocode")
            .to("direct:airQuality")
            .to("direct:weather")
            .bean(meteoService, "buildResponse");

        from("direct:geocode")
            .removeHeaders("*")
            .setHeader(Exchange.HTTP_METHOD, constant("GET"))
            .setHeader(Exchange.HTTP_QUERY, simple(
                "name=${exchangeProperty.request.city}"
                + "&country_code=${exchangeProperty.request.country}"
                + "&count=1&language=en&format=json"))
            .setBody(constant(""))
            .to("https://geocoding-api.open-meteo.com/v1/search?bridgeEndpoint=true")
            .unmarshal(new JacksonDataFormat(GeocodingResponse.class))
            .choice()
                .when(simple("${body.results} == null"))
                    .throwException(new IllegalArgumentException("City not found"))
            .end()
            .setProperty("lat", simple("${body.results[0].latitude}"))
            .setProperty("lon", simple("${body.results[0].longitude}"))
            .setProperty("cityName", simple("${body.results[0].name}"));

        from("direct:airQuality")
            .removeHeaders("*")
            .setHeader(Exchange.HTTP_METHOD, constant("GET"))
            .setHeader(Exchange.HTTP_QUERY, simple(
                "latitude=${exchangeProperty.lat}"
                + "&longitude=${exchangeProperty.lon}"
                + "&current=european_aqi,pm2_5,pm10"))
            .setBody(constant(""))
            .to("https://air-quality-api.open-meteo.com/v1/air-quality?bridgeEndpoint=true")
            .unmarshal(new JacksonDataFormat(AirQualityResponse.class))
            .setProperty("aqi", simple("${body.current.europeanAqi}"));

        from("direct:weather")
            .removeHeaders("*")
            .setHeader(Exchange.HTTP_METHOD, constant("GET"))
            .setHeader(Exchange.HTTP_QUERY, simple(
                "latitude=${exchangeProperty.lat}"
                + "&longitude=${exchangeProperty.lon}"
                + "&current=temperature_2m,apparent_temperature,wind_speed_10m,precipitation,weather_code,cloud_cover"))
            .setBody(constant(""))
            .to("https://api.open-meteo.com/v1/forecast?bridgeEndpoint=true")
            .unmarshal(new JacksonDataFormat(WeatherResponse.class));
    }
}
