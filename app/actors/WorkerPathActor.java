package actors;

import actors.helpers.Process;
import akka.actor.Props;
import akka.actor.UntypedActor;
import models.Path;
import models.ProcessPath;
import play.Logger;

/**
 * Actor to process a paths
 * 
 * @author Sildu
 *
 */
public class WorkerPathActor extends UntypedActor {

	public static Props props = Props.create(WorkerPathActor.class);

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof Process) {
			Long startTime = System.currentTimeMillis();

			Process<?> process = (Process<?>) message;
			ProcessPath processPath = (ProcessPath) process.getModel();

			// Process a path
			Path bestPath = processPath.getAlgorithm().processPath(processPath.getDestiny());

			Logger.debug(String.format("Elapsed time to find Best Route of %s to %s: %s ms", processPath.getAlgorithm().getRoute().getOrigin(), processPath.getDestiny(), (System.currentTimeMillis() - startTime)));

			// Send path to worker
			sender().tell(bestPath, self());
		}
	}

}
