package models;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 
 * @author Sildu
 *
 */
public class CityRoute {
	private static final String			SEMICOLON	= ";";
	private ConcurrentMap<String, Path>	bestPath	= new ConcurrentHashMap<>();
	private Request						maps;

	public Map<String, Path> getBestPath() {
		return bestPath;
	}

	public void addBestPath(Map<String, Path> bestPath) {
		bestPath.forEach((s, p) -> {
			this.bestPath.putIfAbsent(s, p);
		});
	}

	public Path getBestPath(String origin, String destiny) {
		if (this.bestPath.containsKey(origin + SEMICOLON + destiny)) {
			return this.bestPath.get(origin + SEMICOLON + destiny);
		} else {
			return null;
		}
	}

	public Request getMaps() {
		return maps;
	}

	public void setMaps(Request maps) {
		this.maps = maps;
	}

}
