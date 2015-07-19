import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.restassured.RestAssured;

import models.ErrorMessage;
import play.libs.Json;
import play.test.WithServer;

public class IntegrationErrorTest extends WithServer {

	private int PORT = 3333;
	
	@Before
	public void setUp() {
		RestAssured.port = PORT;
	}
	
	@Test
	public void mustBeMapNotFoundTest() {
		running(testServer(PORT), new Runnable() {
			@Override
			public void run() {
				String best = RestAssured.expect()
						.statusCode(400)
						.when()
						.get("/best/BH/A/D/10")
						.body().asString();
				
				JsonNode json = Json.parse(best);
				ErrorMessage error = Json.fromJson(json, ErrorMessage.class);
				
				assertEquals(Integer.valueOf(400), error.getCode());
			}
		});
	}
	
	@Test
	public void mustBeActionNotFoundTest() {
		running(testServer(PORT), new Runnable() {
			@Override
			public void run() {
				String best = RestAssured.expect()
						.statusCode(404)
						.when()
						.get("/best/A/D/10")
						.body().asString();
				
				JsonNode json = Json.parse(best);
				ErrorMessage error = Json.fromJson(json, ErrorMessage.class);
				
				assertEquals(Integer.valueOf(404), error.getCode());
				assertThat(error.getMessage(), containsString("not found"));
			}
		});
	}
	
	@After
	public void tearDown() {
		RestAssured.reset();
	}
}

