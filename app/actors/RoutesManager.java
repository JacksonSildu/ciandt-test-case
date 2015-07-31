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

/**
 * Class responsible for managing sent maps and map out the best routes.
 * 
 * @author Sildu
 *
 */
public class RoutesManager {
	private static final String	ERRO_ROUTE_MANAGER_GENERIC	= "erro.route.manager.generic";
	private static final String	ERROR_PROTOCOL_NOT_FOUND	= "error.protocol.not.found";
	private static final String	ERROR_PATH_NOT_FOUND		= "error.path.not.found";
	private static final String	ERROR_MAP_NOT_FOUND			= "error.map.not.found";

	private ActorSystem								system;
	private ActorRef								actor;
	private ConcurrentHashMap<String, CityRoute>	maps;
	private ConcurrentHashMap<String, StatusMap>	loadMapStatus;
	private ArrayBlockingQueue<Request>				requestQueue;

	// limit time for process
	public static long TIMEOUT = 60 * 60 * 1000;

	public RoutesManager(ActorSystem system) {
		Logger.debug("Init the Routes Manager...");
		this.system = system;
		// Create a supervisor actor
		actor = system.actorOf(SupervisorActor.props, "supervisor");
		// create a concurrentMap to store maps.
		maps = new ConcurrentHashMap<>();
		// Create a concurrentMap for statusMap
		loadMapStatus = new ConcurrentHashMap<>();
		// create a queue for process each map
		requestQueue = new ArrayBlockingQueue<>(1000);

		// Create a thread for process queue maps
		Executors.newFixedThreadPool(1).execute(new Runnable() {
			@Override
			public void run() {
				try {
					while (true) {
						process();
					}
				} catch (Exception e) {
					Logger.error(Messages.get(ERRO_ROUTE_MANAGER_GENERIC), e);
				}
			}
		});
	}

	/**
	 * retrieves the best way for a non-processed map
	 * 
	 * @param mapName
	 *            Name of map
	 * @param origin
	 *            Origin point
	 * @param destiny
	 *            Destiny point
	 * @return Return for best route to path
	 * @throws InvalidPathException
	 */
	public Path getSinglePath(String mapName, String origin, String destiny) throws InvalidPathException {
		if (maps.containsKey(mapName)) {
			// create a request for process
			RequestSiglePath requestSiglePath = this.newRequestSinglePath(mapName, origin, destiny);

			// create a new supervisor actor for process
			ActorRef actor = system.actorOf(SupervisorActor.props);

			// Send a request for the actor
			Promise<Path> p = Promise.wrap(ask(actor, new Process<RequestSiglePath>(requestSiglePath), TIMEOUT)).map(response -> {
				return (Path) response;
			});

			// wait a response
			return p.get(TIMEOUT);

		} else {
			Logger.debug("Invalid Map!!!");
			throw new InvalidPathException(Messages.get(ERROR_MAP_NOT_FOUND));
		}
	}

	/**
	 * Create a new object request for process the path
	 * 
	 * @param mapName
	 *            Name of Map
	 * @param origin
	 *            Origin point
	 * @param destiny
	 *            Destiny Point
	 * @return Return a request object
	 */
	private RequestSiglePath newRequestSinglePath(String mapName, String origin, String destiny) {
		Request request = maps.get(mapName).getMaps();
		RequestSiglePath requestSiglePath = new RequestSiglePath();
		requestSiglePath.setDefaultRoutes(request.getDefaultRoutes());
		requestSiglePath.setDestiny(destiny);
		requestSiglePath.setMapName(mapName);
		requestSiglePath.setOrigin(origin);
		requestSiglePath.setPoints(request.getPoints());
		return requestSiglePath;
	}

	/**
	 * Send the request to the supervisor process map. If asynchronous, will map out the best routes
	 * to the roads.
	 * 
	 * @param request
	 *            Request object for process
	 * @param async
	 *            if true process the best routes for map
	 */
	public void processCityMap(Request request, boolean async) {

		// store a status for map
		if (loadMapStatus.containsKey(request.getMapName())) {
			loadMapStatus.get(request.getMapName()).setStatus(async ? StatusEnum.NO_PROCESSED : StatusEnum.WAITING);
		} else {
			StatusMap status = new StatusMap();
			status.setName(request.getMapName());
			status.setStatus(async ? StatusEnum.NO_PROCESSED : StatusEnum.WAITING);

			loadMapStatus.put(request.getMapName(), status);
		}

		// store a map in memory
		CityRoute city;
		if (maps.containsKey(request.getMapName())) {
			city = maps.get(request.getMapName());
		} else {
			city = new CityRoute();
		}

		city.setMaps(request);
		maps.put(request.getMapName(), city);

		// create a entity for persist in database
		RoutesMap map = new RoutesMap();
		map.setId(request.getMapName());
		map.setRoutes(new ArrayList<>());

		request.getDefaultRoutes().forEach((route, distance) -> {
			Route r = new Route();
			r.setPath(route);
			r.setDistance(distance);

			map.getRoutes().add(r);
		});

		// send map to database actor for store.
		Promise.wrap(ask(actor, new Persist<RoutesMap>(map), TIMEOUT));

		// if true offer the map for process best routes
		if (async) {
			Logger.debug("Offer request to Process");
			requestQueue.offer(request);
		}
	}

