package com.gavi.microservices.product_service;

import com.gavi.microservices.product_service.repository.ProductRepository;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.mongodb.MongoDBContainer;
import io.restassured.RestAssured;

import static org.hamcrest.Matchers.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductServiceApplicationTests {

	@ServiceConnection
	static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0.5");

	@LocalServerPort
	private int port;

	@Autowired
	ProductRepository productRepository;

	@BeforeEach
	void cleanDb() {
		productRepository.deleteAll();
	}

	@BeforeEach
	void setup() {
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = port;
	}

	@Test
	void shouldCreateProduct() {
		String requestBody = """
				{
				    "name": "IPhone 17",
				    "description":"IPhone 17 is smartphone for apple produced in India",
				    "price": 1000
				}
				""";
		RestAssured.given()
				.contentType(ContentType.JSON)
				.body(requestBody)
				.when()
				.post("/api/product")
				.then()
				.statusCode(201)
				.body("id", Matchers.notNullValue())
				.body("name", equalTo("IPhone 17"))
				.body("description", equalTo("IPhone 17 is smartphone for apple produced in India"))
				.body("price", equalTo(1000));
	}

	@Test
	void shouldGetAllProducts() {
		String requestBody = """
        {
            "name": "Pixel 9",
            "description":"Google phone",
            "price": 900
        }
        """;

		RestAssured.given()
				.contentType(ContentType.JSON)
				.body(requestBody)
				.when()
				.post("/api/product")
				.then()
				.statusCode(201);

		// Call GET API
		RestAssured.given()
				.when()
				.get("/api/product")
				.then()
				.statusCode(200)
				.body("size()", greaterThan(0));

	}

	@Test
	void shouldFailWhenNameIsMissing() {
		String requestBody = """
        {
            "description":"Missing name",
            "price": 500
        }
        """;

		RestAssured.given()
				.contentType(ContentType.JSON)
				.body(requestBody)
				.when()
				.post("/api/product")
				.then()
				.statusCode(500);
	}

	@Test
	void shouldReturnEmptyList() {
		RestAssured.given()
				.when()
				.get("/api/product")
				.then()
				.statusCode(200)
				.body("size()", equalTo(0));
	}

	@Test
	void shouldReturnAllProducts() {
		createProduct("Pixel", "Google phone", 900);

		RestAssured.given()
				.when()
				.get("/api/product")
				.then()
				.statusCode(200)
				.body("size()", greaterThan(0));
	}

	@Test
	void shouldHandleMultipleProducts() {
		createProduct("P1", "Test", 100);
		createProduct("P2", "Test", 200);

		RestAssured.given()
				.when()
				.get("/api/product")
				.then()
				.statusCode(200)
				.body("size()", greaterThanOrEqualTo(2));
	}

	@Test
	void shouldFailForInvalidJson() {
		String invalidJson = "{ name: invalid json }";

		RestAssured.given()
				.contentType(ContentType.JSON)
				.body(invalidJson)
				.when()
				.post("/api/product")
				.then()
				.statusCode(400);
	}




	private String createProduct(String name, String description, int price) {
		String body = """
        {
            "name": "%s",
            "description":"%s",
            "price": %d
        }
        """.formatted(name, description, price);

		return RestAssured.given()
				.contentType(ContentType.JSON)
				.body(body)
				.when()
				.post("/api/product")
				.then()
				.extract()
				.path("id");
	}
}
