package actors;

import static akka.pattern.Patterns.ask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.hql.internal.ast.InvalidPathException;

import actors.helpers.Load;
import actors.helpers.Process;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import entity.CityMap;
import enums.StatusEnum;
import exception.InvalidProtocolException;
import models.CityRoute;
import models.Path;
import models.Request;
import models.StatusMap;
import play.libs.F.Promise;

public class RoutesManager {
	private ConcurrentHashMap<String, CityRoute>	loadMaps;
	private ActorRef								actor;
	private ConcurrentHashMap<String, StatusMap>	loadMapStatus;

	public static long TIMEOUT = 60 * 1000;

	public RoutesManager(ActorSystem system) {
		actor = system.actorOf(SupervisorActor.props);
		loadMaps = new ConcurrentHashMap<>();
		loadMapStatus = new ConcurrentHashMap<>();
	}

	public void processCityMap(Request request) {
		StatusMap status = new StatusMap();
		status.setName(request.getMapName());
		status.setStatus(StatusEnum.IN_PROCESS);

		loadMapStatus.put(request.getUuidProtocol(), status);

		Promise.wrap(ask(actor, new Process<Request>(request), TIMEOUT)).map(response -> {

			CityRoute city = (CityRoute) response;

			if (loadMaps.contains(request.getMapName())) {
				loadMaps.get(request.getMapName()).addBestPath(city.getBestPath());
			} else {
				loadMaps.putIfAbsent(request.getMapName(), city);
			}

			status.setStatus(StatusEnum.PROCESSED);
			return response;
		}).onFailure(throwable -> {
			Throwable t = (Throwable) throwable;
			t.printStackTrace(System.err);

			status.setStatus(StatusEnum.FAILED);
			status.setMessage(t.getMessage());
		});
	}

	public Path getBestRoute(String mapName, String origin, String destiny) throws InvalidPathException {
		if (loadMaps.containsKey(mapName)) {
			Path best = loadMaps.get(mapName).getBestPath(origin, destiny);
			return best;
		} else {
			throw new InvalidPathException("O mapa informado nao foi encontrado");
		}
	}

	public StatusMap checkProtocol(String uuidProtocol) throws InvalidPathException {
		if (loadMapStatus.containsKey(uuidProtocol)) {
			return loadMapStatus.get(uuidProtocol);
		} else {
			throw new InvalidProtocolException("O Protocolo informado não foi encontrado");
		}
	}

	public void loadMaps() {
		Promise<Map<String, CityRoute>> p = Promise.wrap(ask(actor, new Load<CityMap>(), TIMEOUT)).map(response -> {
			@SuppressWarnings("unchecked")
			Map<String, CityRoute> maps = (Map<String, CityRoute>) response;
			loadMaps.putAll(maps);
			return maps;
		});

	}

}
