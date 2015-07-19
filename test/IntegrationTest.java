import static play.test.Helpers.running;
import static play.test.Helpers.testServer;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import models.ProtocolResponse;
import play.Application;
import play.libs.Json;
import play.test.WithServer;

public class IntegrationTest extends WithServer {

	/**
	 * add your integration test here in this example we just check if the welcome page is being shown
	 */
	@Inject
	Application application;

	private int PORT = 3333;
	
	private String map = "A B 10\nB D 15\nA C 20\nC D 30\nB E 50\nD E 30";

	@Before
	public void setUp() {
		RestAssured.port = PORT;
	}
	
	@Test
	public void simpleRestTest() {
		running(testServer(PORT), new Runnable() {
			@Override
			public void run() {
				String body = RestAssured.given()
	                .contentType(ContentType.TEXT)
	                .content(map)
	                .expect()
	                .statusCode(200)
	                .when()
	                .post("/send/BH")
	                .body()
	                .asString();
				
				assertThat(body, containsString("uuidProtocol"));
				
				JsonNode json = Json.parse(body);
				ProtocolResponse protocol = Json.fromJson(json, ProtocolResponse.class);
				
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
				}
				
				String check = RestAssured.expect()
		                .statusCode(200)
		                .when()
		                .get("/check/" + protocol.getUuidProtocol())
		                .body()
		                .asString();
				
				assertThat(check, containsString("PROCESSED"));
				
				String best = RestAssured.expect()
						.statusCode(200)
						.when()
						.get("/best/BH/A/D/10")
						.body().asString();
				
				assertThat(best, containsString("ABD"));
				assertThat(best, containsString("2.5"));
				assertThat(best, containsString("25.0"));
			}
		});
	}
	
	@Test
	public void simpleRestReverseTest() {
		running(testServer(PORT), new Runnable() {
			@Override
			public void run() {
				String body = RestAssured.given()
	                .contentType(ContentType.TEXT)
	                .content(map)
	                .expect()
	                .statusCode(200)
	                .when()
	                .post("/send/BH")
	                .body()
	                .asString();
				
				assertThat(body, containsString("uuidProtocol"));
				
				JsonNode json = Json.parse(body);
				ProtocolResponse protocol = Json.fromJson(json, ProtocolResponse.class);
				
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
				}
				
				String check = RestAssured.expect()
		                .statusCode(200)
		                .when()
		                .get("/check/" + protocol.getUuidProtocol())
		                .body()
		                .asString();
				
				assertThat(check, containsString("PROCESSED"));
				
				String best = RestAssured.expect()
						.statusCode(200)
						.when()
						.get("/best/BH/D/A/10")
						.body().asString();
				
				
				assertThat(best, containsString("DBA"));
				assertThat(best, containsString("2.5"));
				assertThat(best, containsString("25.0"));
			}
		});
	}


	@After
	public void tearDown() {
		RestAssured.reset();
	}
}
