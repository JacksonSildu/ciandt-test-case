package controllers;

import play.Application;
import play.GlobalSettings;
import play.Logger;

/**
 * Global settings for application
 * 
 * @author Sildu
 *
 */
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
