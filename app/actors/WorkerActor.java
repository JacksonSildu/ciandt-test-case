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

/**
 * Actor class for process
 * 
 * @author Sildu
 *
 */
public class WorkerActor extends UntypedActor {

	private static final String	SEMICOLON			= ";";
	private static final int	TIMEOUT				= 30 * 1000;
	private static final String	ERROR_WORKER_FAILED	= "error.worker.failed";

	public static Props props = Props.create(WorkerActor.class);

	public WorkerActor() {
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof RouteMap) {
			RouteMap map = (RouteMap) message;

			// create a algorithm to process
			Algorithm force = new ForceBrute(map);

			// if the destiny is defined, process only path
			if (StringUtils.isNotBlank(map.getDestiny())) {
				Path bestPath = this.getSinglePath(force, map);

				// send result to Supervisor
				sender().tell(bestPath, self());
			} else {
				// else, process entire map
				Map<String, Path> best = this.processRoute(force, map);

				// send result to the Supervisor
				sender().tell(best, self());
			}

		}
	}

	/**
	 * Process the map routes
	 * 
	 * @param algorithm
	 *            Algorithm use to process
	 * @param route
	 *            Route to be mapped
	 * @return
	 */
	public Map<String, Path> processRoute(Algorithm algorithm, RouteMap route) {
		ConcurrentMap<String, Path> bestPath = new ConcurrentHashMap<>();

		// create a latch to wait a WokerPath, remove the origin route
		CountDownLatch count = new CountDownLatch(route.getPoints().size() - 1);

		for (String destiny : route.getPoints()) {

			if (route.getOrigin().equals(destiny)) {
				continue;
			}

			ProcessPath request = new ProcessPath(destiny, algorithm);

			// create a worker path to process
			ActorRef workerPathActor = context().actorOf(WorkerPathActor.props);

			// send request to worker path
			Promise.wrap(ask(workerPathActor, new Process<ProcessPath>(request), TIMEOUT)).map(response -> {
				Path best = (Path) response;
				bestPath.putIfAbsent(route.getOrigin() + SEMICOLON + destiny, best);

				count.countDown();
				context().stop(workerPathActor);

				return response;
			}).onFailure(t -> {
				count.countDown();
				throw t;
			});
			;

		}

		try {
			Logger.debug("Waiting for WorkerPaths....");
			count.await();
		} catch (InterruptedException e) {
			throw new WorkerInterruptException(Messages.get(ERROR_WORKER_FAILED), e);
		}

		return bestPath;
	}

	/**
	 * Process a single path
	 * 
	 * @param algorithm
	 *            Algorithm use to process
	 * @param route
	 *            Routes
	 * @return Return a Best Path
	 */
	public Path getSinglePath(Algorithm algorithm, RouteMap route) {
		ActorRef workerPathActor = context().actorOf(WorkerPathActor.props);
		ProcessPath request = new ProcessPath(route.getDestiny(), algorithm);

		// send request to worker path
		Promise<Path> p = Promise.wrap(ask(workerPathActor, new Process<ProcessPath>(request), TIMEOUT)).map(response -> {
			Path best = (Path) response;
			context().stop(workerPathActor);

			return best;
		});

		return p.get(TIMEOUT);
	}

}
