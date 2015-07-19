package actors.helpers;

public abstract class Actions<T> {
	public T entity;

	public Actions(T entity) {
		this.entity = entity;
	}

	public Actions() {
	}

	public Class<?> getEntityClass() {
		return entity.getClass();
	}

	public T getEntity() {
		return entity;
	}

	public void setEntity(T entity) {
		this.entity = entity;
	}

}
