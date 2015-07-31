package models;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * Routes Pojo
 * 
 * @author Sildu
 *
 */
public class RouteMap implements Serializable {
	private static final long serialVersionUID = 1L;

	private String				mapName;
	private String				origin;
	private String				destiny;
	private Set<String>			points;
	private Map<String, Double>	map;

	public RouteMap(String name, String origin, Map<String, Double> map, Set<String> points) {
		this.origin = origin;
		this.map = map;
		this.mapName = name;
		this.points = points;
	}

	public RouteMap(String name, String origin, Map<String, Double> map, Set<String> points, String destiny) {
		this(name, origin, map, points);
		this.destiny = destiny;
	}

	public String getOrigin() {
		return origin;
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

	public String getDestiny() {
		return destiny;
	}

	public void setDestiny(String destiny) {
		this.destiny = destiny;
	}

}
