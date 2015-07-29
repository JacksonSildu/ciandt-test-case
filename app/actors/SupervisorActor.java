package actors;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.restart;
import static akka.pattern.Patterns.ask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class SupervisorActor extends UntypedActor {

	private static final int	TIMEOUT				= 30 * 1000;
	private static final String	ROUTES_MAP_FIND_ALL	= "RoutesMap.findAll";
	private static final String	ERROR_WORKER_FAILED	= "error.worker.failed";

	public static Props props = Props.create(SupervisorActor.class);

	private ActorRef	actor;
	private ActorRef	dataBaseActor;

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
		actor = context().actorOf(WorkerPointActor.props, "worker-actor");
		dataBaseActor = context().actorOf(WorkerDataBaseActor.props, "database-actor");

		super.preStart();
	}

	@Override
	public void preRestart(Throwable arg0, Option<Object> arg1) throws Exception {
		super.preRestart(arg0, arg1);
	}

	@Override
	public void onReceive(Object message) throws Exception {
		Logger.debug("Supervisor Receive action...");
		if (message instanceof Process) {
			Process<?> process = (Process<?>) message;

			if (process.getModel() instanceof RequestSiglePath) {
				RequestSiglePath request = (RequestSiglePath) process.getModel();
				Path bestPath = ProcessSinglePath(request);

				sender().tell(bestPath, self());

			} else {
				Request request = (Request) process.getModel();
				CityRoute city = processRoutes(request);

				RoutesMap cityMap = this.getBestRoutesMap(request, city);
				RoutesMap managedEntity = this.getManagedEntity(cityMap);

				if (managedEntity != null) {
					managedEntity.setBestRoutes(cityMap.getBestRoutes());
					cityMap = managedEntity;
				}

				ask(dataBaseActor, new Persist<RoutesMap>(cityMap), TIMEOUT);

				sender().tell(city, self());
			}

		} else if (message instanceof Load) {
			Find<RoutesMap> find = new Find<RoutesMap>();
			find.setNamedQuery(ROUTES_MAP_FIND_ALL);

			Map<String, CityRoute> loadMaps = getMapsFromDataBase(find);
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

			ask(dataBaseActor, new Persist<RoutesMap>(cityMap), TIMEOUT);
		} else {

		}
	}

	private Map<String, CityRoute> getMapsFromDataBase(Find<RoutesMap> find) {
		Promise<Map<String, CityRoute>> p = Promise.wrap(ask(dataBaseActor, find, TIMEOUT)).map(response -> {
			Map<String, CityRoute> loadMaps = new HashMap<>();

			@SuppressWarnings("unchecked")
			List<RoutesMap> maps = (List<RoutesMap>) response;

			maps.forEach(map -> {
				CityRoute route = new CityRoute();
				Map<String, Path> paths = new HashMap<>();

				map.getBestRoutes().forEach(bestPath -> {
					Path path = new Path(bestPath.getOrigin(), bestPath.getDestiny(), bestPath.getDistance(), bestPath.getPath());
					paths.put(bestPath.getOrigin() + bestPath.getDestiny(), path);
				});

				route.addBestPath(paths);
				loadMaps.put(map.getId(), route);
			});

			return loadMaps;
		});

		Map<String, CityRoute> loadMaps = p.get(RoutesManager.TIMEOUT);
		return loadMaps;
	}

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

	@SuppressWarnings("unchecked")
	private CityRoute processRoutes(Request request) {
		CityRoute city = new CityRoute();
		CountDownLatch count = new CountDownLatch(request.getPoints().size());
		Long startTime = System.currentTimeMillis();

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

	private Path ProcessSinglePath(RequestSiglePath request) {
		ActorRef actor = context().actorOf(WorkerPointActor.props);
		Promise<Path> p = Promise.wrap(ask(actor, new RouteMap(request.getMapName(), request.getOrigin(), request.getDefaultRoutes(), request.getPoints(), request.getDestiny()), RoutesManager.TIMEOUT)).map(response -> {
			return (Path) response;
		});

		return p.get(TIMEOUT);
	}

}
