package pl.piomin.services.meteo;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class MeteoRouteTest {

    @Test
    void getRatingForWarsaw() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"city":"Warsaw","country":"PL"}
                """)
        .when()
            .post("/rating")
        .then()
            .statusCode(200)
            .body("city", is("Warsaw"))
            .body("country", is("PL"))
            .body("rating", matchesPattern("[A-E][+-]?"))
            .body("airQualityIndex", greaterThanOrEqualTo(0))
            .body("temperature", notNullValue())
            .body("apparentTemperature", notNullValue())
            .body("windSpeed", greaterThanOrEqualTo(0.0f))
            .body("precipitation", greaterThanOrEqualTo(0.0f))
            .body("cloudCover", both(greaterThanOrEqualTo(0)).and(lessThanOrEqualTo(100)))
            .body("description", notNullValue());
    }
}
