package actors.helpers;

/**
 * Helper class to mapper actions to the actors
 * 
 * @author Sildu
 *
 * @param <T>
 */
public abstract class Actions<T> {
	public T model;

	public Actions(T model) {
		this.model = model;
	}

	public Actions() {
	}

	public Class<?> getModelClass() {
		return model.getClass();
	}

	public T getModel() {
		return model;
	}

	public void setModel(T entity) {
		this.model = entity;
	}

}
