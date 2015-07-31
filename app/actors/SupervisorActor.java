package actors;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.restart;
import static akka.pattern.Patterns.ask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

import actors.helpers.Find;
import actors.helpers.Load;
import actors.helpers.Persist;
import actors.helpers.Process;
import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.japi.pf.DeciderBuilder;
import entity.BestRoute;
import entity.RoutesMap;
import exception.WorkerInterruptException;
import models.CityRoute;
import models.Null;
import models.Path;
import models.Request;
import models.RequestSiglePath;
import models.RouteMap;
import play.Logger;
import play.i18n.Messages;
import play.libs.F.Promise;
import scala.Option;
import scala.concurrent.duration.Duration;

/**
 * Supervisor for manager WorkerActors
 * 
 * @author Sildu
 *
 */
public class SupervisorActor extends UntypedActor {

	private static final String	SEMICOLON			= ";";
	private static final int	TIMEOUT				= 30 * 1000;
	private static final String	ROUTES_MAP_FIND_ALL	= "RoutesMap.findAll";
	private static final String	ERROR_WORKER_FAILED	= "error.worker.failed";

	public static Props props = Props.create(SupervisorActor.class);

	private ActorRef	actor;
	private ActorRef	dataBaseActor;

	/**
	 * Create a strategy for fault tolerance
	 */
	private static SupervisorStrategy strategy = new OneForOneStrategy(5, Duration.create("1 minute"), DeciderBuilder.match(RuntimeException.class, e -> {
		Logger.error("Restart Actors: ", e);
		return restart();
	}).matchAny(o -> {
		Logger.info("Escalate: ", o);
		return escalate();
	}).build());

	@Override
	public SupervisorStrategy supervisorStrategy() {
		return strategy;
	}

	@Override
	public void preStart() throws Exception {
		// create a worker actor
		actor = context().actorOf(WorkerActor.props, "worker-actor");

		// Create a database actor
		dataBaseActor = context().actorOf(WorkerDataBaseActor.props, "database-actor");

		super.preStart();
	}

	@Override
	public void preRestart(Throwable arg0, Option<Object> arg1) throws Exception {
		super.preRestart(arg0, arg1);
	}

	/**
	 * Process the receive message
	 * 
	 * @param message
	 *            Receive message
	 * @throws Exception
	 */
	@Override
	public void onReceive(Object message) throws Exception {
		Logger.debug("Supervisor Receive action...");
		// check if a process message
		if (message instanceof Process) {
			Process<?> process = (Process<?>) message;

			// get a object to process
			if (process.getModel() instanceof RequestSiglePath) {
				// if single path send only a message to WorkerPath
				RequestSiglePath request = (RequestSiglePath) process.getModel();
				Path bestPath = ProcessSinglePath(request);

				// sender besth path to Supervisor
				sender().tell(bestPath, self());

			} else {
				// if a request Map, process a entire map
				Request request = (Request) process.getModel();

				// process all paths to map
				CityRoute city = processRoutes(request);

				RoutesMap cityMap = this.getBestRoutesMap(request, city);
				RoutesMap managedEntity = this.getManagedEntity(cityMap);

				if (managedEntity != null) {
					managedEntity.setBestRoutes(cityMap.getBestRoutes());
					cityMap = managedEntity;
				}

				// Send a best routes to database to persist
				ask(dataBaseActor, new Persist<RoutesMap>(cityMap), TIMEOUT);

				// send the best routes to Supervisor actor
				sender().tell(city, self());
			}

		} else if (message instanceof Load) {
			Find<RoutesMap> find = new Find<RoutesMap>();
			find.setNamedQuery(ROUTES_MAP_FIND_ALL);

			// load maps from database
			Map<String, CityRoute> loadMaps = getMapsFromDataBase(find);
			// Sender maps to Supervisor
			sender().tell(loadMaps, self());
		} else if (message instanceof Persist) {
			Persist<?> process = (Persist<?>) message;
			RoutesMap cityMap = (RoutesMap) process.getModel();

			RoutesMap managedEntity = this.getManagedEntity(cityMap);

			if (managedEntity != null) {
				managedEntity.setRoutes(cityMap.getRoutes());
				managedEntity.setBestRoutes(cityMap.getBestRoutes());
				cityMap = managedEntity;
			}

			// send request to database actor
			ask(dataBaseActor, new Persist<RoutesMap>(cityMap), TIMEOUT);
		} else {

		}
	}

