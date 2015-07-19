package models;

import java.util.HashMap;
import java.util.Map;

public class CityRoute {
	private Map<String, Path> bestPath = new HashMap<>();

	public Map<String, Path> getBestPath() {
		return bestPath;
	}

	public void addBestPath(Map<String, Path> bestPath) {
		this.bestPath.putAll(bestPath);
	}

	public Path getBestPath(String origin, String destiny) {
		return this.bestPath.get(origin + destiny);
	}

}
