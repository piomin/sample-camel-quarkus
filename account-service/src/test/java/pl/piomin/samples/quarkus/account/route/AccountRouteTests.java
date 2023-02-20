package pl.piomin.samples.quarkus.account.route;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
public class AccountRouteTests {

    @Test
    void findAll() {
        given().get("/accounts")
                .then()
                .statusCode(200)
                .body("size()", is(3));
    }

    @Test
    void findById() {
        given().get("/accounts/{id}", 1)
                .then()
                .statusCode(200)
                .body("id", is(1));
    }

    @Test
    void findByCustomerId() {
        given().get("/accounts/customer/{customerId}", 2)
                .then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    void add() {
        AccountRoute.Account a = new AccountRoute.Account(null, "1234567890", 5000, 10);
        given().contentType(ContentType.JSON).body(a).post("/accounts")
                .then()
                .statusCode(200)
                .body("id", notNullValue());
    }
}
