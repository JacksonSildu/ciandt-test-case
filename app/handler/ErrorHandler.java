package handler;

import play.*;
import play.api.OptionalSourceMapper;
import play.api.UsefulException;
import play.api.routing.Router;
import play.http.DefaultHttpErrorHandler;
import play.libs.F.*;
import play.libs.Json;
import play.mvc.Http.*;
import play.mvc.*;

import javax.inject.*;

import models.ErrorMessage;

/**
 * Class to handle exceptions
 * 
 * @author Sildu
 *
 */
public class ErrorHandler extends DefaultHttpErrorHandler {

	@Inject
	public ErrorHandler(Configuration configuration, Environment environment, OptionalSourceMapper sourceMapper, Provider<Router> routes) {
		super(configuration, environment, sourceMapper, routes);
	}

	protected Promise<Result> onProdServerError(RequestHeader request, UsefulException exception) {
		ErrorMessage error = new ErrorMessage(Results.internalServerError().status(), "A server error occurred: " + exception.getMessage());
		String json = Json.toJson(error).toString();

		return Promise.<Result> pure(Results.internalServerError(json));
	}

	protected Promise<Result> onForbidden(RequestHeader request, String message) {
		ErrorMessage error = new ErrorMessage(Results.forbidden().status(), "You're not allowed to access this resource.");
		String json = Json.toJson(error).toString();

		return Promise.<Result> pure(Results.forbidden(json));
	}

	@Override
	protected Promise<Result> onBadRequest(RequestHeader request, String message) {
		ErrorMessage error = new ErrorMessage(Results.badRequest().status(), "Ocurred an error: " + message);
		String json = Json.toJson(error).toString();

		return Promise.<Result> pure(Results.badRequest(json));
	}

	@Override
	protected Promise<Result> onNotFound(RequestHeader request, String message) {
		ErrorMessage error = new ErrorMessage(Results.notFound().status(), "Action not found");
		String json = Json.toJson(error).toString();

		return Promise.<Result> pure(Results.notFound(json));
	}

}