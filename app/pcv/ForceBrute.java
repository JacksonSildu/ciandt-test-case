package pcv;

import java.util.HashMap;
import java.util.Map;

import models.Path;
import models.RouteMap;

public class ForceBrute implements Algorithm {
	private RouteMap route;

	public ForceBrute(RouteMap route) {
		this.route = route;
	}

	@Override
	public Map<String, Path> processRoute() {
		Map<String, Path> bestPath = new HashMap<>();
		for (String destinyPoint : route.getPoints()) {
			if (route.getInitialPoint().equals(destinyPoint)) {
				continue;
			}

			Path best = processPath(getIntialPath(), destinyPoint, null);
			bestPath.put(route.getInitialPoint() + destinyPoint, best);
		}

		return bestPath;
	}

	private Path processPath(Path actualPath, String destinyPoint, Path bestPath) {
		for (String nextPoint : route.getPoints()) {
			if (actualPath.getPath().contains(nextPoint)) {
				continue;
			} else if (route.getMap().containsKey(actualPath.getLastPath() + nextPoint)) {
				double cost = route.getMap().get(actualPath.getLastPath() + nextPoint);
				actualPath.addDistance(cost);

				if (nextPoint.equals(destinyPoint)) {
					actualPath.addPath(nextPoint);
					bestPath = checkBestPath(actualPath, bestPath);
				} else {
					actualPath.addPath(nextPoint);
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
		initialPath.addPath(route.getInitialPoint());

		return initialPath;
	}

	private Path checkBestPath(Path actualPath, Path bestPath) {
		return bestPath == null || actualPath.getActualDistance() < bestPath.getActualDistance() ? ((Path) actualPath.clone()) : bestPath;
	}

}
