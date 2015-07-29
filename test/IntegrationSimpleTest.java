import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import play.Application;
import play.Logger;
import play.test.WithServer;

public class IntegrationSimpleTest extends WithServer {

	private static int PORT = 3333;

	@Inject
	Application application;

	private String	map			= "A B 10\nB D 15\nA C 20\nC D 30\nB E 50\nD E 30";
	private String	complexMap	= "A B 10\nA H 15\nB H 20\nB C 20\nC G 20\nB G 20\nH G 30\nH I 50\nG L 20\nG M 10\nM L 15\nF M 15\nC F 20\nC D 25\nD F 30\nD E 10\nE F 25\nE N 25\nN M 20\nN R 15\nM Q 25\nR Q 10\nL P 20\nQ P 15\nQ W 10\nR S 15\nR T 25\nT U 25\nR U 25\nU V 20\nV F1 15\nL J 40\nI J 30\nI H1 30\nJ H1 15\nP O 25\nH1 O 20\nA1 O 25\nW A1 20\nP W 10\nQ S 15\nS X 10\nW X 15\nX E1 15\nW E1 10\nA1 C1 10\nO B1 30\nB1 C1 50\nC1 D1 15\nC1 E1 20\nD1 G1 40\nC1 G1 30\nE1 F1 20\nF1 G1 25";

	@Before
	public void setUp() {
		Logger.debug("************ RUNNING INTEGRATION TESTS ************");
		RestAssured.port = PORT;
	}

	@Test
	public void simpleRestTest() {
		Logger.debug("************ INIT SIMPLE REST TEST ************");
		running(testServer(PORT), new Runnable() {
			@Override
			public void run() {
				InitTest(map, "A", "D", "A;B;D", "2.5", "25.0");
			}
		});
	}

	private void InitTest(String map, String origin, String destiny, String routeExpected, String costExpected, String distancyExpected) {
		long startTime = System.currentTimeMillis();
		Logger.debug("************ SEND MAP ************");
		String body = RestAssured.given().contentType(ContentType.TEXT).content(map).expect().statusCode(200).when().post("/process/SP").body().asString();

		assertThat(body, containsString("name"));

		Logger.debug("************ VERIFY BEST ROUTE " + origin + " TO " + destiny + " ************");
		String best = RestAssured.expect().statusCode(200).when().get("/best/SP/" + origin + "/" + destiny + "/10").body().asString();

		Logger.debug(String.format("Best Route: %s", best));

		assertThat(best, containsString(routeExpected));
		assertThat(best, containsString(costExpected));
		assertThat(best, containsString(distancyExpected));

		Logger.debug(String.format("Elapsed Time: %s ms", (System.currentTimeMillis() - startTime)));
	}

	@Test
	public void simpleRestReverseTest() {
		Logger.debug("************ INIT SIMPLE REVERSE TEST ************");
		running(testServer(PORT), new Runnable() {
			@Override
			public void run() {
				InitTest(map, "D", "A", "D;B;A", "2.5", "25.0");
			}
		});
	}
	
	@Test
	public void complexRestTest() {
		Logger.debug("************ INIT COMPLEX TEST ************");
		running(testServer(PORT), new Runnable() {
			@Override
			public void run() {
				InitTest(complexMap, "A", "G1", "A;B;G;M;Q;W;E1;F1;G1;", "130.0", "13.0");
			}
		});
	}

	@After
	public void tearDown() {
		RestAssured.reset();
	}
}
