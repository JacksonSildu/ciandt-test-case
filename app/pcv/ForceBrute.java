package pcv;

import models.Path;
import models.RouteMap;
import play.Logger;
import play.libs.Json;

/**
 * Algorithm to process map in brute force.
 * 
 * @author Sildu
 *
 */
public class ForceBrute implements Algorithm {
	private static final String	SEMICOLON	= ";";
	private RouteMap			route;

	public ForceBrute(RouteMap route) {
		this.route = route;
	}

	/**
	 * Check the best paths
	 * 
	 * @param actualPath
	 * @param bestPath
	 * @return
	 */
	private Path checkBestPath(Path actualPath, Path bestPath) {
		return bestPath == null || actualPath.getActualDistance() < bestPath.getActualDistance() ? ((Path) actualPath.clone()) : bestPath;
	}

	/**
	 * Process the map
	 */
	@Override
	public Path processPath(String destinyPoint) {
		// Process the path
		Path path = processPath(this.getIntialPath(), destinyPoint, null);
		Logger.debug(Json.toJson(path).toString());
		return path;
	}

	/**
	 * Process the map
	 * 
	 * @param actualPath
	 *            Actual Point
	 * @param destinyPoint
	 *            Destiny Point
	 * @param bestPath
	 *            Best Path
	 * @return
	 */
	private Path processPath(Path actualPath, String destinyPoint, Path bestPath) {
		for (String nextPoint : route.getPoints()) {
			// check is the actual point contains the next point
			if (actualPath.getPath().contains(nextPoint)) {
				continue;
			} else if (route.getMap().containsKey(actualPath.getLastPath() + SEMICOLON + nextPoint)) {
				double cost = route.getMap().get(actualPath.getLastPath() + SEMICOLON + nextPoint);
				actualPath.addDistance(cost);
				actualPath.addPath(nextPoint);

				// if next poins is destiny point, check the best route
				if (nextPoint.equals(destinyPoint)) {
					bestPath = checkBestPath(actualPath, bestPath);
				} else {
					// check new point
					bestPath = this.processPath(actualPath, destinyPoint, bestPath);
				}

				actualPath.removePath(nextPoint);
				actualPath.removeDistance(cost);
			}
		}

		return bestPath;
	}

	/**
	 * Get the initial point
	 * 
	 * @return
	 */
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
