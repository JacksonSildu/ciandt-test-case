package controllers;

import play.Application;
import play.GlobalSettings;
import play.Logger;

public class Global extends GlobalSettings {

	@Override
	public void onStart(Application app) {
		Logger.debug("Application has started");
	}

	@Override
	public void onStop(Application app) {
		Logger.debug("Application has stopped");
	}

}
