package actors.helpers;

/**
 * Action for load data
 * 
 * @author Sildu
 *
 * @param <T>
 */
public class Load<T> extends Actions<T> {

	public Load() {
	}

	public Load(T entity) {
		super(entity);
	}

}
