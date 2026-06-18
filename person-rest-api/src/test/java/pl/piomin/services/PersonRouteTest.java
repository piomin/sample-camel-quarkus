package pl.piomin.services;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PersonRouteTest {

    static Long personId;

    @Test
    @Order(1)
    void createPerson() {
        String body = """
                {"firstName":"John","lastName":"Doe","email":"john.doe@example.com","age":30,"gender":"M"}
                """;
        personId = given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/persons")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("firstName", is("John"))
                .body("lastName", is("Doe"))
                .body("email", is("john.doe@example.com"))
                .body("age", is(30))
                .extract().jsonPath().getLong("id");
    }

    @Test
    @Order(2)
    void findAllPersons() {
        given()
                .get("/persons")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0))
                .body("firstName", hasItem("John"));
    }

    @Test
    @Order(3)
    void findPersonById() {
        given()
                .get("/persons/{id}", personId)
                .then()
                .statusCode(200)
                .body("id", is(personId.intValue()))
                .body("firstName", is("John"))
                .body("lastName", is("Doe"))
                .body("email", is("john.doe@example.com"));
    }

    @Test
    @Order(4)
    void updatePerson() {
        String body = """
                {"firstName":"Jane","lastName":"Smith","email":"jane.smith@example.com","age":28,"gender":"F"}
                """;
        given()
                .contentType(ContentType.JSON)
                .body(body)
                .put("/persons/{id}", personId)
                .then()
                .statusCode(200)
                .body("id", is(personId.intValue()))
                .body("firstName", is("Jane"))
                .body("lastName", is("Smith"))
                .body("age", is(28));
    }

    @Test
    @Order(5)
    void deletePerson() {
        given()
                .delete("/persons/{id}", personId)
                .then()
                .statusCode(204);

        given()
                .get("/persons")
                .then()
                .statusCode(200)
                .body("id", not(hasItem(personId.intValue())));
    }
}
