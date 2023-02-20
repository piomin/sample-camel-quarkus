package pl.piomin.samples.quarkus.serverless.customer;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class CustomerRouteTests {

    @Test
    void reserve() {
        Order o = new Order();
        o.setId(1L);
        o.setCustomerId(1L);
        o.setStatus(OrderStatus.NEW);
        o.setAmount(100);

        given().contentType(ContentType.JSON).body(o).post("/customers/reserve")
                .then()
                .statusCode(201);
    }
}