	/**
	 * Process the best routes of map (async)
	 * 
	 * @throws InterruptedException
	 */
	public void process() throws InterruptedException {
		// take a request for the queue
		Request request = requestQueue.take();
		Logger.debug("Retrieve request to process");
		Logger.debug(String.format("Setting %s map to IN_PROCESS...", request.getMapName()));

		// update a map status
		loadMapStatus.get(request.getMapName()).setStatus(StatusEnum.IN_PROCESS);

		// send request to supervisor to process a best routes of map
		Promise<CityRoute> p = Promise.wrap(ask(actor, new Process<Request>(request), TIMEOUT)).map(response -> {
			CityRoute city = (CityRoute) response;
			return city;
		});

		try {
			// wait a response for supervisor
			CityRoute city = p.get(TIMEOUT);

			if (maps.containsKey(request.getMapName())) {
				maps.remove(request.getMapName());
			}

			// store the best routes in memory
			maps.putIfAbsent(request.getMapName(), city);

			Logger.debug(String.format("Setting %s map to PROCESSED...", request.getMapName()));

			// update a map status to PROCESSED
			loadMapStatus.get(request.getMapName()).setStatus(StatusEnum.PROCESSED);
		} catch (Exception e) {
			e.printStackTrace(System.err);

			// Update a status map to failed.
			Logger.debug(String.format("Setting %s map to FAILED...", request.getMapName()));
			loadMapStatus.get(request.getMapName()).setStatus(StatusEnum.FAILED);
			loadMapStatus.get(request.getMapName()).setMessage(e.getMessage());

			throw new RuntimeException(e);
		}

	}

	/**
	 * Retrieve a best route of the points.
	 * <p>
	 * If the map was previously processed, retrieves the best way of memory, but performs
	 * processing.
	 * 
	 * @param mapName
	 *            Name of map
	 * @param origin
	 *            Origin point
	 * @param destiny
	 *            Destiny point
	 * @return Throws an exception if the best way n is found
	 * @throws InvalidPathException
	 *             Send a exception if the map not found.
	 */
	public Path getBestRoute(String mapName, String origin, String destiny) throws InvalidPathException {
		Logger.debug(String.format("Retrieve %s Best Route for %s ; %s", mapName, origin, destiny));
		// get status of map
		StatusMap status = loadMapStatus.get(mapName);

		switch (status.getStatus()) {
		case PROCESSED:
			if (maps.containsKey(mapName)) {
				// get a best map of the memory
				Path best = maps.get(mapName).getBestPath(origin, destiny);

				// Check if best path is found
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
			// Process a single path
			return this.getSinglePath(mapName, origin, destiny);
		}

	}

	/**
	 * Check the status map
	 * 
	 * @param mapName
	 *            Name of map
	 * @return Return the status of map
	 * @throws InvalidPathException
	 */
	public StatusMap checkProtocol(String mapName) throws InvalidPathException {
		if (loadMapStatus.containsKey(mapName)) {
			return loadMapStatus.get(mapName);
		} else {
			throw new InvalidProtocolException(Messages.get(ERROR_PROTOCOL_NOT_FOUND));
		}
	}

	/**
	 * Loads persisted maps in the database
	 */
	@SuppressWarnings("unchecked")
	public void loadMaps() {
		Logger.debug("Starting thread to load Maps from DataBase...");
		// Retrieve the best routes from database
		Promise<Map<String, CityRoute>> p = Promise.wrap(ask(actor, new Load<RoutesMap>(), TIMEOUT)).map(response -> {
			Map<String, CityRoute> maps = (Map<String, CityRoute>) response;
			return maps;
		});

		// put best routes in map
		maps.putAll(p.get(RoutesManager.TIMEOUT));

		maps.keySet().forEach(key -> {
			StatusMap status = new StatusMap();
			status.setStatus(StatusEnum.PROCESSED);
			status.setName(key);

			// update status map
			loadMapStatus.putIfAbsent(key, status);
		});

		Logger.debug("Maps Loaded...");

	}

}
