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

@Entity
@Table(name = "MAP")
@NamedQueries({ @NamedQuery(name = "CityMap.findAll", query = "SELECT m FROM CityMap m ") })
public class CityMap extends AbstractEntity<String>implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "ID")
	private String id;

	@OneToMany(targetEntity = BestPath.class, fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "MAP_ID", referencedColumnName = "ID")
	private List<BestPath> paths;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<BestPath> getPaths() {
		return paths;
	}

	public void setPaths(List<BestPath> paths) {
		this.paths = paths;
	}
}
