package models;

public class PathResponse {
	private String	path;
	private Double	distance;
	private Double	cost;
	
	public PathResponse(String path, Double distance, Double cost) {
		this.path = path;
		this.distance = distance;
		this.cost = cost;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Double getDistance() {
		return distance;
	}

	public void setDistance(Double distance) {
		this.distance = distance;
	}

	public Double getCost() {
		return cost;
	}

	public void setCost(Double cost) {
		this.cost = cost;
	}

}
