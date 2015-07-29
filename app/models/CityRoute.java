package models;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CityRoute {
	private ConcurrentMap	<String, Path> bestPath = new ConcurrentHashMap<>();

	public Map<String, Path> getBestPath() {
		return bestPath;
	}

	public void addBestPath(Map<String, Path> bestPath) {
		bestPath.forEach((s, p) ->{
			this.bestPath.putIfAbsent(s, p);
		});
	}

	public Path getBestPath(String origin, String destiny) {
		if (this.bestPath.containsKey(origin + destiny)) {
			return this.bestPath.get(origin + destiny);
		} else {
			return null;
		}
	}

}
