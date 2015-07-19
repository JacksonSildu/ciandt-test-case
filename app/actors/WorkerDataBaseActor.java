package actors;

import javax.persistence.Query;

import actors.helpers.Find;
import actors.helpers.Persist;
import akka.actor.Props;
import akka.actor.UntypedActor;
import entity.AbstractEntity;
import models.Null;
import play.db.jpa.JPA;

public class WorkerDataBaseActor extends UntypedActor {

	public static Props props = Props.create(WorkerDataBaseActor.class);

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof Persist) {
			Persist<?> request = (Persist<?>) message;
			AbstractEntity<?> requestEntity = (AbstractEntity<?>) request.getEntity();

			JPA.withTransaction(() -> {
				JPA.em().merge(requestEntity);
			});
		} else if (message instanceof Find) {
			Find<?> request = (Find<?>) message;

			Object result = null;
			try {
				result = JPA.withTransaction(() -> {
					if (request.getNamedQuery() != null) {
						return findByNamedQuery(request);
					} else if (request.getEntity() != null) {
						return findById(request);
					} else {
						return null;
					}
				});

				if (result == null) {
					result = new Null();
				}

				sender().tell(result, self());
			} catch (Throwable e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}

		}
	}

	private Object findByNamedQuery(Find<?> request) {
		Query query = JPA.em().createNamedQuery(request.getNamedQuery());
		return query.getResultList();
	}

	private Object findById(Find<?> request) {
		AbstractEntity<?> requestEntity = (AbstractEntity<?>) request.getEntity();
		return JPA.em().find(requestEntity.getClass(), requestEntity.getId());
	}

}
