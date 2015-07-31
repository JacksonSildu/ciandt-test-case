package models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author Sildu
 *
 */
public class Request {
	private String				mapName;
	private Map<String, Double>	defaultRoutes	= new HashMap<>();
	private Set<String>			points			= new HashSet<>();

	public String getMapName() {
		return mapName;
	}

	public void setMapName(String mapName) {
		this.mapName = mapName;
	}

	public Map<String, Double> getDefaultRoutes() {
		return defaultRoutes;
	}

	public void setDefaultRoutes(Map<String, Double> defaultRoutes) {
		this.defaultRoutes = defaultRoutes;
	}

	public Set<String> getPoints() {
		return points;
	}

	public void setPoints(Set<String> points) {
		this.points = points;
	}

}
