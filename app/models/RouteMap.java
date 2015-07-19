package models;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class RouteMap implements Serializable {
	private static final long serialVersionUID = 1L;

	private String				mapName;
	private String				originPoint;
	private Map<String, Double>	map;
	private Set<String>			points;

	public RouteMap(String name, String point, Map<String, Double> map, Set<String> points) {
		this.originPoint = point;
		this.map = map;
		this.mapName = name;
		this.points = points;
	}

	public String getInitialPoint() {
		return originPoint;
	}

	public Map<String, Double> getMap() {
		return map;
	}

	public String getName() {
		return mapName;
	}

	public Set<String> getPoints() {
		return points;
	}

}
