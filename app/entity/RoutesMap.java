package entity;

import java.io.Serializable;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * 
 * @author Sildu
 *
 */
@Entity
@Table(name = "BEST_ROUTES_MAP")
@NamedQueries({ @NamedQuery(name = "RoutesMap.findAll", query = "SELECT m FROM RoutesMap m") })
public class RoutesMap extends AbstractEntity<String>implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "ID")
	private String id;

	@OneToMany(targetEntity = BestRoute.class, fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "MAP_ID", referencedColumnName = "ID")
	private List<BestRoute> bestRoutes;

	@OneToMany(targetEntity = Route.class, fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "MAP_ID", referencedColumnName = "ID")
	private List<Route> routes;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<BestRoute> getBestRoutes() {
		return bestRoutes;
	}

	public void setBestRoutes(List<BestRoute> paths) {
		this.bestRoutes = paths;
	}

	public List<Route> getRoutes() {
		return routes;
	}

	public void setRoutes(List<Route> routes) {
		this.routes = routes;
	}
}
