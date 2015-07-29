package actors;

import static akka.pattern.Patterns.ask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.StringUtils;

import actors.helpers.Process;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import exception.WorkerInterruptException;
import models.Path;
import models.ProcessPath;
import models.RouteMap;
import pcv.Algorithm;
import pcv.ForceBrute;
import play.Logger;
import play.i18n.Messages;
import play.libs.F.Promise;

public class WorkerPointActor extends UntypedActor {

	private static final int	TIMEOUT				= 30 * 1000;
	private static final String	ERROR_WORKER_FAILED	= "error.worker.failed";

	public static Props props = Props.create(WorkerPointActor.class);

	public WorkerPointActor() {
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof RouteMap) {
			RouteMap map = (RouteMap) message;
			Algorithm force = new ForceBrute(map);

			if (StringUtils.isNotBlank(map.getDestiny())) {
				Path bestPath = this.getSinglePath(force, map);
				sender().tell(bestPath, self());
			} else {
				Map<String, Path> best = this.processRoute(force, map);
				sender().tell(best, self());
			}

		}
	}

	public Map<String, Path> processRoute(Algorithm algorithm, RouteMap route) {
		ConcurrentMap<String, Path> bestPath = new ConcurrentHashMap<>();
		CountDownLatch count = new CountDownLatch(route.getPoints().size() - 1); // remove the origin route

		for (String destiny : route.getPoints()) {

			if (route.getOrigin().equals(destiny)) {
				continue;
			}

			ProcessPath request = new ProcessPath(destiny, algorithm);

			ActorRef workerPathActor = context().actorOf(WorkerPathActor.props);
			Promise.wrap(ask(workerPathActor, new Process<ProcessPath>(request), TIMEOUT)).map(response -> {
				Path best = (Path) response;
				bestPath.putIfAbsent(route.getOrigin() + destiny, best);

				count.countDown();
				context().stop(workerPathActor);

				return response;
			}).onFailure(t -> {
				count.countDown();
				throw t;
			});;

		}

		try {
			Logger.debug("Waiting for WorkerPaths....");
			count.await();
		} catch (InterruptedException e) {
			throw new WorkerInterruptException(Messages.get(ERROR_WORKER_FAILED), e);
		}

		return bestPath;
	}

	public Path getSinglePath(Algorithm algorithm, RouteMap route) {
		ActorRef workerPathActor = context().actorOf(WorkerPathActor.props);
		ProcessPath request = new ProcessPath(route.getDestiny(), algorithm);
		Promise<Path> p = Promise.wrap(ask(workerPathActor, new Process<ProcessPath>(request), TIMEOUT)).map(response -> {
			Path best = (Path) response;
			context().stop(workerPathActor);

			return best;
		});

		return p.get(TIMEOUT);
	}

}
