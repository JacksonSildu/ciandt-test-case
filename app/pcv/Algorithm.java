package pcv;

import models.Path;
import models.RouteMap;

/**
 * Algorithm Interface
 * 
 * @author Sildu
 *
 */
public interface Algorithm {
	RouteMap getRoute();

	Path processPath(String destinyPoint);
}
