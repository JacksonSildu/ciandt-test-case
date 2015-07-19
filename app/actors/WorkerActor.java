package actors;

import java.util.Map;

import akka.actor.Props;
import akka.actor.UntypedActor;
import models.Path;
import models.RouteMap;
import pcv.Algorithm;
import pcv.ForceBrute;

public class WorkerActor extends UntypedActor {

	public static Props props = Props.create(WorkerActor.class);

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof RouteMap) {
			RouteMap map = (RouteMap) message;
			Algorithm force = new ForceBrute(map);
			Map<String, Path> best = force.processRoute();

			sender().tell(best, self());
		}
	}

}
