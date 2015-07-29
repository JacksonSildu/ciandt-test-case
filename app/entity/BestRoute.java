package entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@Table(name = "BEST_PATH")
public class BestRoute extends AbstractEntity<Long>implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@TableGenerator(name = "EVENT_GEN", table = "SEQUENCES", pkColumnName = "SEQ_NAME", valueColumnName = "SEQ_NUMBER", pkColumnValue = "BEST_PATH", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "EVENT_GEN")
	private Long	id;
	@Column(name = "ORIGIN")
	private String	origin;
	@Column(name = "DESTINY")
	private String	destiny;
	@Column(name = "PATH")
	private String	path;
	@Column(name = "DISTANCE")
	private Double	distance;
	@Column(name = "MAP_ID")
	private String	mapId;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		super.setId(id);
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public String getDestiny() {
		return destiny;
	}

	public void setDestiny(String destiny) {
		this.destiny = destiny;
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

}
