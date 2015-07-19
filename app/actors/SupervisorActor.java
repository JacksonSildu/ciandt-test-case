package actors;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.restart;
import static akka.pattern.Patterns.ask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import entity.BestPath;
import entity.CityMap;
import exception.WorkerInterruptException;
import models.CityRoute;
import models.Null;
import models.Path;
import models.Request;
import models.RouteMap;
import play.libs.F.Promise;
import scala.concurrent.duration.Duration;

public class SupervisorActor extends UntypedActor {

	public static Props props = Props.create(SupervisorActor.class);

	private ActorRef	actor;
	private ActorRef	dataBaseActor;

	private static SupervisorStrategy strategy = new OneForOneStrategy(5, Duration.create("1 minute"), DeciderBuilder.match(RuntimeException.class, e -> {
		return restart();
	}).matchAny(o -> {
		return escalate();
	}).build());

	@Override
	public SupervisorStrategy supervisorStrategy() {
		return strategy;
	}

	@Override
	public void preStart() throws Exception {
		actor = context().actorOf(WorkerActor.props);
		dataBaseActor = context().actorOf(WorkerDataBaseActor.props);

		super.preStart();
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof Process) {
			Process<?> process = (Process<?>) message;

			Request request = (Request) process.getEntity();
			CityRoute city = processRoutes(request);

			CityMap cityMap = this.getCityMap(request, city);
			CityMap managedEntity = this.getManagedEntity(cityMap);

			if (managedEntity != null) {
				managedEntity.setPaths(cityMap.getPaths());
				cityMap = managedEntity;
			}

			ask(dataBaseActor, new Persist<CityMap>(cityMap), RoutesManager.TIMEOUT);

			sender().tell(city, self());
		} else if (message instanceof Load) {
			Find<CityMap> find = new Find<CityMap>();
			find.setNamedQuery("CityMap.findAll");

			Map<String, CityRoute> loadMaps = getMapsFromDataBase(find);
			sender().tell(loadMaps, self());
		}
	}

	private Map<String, CityRoute> getMapsFromDataBase(Find<CityMap> find) {
		Promise<Map<String, CityRoute>> p = Promise.wrap(ask(dataBaseActor, find, RoutesManager.TIMEOUT)).map(response -> {
			Map<String, CityRoute> loadMaps = new HashMap<>();

			@SuppressWarnings("unchecked")
			List<CityMap> maps = (List<CityMap>) response;

			maps.forEach(map -> {
				CityRoute route = new CityRoute();
				Map<String, Path> paths = new HashMap<>();

				map.getPaths().forEach(bestPath -> {
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

	private CityMap getManagedEntity(CityMap cityMap) {
		Promise<CityMap> p = Promise.wrap(ask(dataBaseActor, new Find<CityMap>(cityMap), RoutesManager.TIMEOUT)).map(response -> {
			if (response instanceof Null) {
				return null;
			}

			CityMap managedEntity = (CityMap) response;
			return managedEntity;
		});

		CityMap managedEntity = p.get(RoutesManager.TIMEOUT);
		return managedEntity;
	}

	private CityMap getCityMap(Request request, CityRoute city) {
		List<BestPath> bestPaths = new ArrayList<>();
		city.getBestPath().values().forEach(path -> {
			BestPath bestPath = new BestPath();
			bestPath.setOrigin(path.getInitialPath());
			bestPath.setDestiny(path.getLastPath());
			bestPath.setDistance(path.getActualDistance());
			bestPath.setPath(path.getStringPath());

			bestPaths.add(bestPath);
		});

		CityMap cityMap = new CityMap();
		cityMap.setId(request.getMapName());
		cityMap.setPaths(bestPaths);
		return cityMap;
	}

	@SuppressWarnings("unchecked")
	private CityRoute processRoutes(Request request) {
		CityRoute city = new CityRoute();
		CountDownLatch count = new CountDownLatch(request.getPoints().size());

		for (String originPoint : request.getPoints()) {
			Promise.wrap(ask(actor, new RouteMap(request.getMapName(), originPoint, request.getDefaultRoutes(), request.getPoints()), RoutesManager.TIMEOUT)).map(response -> {
				Map<String, Path> best = (Map<String, Path>) response;
				city.addBestPath(best);

				count.countDown();
				return response;
			});
		}

		try {
			count.await();
		} catch (InterruptedException e) {
			throw new WorkerInterruptException("Houve um erro nos Workers.", e);
		}

		return city;
	}

}
