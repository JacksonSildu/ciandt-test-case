package actors;

import static akka.pattern.Patterns.ask;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import org.hibernate.hql.internal.ast.InvalidPathException;

import actors.helpers.Load;
import actors.helpers.Persist;
import actors.helpers.Process;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import entity.Route;
import entity.RoutesMap;
import enums.StatusEnum;
import exception.InvalidProtocolException;
import models.CityRoute;
import models.Path;
import models.Request;
import models.RequestSiglePath;
import models.StatusMap;
import play.Logger;
import play.i18n.Messages;
import play.libs.F.Promise;

public class RoutesManager {
	private static final String	ERRO_ROUTE_MANAGER_GENERIC	= "erro.route.manager.generic";
	private static final String	ERROR_PROTOCOL_NOT_FOUND	= "error.protocol.not.found";
	private static final String	ERROR_PATH_NOT_FOUND		= "error.path.not.found";
	private static final String	ERROR_MAP_NOT_FOUND			= "error.map.not.found";

	private ActorSystem								system;
	private ActorRef								actor;
	private ConcurrentHashMap<String, Request>		maps;
	private ConcurrentHashMap<String, CityRoute>	bestPathsMaps;
	private ConcurrentHashMap<String, StatusMap>	loadMapStatus;
	private ArrayBlockingQueue<Request>				requestQueue;

	public static long TIMEOUT = 60 * 60 * 1000;

	public RoutesManager(ActorSystem system) {
		Logger.debug("Init the Routes Manager...");
		this.system = system;
		actor = system.actorOf(SupervisorActor.props, "supervisor");
		maps = new ConcurrentHashMap<>();
		bestPathsMaps = new ConcurrentHashMap<>();
		loadMapStatus = new ConcurrentHashMap<>();
		requestQueue = new ArrayBlockingQueue<>(1000);

		Executors.newFixedThreadPool(1).execute(new Runnable() {
			@Override
			public void run() {
				process();
			}
		});
	}

	public Path getSinglePath(String mapName, String origin, String destiny) throws InvalidPathException {
		if (maps.containsKey(mapName)) {
			Request request = maps.get(mapName);
			RequestSiglePath requestSiglePath = new RequestSiglePath();
			requestSiglePath.setDefaultRoutes(request.getDefaultRoutes());
			requestSiglePath.setDestiny(destiny);
			requestSiglePath.setMapName(mapName);
			requestSiglePath.setOrigin(origin);
			requestSiglePath.setPoints(request.getPoints());

			ActorRef actor = system.actorOf(SupervisorActor.props);
			Promise<Path> p = Promise.wrap(ask(actor, new Process<RequestSiglePath>(requestSiglePath), TIMEOUT)).map(response -> {
				return (Path) response;
			});

			return p.get(TIMEOUT);

		} else {
			Logger.debug("Invalid Map!!!");
			throw new InvalidPathException(Messages.get(ERROR_MAP_NOT_FOUND));
		}
	}

	public void processCityMap(Request request, boolean assinc) {

		if (loadMapStatus.containsKey(request.getMapName())) {
			loadMapStatus.get(request.getMapName()).setStatus(assinc ? StatusEnum.NO_PROCESSED : StatusEnum.WAITING);
		} else {
			StatusMap status = new StatusMap();
			status.setName(request.getMapName());
			status.setStatus(assinc ? StatusEnum.NO_PROCESSED : StatusEnum.WAITING);

			loadMapStatus.put(request.getMapName(), status);
		}

		maps.put(request.getMapName(), request);
		RoutesMap map = new RoutesMap();
		map.setId(request.getMapName());
		map.setRoutes(new ArrayList<>());
		request.getDefaultRoutes().forEach((route, distance) -> {
			Route r = new Route();
			r.setPath(route);
			r.setDistance(distance);

			map.getRoutes().add(r);

		});

		Promise.wrap(ask(actor, new Persist<RoutesMap>(map), TIMEOUT));

		if (assinc) {
			Logger.debug("Offer request to Process");
			requestQueue.offer(request);
		}
	}

	public void process() {
		try {
			while (true) {
				Request request = requestQueue.take();
				Logger.debug("Retrieve request to process");

				Logger.debug(String.format("Setting %s map to IN_PROCESS...", request.getMapName()));

				loadMapStatus.get(request.getMapName()).setStatus(StatusEnum.IN_PROCESS);

				Promise<CityRoute> p = Promise.wrap(ask(actor, new Process<Request>(request), TIMEOUT)).map(response -> {
					CityRoute city = (CityRoute) response;
					return city;
				});

				try {
					CityRoute city = p.get(TIMEOUT);

					if (bestPathsMaps.containsKey(request.getMapName())) {
						bestPathsMaps.remove(request.getMapName());
					}
					
					bestPathsMaps.putIfAbsent(request.getMapName(), city);

					Logger.debug(String.format("Setting %s map to PROCESSED...", request.getMapName()));
					loadMapStatus.get(request.getMapName()).setStatus(StatusEnum.PROCESSED);
				} catch (Exception e) {
					e.printStackTrace(System.err);
					Logger.debug(String.format("Setting %s map to FAILED...", request.getMapName()));
					loadMapStatus.get(request.getMapName()).setStatus(StatusEnum.FAILED);
					loadMapStatus.get(request.getMapName()).setMessage(e.getMessage());

					throw new RuntimeException(e);
				}
			}
		} catch (Exception e) {
			Logger.error(Messages.get(ERRO_ROUTE_MANAGER_GENERIC), e);
		}
	}

	public Path getBestRoute(String mapName, String origin, String destiny) throws InvalidPathException {
		Logger.debug(String.format("Retrieve %s Best Route for %s ; %s", mapName, origin, destiny));
		StatusMap status = loadMapStatus.get(mapName);

		switch (status.getStatus()) {
		case PROCESSED:
			if (bestPathsMaps.containsKey(mapName)) {
				Path best = bestPathsMaps.get(mapName).getBestPath(origin, destiny);
				if (best != null) {
					return best;
				} else {
					Logger.debug("Invalid Path!!!");
					throw new InvalidPathException(Messages.get(ERROR_PATH_NOT_FOUND));
				}
			} else {
				Logger.debug("Invalid Map!!!");
				throw new InvalidPathException(Messages.get(ERROR_MAP_NOT_FOUND));
			}
		default:
			return this.getSinglePath(mapName, origin, destiny);
		}

	}

	public StatusMap checkProtocol(String mapName) throws InvalidPathException {
		if (loadMapStatus.containsKey(mapName)) {
			return loadMapStatus.get(mapName);
		} else {
			throw new InvalidProtocolException(Messages.get(ERROR_PROTOCOL_NOT_FOUND));
		}
	}

	@SuppressWarnings("unchecked")
	public void loadMaps() {
		Logger.debug("Starting thread to load Maps from DataBase...");
		Promise<Map<String, CityRoute>> p = Promise.wrap(ask(actor, new Load<RoutesMap>(), TIMEOUT)).map(response -> {
			Map<String, CityRoute> maps = (Map<String, CityRoute>) response;

			return maps;
		});

		bestPathsMaps.keySet().forEach(key -> {
			StatusMap status = new StatusMap();
			status.setStatus(StatusEnum.PROCESSED);
			status.setName(key);

			loadMapStatus.putIfAbsent(key, status);
		});

		bestPathsMaps.putAll(p.get(RoutesManager.TIMEOUT));
		Logger.debug("Maps Loaded...");

	}

}
