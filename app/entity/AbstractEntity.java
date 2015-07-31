package entity;

/**
 * Abstract entity
 * 
 * @author Sildu
 *
 * @param <T>
 */
public abstract class AbstractEntity<T> {
	protected T id;

	public T getId() {
		return id;
	}

	public void setId(T id) {
		this.id = id;
	}
}
