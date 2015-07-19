package models;

import java.util.Iterator;
import java.util.LinkedHashSet;

public class Path implements Cloneable {
	private LinkedHashSet<String>	path	= new LinkedHashSet<>();
	private String					initialPath;
	private String					lastPath;
	private double					actualDistance;

	public Path(LinkedHashSet<String> path, double actualDistance) {
		this.path = path;
		this.actualDistance = actualDistance;
	}

	public Path() {
	}

	public Path(String initialPath, String lastPath, double actualDistance, String fullPath) {
		this.initialPath = initialPath;
		this.lastPath = lastPath;
		this.actualDistance = actualDistance;

		for (char c : fullPath.toCharArray()) {
			path.add(String.valueOf(c));
		}

	}

	public LinkedHashSet<String> getPath() {
		return path;
	}

	public double getActualDistance() {
		return actualDistance;
	}

	public String getLastPath() {
		return lastPath;
	}

	public String getInitialPath() {
		return initialPath;
	}

	public void addDistance(double cost) {
		this.actualDistance += cost;
	}

	public void removeDistance(double cost) {
		this.actualDistance -= cost;
	}

	public void addPath(String actualPath) {

		actualPath = actualPath.toUpperCase();

		if (path.isEmpty()) {
			initialPath = actualPath;
		}

		path.add(actualPath);
		lastPath = actualPath;
	}

	public void removePath(String pathToRemove) {
		pathToRemove = pathToRemove.toUpperCase();
		path.remove(pathToRemove);
		lastPath = (String) path.toArray()[path.size() - 1];
	}

	public String getStringPath() {
		StringBuilder builder = new StringBuilder();
		for (Iterator<String> i = path.iterator(); i.hasNext();) {
			String next = i.next();
			builder.append(next);
		}

		return builder.toString().toUpperCase();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object clone() {
		Path clone = new Path();
		clone.actualDistance = this.actualDistance;
		clone.initialPath = this.initialPath;
		clone.lastPath = this.lastPath;
		clone.path = (LinkedHashSet<String>) this.path.clone();

		return clone;
	}

}