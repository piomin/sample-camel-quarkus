package pl.piomin.samples.quarkus.serverless.order;

import com.github.piomin.entity.model.order.Order;
import com.github.piomin.entity.model.order.OrderStatus;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class OrderRouteTests {

    @Test
    void confirm() {
        Order o = new Order();
        o.setId(1L);
        o.setStatus(OrderStatus.IN_PROGRESS);

        given().contentType(ContentType.JSON).body(o).post("/orders/confirm")
                .then()
                .statusCode(201);
    }
}