	/**
	 * Retrieve maps from database
	 * 
	 * @param find
	 *            Object to request
	 * @return
	 */
	private Map<String, CityRoute> getMapsFromDataBase(Find<RoutesMap> find) {
		// Send a request to dataBase actor
		Promise<Map<String, CityRoute>> p = Promise.wrap(ask(dataBaseActor, find, TIMEOUT)).map(response -> {
			Map<String, CityRoute> loadMaps = new HashMap<>();

			@SuppressWarnings("unchecked")
			List<RoutesMap> maps = (List<RoutesMap>) response;

			maps.forEach(map -> {
				Map<String, Path> paths = new HashMap<>();

				Map<String, Double> defaultRoute = new HashMap<>();
				Set<String> points = new HashSet<>();
				map.getRoutes().forEach(r -> {
					String[] point = r.getPath().split(SEMICOLON);
					defaultRoute.put(r.getPath(), r.getDistance());
					points.add(point[0]);
					points.add(point[1]);
				});

				Request request = new Request();
				request.setMapName(map.getId());
				request.setDefaultRoutes(defaultRoute);

				map.getBestRoutes().forEach(bestPath -> {
					Path path = new Path(bestPath.getOrigin(), bestPath.getDestiny(), bestPath.getDistance(), bestPath.getPath());
					paths.put(bestPath.getOrigin() + bestPath.getDestiny(), path);
				});

				CityRoute route = new CityRoute();
				route.addBestPath(paths);
				route.setMaps(request);
				loadMaps.put(map.getId(), route);
			});

			return loadMaps;
		});

		// wait the best maps
		Map<String, CityRoute> loadMaps = p.get(RoutesManager.TIMEOUT);
		return loadMaps;
	}

	/**
	 * Get a Managed Entity for a database
	 * 
	 * @param cityMap
	 * @return
	 */
	private RoutesMap getManagedEntity(RoutesMap cityMap) {
		Promise<RoutesMap> p = Promise.wrap(ask(dataBaseActor, new Find<RoutesMap>(cityMap), TIMEOUT)).map(response -> {
			if (response instanceof Null) {
				return null;
			}

			RoutesMap managedEntity = (RoutesMap) response;
			return managedEntity;
		});

		RoutesMap managedEntity = p.get(RoutesManager.TIMEOUT);
		return managedEntity;
	}

	/**
	 * Mapping a best routes
	 * 
	 * @param request
	 *            Request Object
	 * @param city
	 *            City Map
	 * @return
	 */
	private RoutesMap getBestRoutesMap(Request request, CityRoute city) {
		List<BestRoute> bestPaths = new ArrayList<>();
		city.getBestPath().values().forEach(path -> {
			BestRoute bestPath = new BestRoute();
			bestPath.setOrigin(path.getInitialPath());
			bestPath.setDestiny(path.getLastPath());
			bestPath.setDistance(path.getActualDistance());
			bestPath.setPath(path.getStringPath());

			bestPaths.add(bestPath);
		});

		RoutesMap bestRoutes = new RoutesMap();
		bestRoutes.setId(request.getMapName());
		bestRoutes.setBestRoutes(bestPaths);

		return bestRoutes;
	}

	/**
	 * Process all routes of map
	 * 
	 * @param request
	 *            Request Object
	 * @return Return a map of city
	 */
	@SuppressWarnings("unchecked")
	private CityRoute processRoutes(Request request) {
		CityRoute city = new CityRoute();

		// create a latch down for wait all actors
		CountDownLatch count = new CountDownLatch(request.getPoints().size());
		Long startTime = System.currentTimeMillis();

		// process all points of request
		for (String originPoint : request.getPoints()) {
			Promise.wrap(ask(actor, new RouteMap(request.getMapName(), originPoint, request.getDefaultRoutes(), request.getPoints()), RoutesManager.TIMEOUT)).map(response -> {
				ConcurrentMap<String, Path> best = (ConcurrentMap<String, Path>) response;
				city.addBestPath(best);

				count.countDown();
				return response;
			}).onFailure(t -> {
				count.countDown();
				throw t;
			});
		}

		try {
			Logger.debug("Waiting for WorkerPoints....");
			count.await();

			Logger.debug(String.format("Elapsed time: %s ms", (System.currentTimeMillis() - startTime)));
		} catch (InterruptedException e) {
			throw new WorkerInterruptException(Messages.get(ERROR_WORKER_FAILED), e);
		}

		return city;
	}

	/**
	 * Process a single path
	 * 
	 * @param request
	 *            Request Object
	 * @return Return a best path
	 */
	private Path ProcessSinglePath(RequestSiglePath request) {
		// Create a new actor
		ActorRef actor = context().actorOf(WorkerActor.props);

		// send request to actor
		Promise<Path> p = Promise.wrap(ask(actor, new RouteMap(request.getMapName(), request.getOrigin(), request.getDefaultRoutes(), request.getPoints(), request.getDestiny()), RoutesManager.TIMEOUT)).map(response -> {
			return (Path) response;
		});

		// wait actor
		return p.get(TIMEOUT);
	}

}
