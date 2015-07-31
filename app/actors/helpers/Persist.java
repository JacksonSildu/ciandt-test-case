package actors.helpers;

/**
 * Action to persist data in database
 * 
 * @author Sildu
 *
 * @param <T>
 */
public class Persist<T> extends Actions<T> {

	public Persist(T entity) {
		super(entity);
	}

}
