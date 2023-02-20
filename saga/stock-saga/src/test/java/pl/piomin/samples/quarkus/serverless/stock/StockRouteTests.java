package pl.piomin.samples.quarkus.serverless.stock;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class StockRouteTests {

    @Test
    void reserve() {
        Order o = new Order();
        o.setId(1L);
        o.setStatus(OrderStatus.NEW);
        o.setProductId(1L);
        o.setProductCount(5);

        given().contentType(ContentType.JSON).body(o).post("/stock/reserve")
                .then()
                .statusCode(201);
    }

}
