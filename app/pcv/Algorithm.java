package pcv;

import models.Path;
import models.RouteMap;

public interface Algorithm {
	RouteMap getRoute();

	Path processPath(String destinyPoint);
}
