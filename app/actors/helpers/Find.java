package actors.helpers;

/**
 * Action to find data in database
 * 
 * @author Sildu
 *
 * @param <T>
 */
public class Find<T> extends Actions<T> {
	private String namedQuery;

	public Find() {
	}

	public Find(T entity) {
		super(entity);
	}

	public void setNamedQuery(String namedQuery) {
		this.namedQuery = namedQuery;
	}

	public String getNamedQuery() {
		return namedQuery;
	}

}
