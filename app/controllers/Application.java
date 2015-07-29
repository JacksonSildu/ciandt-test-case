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

@Singleton
public class Application extends Controller {

	private static final String WRAP_TEXT = "\n";

	private RoutesManager manager;

	public Result index() {
		return ok(index.render());
	}

	@Inject
	public Application(ActorSystem system) {
		Logger.debug("Application Starting...");
		manager = new RoutesManager(system);
		manager.loadMaps();
	}

	@BodyParser.Of(BodyParser.Text.class)
	public Result processMapAssinc(String mapName) {
		return this.sendMap(mapName, true);
	}
	
	@BodyParser.Of(BodyParser.Text.class)
	public Result processMap(String mapName) {
		return this.sendMap(mapName, false);
	}

	public Result sendMap(String mapName, boolean assinc) {
		try {
			Request request = this.getMapRequest(mapName);

			// Map best routes
			Logger.debug("Init processing of Map...");
			manager.processCityMap(request, assinc);

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

	private Request getMapRequest(String mapName) {
		Logger.debug("Mapping the request...");
		Map<String, Double> defaultRoutes = new HashMap<>();
		Set<String> points = new HashSet<>();
		String map = request().body().asText();

		for (String line : map.split(WRAP_TEXT)) {
			String[] array = line.trim().split(" ");

			String origin = array[0].toUpperCase();
			String destiny = array[1].toUpperCase();
			Double cost = Double.valueOf(array[2]);

			// Add route to default
			defaultRoutes.put(origin + destiny, cost);
			// Add Inverse route too.
			defaultRoutes.put(destiny + origin, cost);

			points.add(origin);
			points.add(destiny);
		}

		Request request = new Request();
		request.setMapName(mapName.toUpperCase());
		request.setDefaultRoutes(defaultRoutes);
		request.setPoints(points);
		return request;
	}

	public Result getBestRoute(String mapName, String origin, String destiny, Double autonomy) {
		try {
			Path best = manager.getBestRoute(mapName.toUpperCase(), origin.toUpperCase(), destiny.toUpperCase());
			PathResponse response = new PathResponse(best.getStringPath(), best.getActualDistance(), best.getActualDistance() / autonomy);
			return ok(Json.toJson(response).toString());
		} catch (Exception e) {
			ErrorMessage message = new ErrorMessage(badRequest().status(), e.getMessage());
			return badRequest(Json.toJson(message).toString());
		}
	}

	public Result checkProtocol(String uuidProtocol) {
		try {
			StatusMap status = manager.checkProtocol(uuidProtocol);
			return ok(Json.toJson(status).toString());
		} catch (Exception e) {
			ErrorMessage message = new ErrorMessage(badRequest().status(), e.getMessage());
			return badRequest(Json.toJson(message).toString());
		}
	}

}