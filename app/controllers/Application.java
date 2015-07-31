package controllers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.JsonNode;

import actors.RoutesManager;
import akka.actor.ActorSystem;
import models.ErrorMessage;
import models.Path;
import models.PathResponse;
import models.ProtocolResponse;
import models.Request;
import models.StatusMap;
import play.Logger;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;

/**
 * Main class of application
 * 
 * <p>
 * This class contains the rest's methods mapped on route's configuration file.
 * 
 * @author Sildu
 *
 */
@Singleton
public class Application extends Controller {

	private static final String	SEMICOLON	= ";";
	private static final String	WRAP_TEXT	= "\n";

	private RoutesManager manager;

	public Result index() {
		return ok(index.render());
	}

	@Inject
	public Application(ActorSystem system) {
		Logger.debug("Application Starting...");
		// Inject the Actor System for create actors
		manager = new RoutesManager(system);
		// Load the existing maps
		manager.loadMaps();
	}

	/**
	 * Processes the entire map by mapping the best routes for each path provided.
	 * <p>
	 * retrieves the payload routes in the following format
	 * <p>
	 * Method: POST<br>
	 * Type: text/plain <br>
	 * A B 10 <br>
	 * A H 15 <br>
	 * B H 20 <br>
	 * B C 20 <br>
	 * C G 20 <br>
	 * B G 20 <br>
	 * <p>
	 * 
	 * @param mapName
	 *            Name of map
	 * @return
	 */
	@BodyParser.Of(BodyParser.Text.class)
	public Result processMapAsync(String mapName) {
		// Process the map
		return this.process(mapName, true);
	}

	/**
	 * Records map without processing routes
	 * <p>
	 * retrieves the payload routes in the following format
	 * <p>
	 * Method: POST<br>
	 * Type: text/plain <br>
	 * A B 10 <br>
	 * A H 15 <br>
	 * B H 20 <br>
	 * B C 20 <br>
	 * C G 20 <br>
	 * B G 20 <br>
	 * <p>
	 * 
	 * @param mapName
	 *            Name of map
	 * @return
	 * 
	 */
	@BodyParser.Of(BodyParser.Text.class)
	public Result processMap(String mapName) {
		// process the map
		return this.process(mapName, false);
	}

	/**
	 * Processes the received map. The processing can be asynchronous, which in this case will map
	 * out the best routes for each point informed.
	 * 
	 * @param mapName
	 *            Name of Map
	 * @param async
	 *            checks whether the processing is asynchronous
	 * @return return 200 code
	 */
	public Result process(String mapName, boolean async) {
		try {
			Request request = this.getMapRequest(mapName);

			Logger.debug("Init processing of Map...");
			// send the map to Manager
			manager.processCityMap(request, async);

			// Create a protocol to send a user
			ProtocolResponse response = new ProtocolResponse();
			response.setName(mapName.toUpperCase());

			Logger.debug("Send protocol to user...");
			JsonNode json = Json.toJson(response);
			return ok(json.toString());
		} catch (Exception e) {
			Logger.error("An Error occurred: ", e);
			ErrorMessage message = new ErrorMessage(badRequest().status(), e.getMessage());
			return badRequest(Json.toJson(message).toString());
		}

	}

	/**
	 * Retrieves a request object to map processing.
	 * 
	 * @param mapName
	 *            Name of map.
	 * @return returns a Request object
	 */
	private Request getMapRequest(String mapName) {
		Logger.debug("Mapping the request...");
		// Create a object Map with the routes
		Map<String, Double> defaultRoutes = new HashMap<>();
		Set<String> points = new HashSet<>();

		// Retrieve the map in payload
		String map = request().body().asText();

		for (String line : map.split(WRAP_TEXT)) {
			String[] array = line.trim().split(" ");

			String origin = array[0].toUpperCase();
			String destiny = array[1].toUpperCase();
			Double cost = Double.valueOf(array[2]);

			// Add route to default
			defaultRoutes.put(origin + SEMICOLON + destiny, cost);
			// Add Inverse route too.
			defaultRoutes.put(destiny + SEMICOLON + origin, cost);

			points.add(origin);
			points.add(destiny);
		}

		// Create a request Object
		Request request = new Request();
		request.setMapName(mapName.toUpperCase());
		request.setDefaultRoutes(defaultRoutes);
		request.setPoints(points);

		return request;
	}

	/**
	 * Retrieves the best route for the path of a map previously sent.
	 * 
	 * @param mapName
	 *            Name of map.
	 * @param origin
	 *            origin point
	 * @param destiny
	 *            destiny point
	 * @param autonomy
	 *            Vehicle autonomy
	 * @return Return a code 200 with the best route in payload
	 * 
	 */
	public Result getBestRoute(String mapName, String origin, String destiny, Double autonomy) {
		try {
			// Call a best route
			Path best = manager.getBestRoute(mapName.toUpperCase(), origin.toUpperCase(), destiny.toUpperCase());

			// create a response for user
			PathResponse response = new PathResponse(best.getStringPath(), best.getActualDistance(), best.getActualDistance() / autonomy);
			return ok(Json.toJson(response).toString());
		} catch (Exception e) {
			ErrorMessage message = new ErrorMessage(badRequest().status(), e.getMessage());
			return badRequest(Json.toJson(message).toString());
		}
	}

	/**
	 * checks the process of the map
	 * 
	 * @param mapName
	 *            Name of the map
	 * @return Return the status of map
	 */
	public Result checkProtocol(String mapName) {
		try {
			// Check map
			StatusMap status = manager.checkProtocol(mapName);
			return ok(Json.toJson(status).toString());
		} catch (Exception e) {
			ErrorMessage message = new ErrorMessage(badRequest().status(), e.getMessage());
			return badRequest(Json.toJson(message).toString());
		}
	}

}