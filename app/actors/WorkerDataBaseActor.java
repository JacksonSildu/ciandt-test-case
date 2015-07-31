package actors;

import javax.persistence.Query;

import actors.helpers.Find;
import actors.helpers.Persist;
import akka.actor.Props;
import akka.actor.UntypedActor;
import entity.AbstractEntity;
import models.Null;
import play.Logger;
import play.db.jpa.JPA;

/**
 * Actor to persist ou retrieve data in database
 * 
 * @author Sildu
 *
 */
public class WorkerDataBaseActor extends UntypedActor {

	public static Props props = Props.create(WorkerDataBaseActor.class);

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof Persist) {
			Persist<?> request = (Persist<?>) message;

			AbstractEntity<?> requestEntity = (AbstractEntity<?>) request.getModel();
			Logger.debug(String.format("Persist or Update entity %s in database...", request.getModelClass().getName()));

			JPA.withTransaction(() -> {
				// merge data in database
				JPA.em().merge(requestEntity);
			});
		} else if (message instanceof Find) {
			Find<?> request = (Find<?>) message;
			Object result = null;

			try {
				Logger.debug("Find entity in DataBase...");
				result = JPA.withTransaction(() -> {
					if (request.getNamedQuery() != null) {
						Logger.debug(String.format("Fidding by NamedQuery... NamedQuery: %s", request.getNamedQuery()));
						return findByNamedQuery(request);
					} else if (request.getModel() != null) {
						Logger.debug("Fidding by entity primary key...");
						return findById(request);
					} else {
						return null;
					}
				});

				if (result == null) {
					Logger.debug("No Result found...");
					result = new Null();
				}

				sender().tell(result, self());
			} catch (Throwable e) {
				Logger.error("Error in DataBase Access: ", e);
				throw new RuntimeException(e);
			}

		}
	}

	/**
	 * Retrieve data from named query
	 * 
	 * @param request
	 * @return
	 */
	private Object findByNamedQuery(Find<?> request) {
		Query query = JPA.em().createNamedQuery(request.getNamedQuery());
		return query.getResultList();
	}

	/**
	 * Retrieve data from id
	 * 
	 * @param request
	 * @return
	 */
	private Object findById(Find<?> request) {
		AbstractEntity<?> requestEntity = (AbstractEntity<?>) request.getModel();
		return JPA.em().find(requestEntity.getClass(), requestEntity.getId());
	}

}
