package pcv;

import models.Path;
import models.RouteMap;
import play.Logger;
import play.libs.Json;

public class ForceBrute implements Algorithm {
	private RouteMap route;

	public ForceBrute(RouteMap route) {
		this.route = route;
	}

	private Path checkBestPath(Path actualPath, Path bestPath) {
		return bestPath == null || actualPath.getActualDistance() < bestPath.getActualDistance() ? ((Path) actualPath.clone()) : bestPath;
	}

	@Override
	public Path processPath(String destinyPoint) {
		Path path = processPath(this.getIntialPath(), destinyPoint, null);
		Logger.debug(Json.toJson(path).toString());
		return path;
	}

	private Path processPath(Path actualPath, String destinyPoint, Path bestPath) {
		for (String nextPoint : route.getPoints()) {
			if (actualPath.getPath().contains(nextPoint)) {
				continue;
			} else if (route.getMap().containsKey(actualPath.getLastPath() + nextPoint)) {
				double cost = route.getMap().get(actualPath.getLastPath() + nextPoint);
				actualPath.addDistance(cost);
				actualPath.addPath(nextPoint);

				if (nextPoint.equals(destinyPoint)) {
					bestPath = checkBestPath(actualPath, bestPath);
				} else {
					bestPath = this.processPath(actualPath, destinyPoint, bestPath);
				}

				actualPath.removePath(nextPoint);
				actualPath.removeDistance(cost);
			}
		}

		return bestPath;
	}

	private Path getIntialPath() {
		Path initialPath = new Path();
		initialPath.addPath(route.getOrigin());

		return initialPath;
	}

	@Override
	public RouteMap getRoute() {
		return route;
	}

}
