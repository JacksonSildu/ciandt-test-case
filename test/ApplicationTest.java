import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import models.Path;
import models.RouteMap;
import pcv.Algorithm;
import pcv.ForceBrute;

/**
 *
 * @author Sildu
 *
 */
public class ApplicationTest {

	@Test
	public void simpleCheck() {
		Map<String, Double> map = new HashMap<>();
		map.put("A;B", 10D);
		map.put("B;A", 10D);
		map.put("B;D", 15D);
		map.put("D;B", 15D);
		map.put("A;C", 20D);
		map.put("C;A", 20D);
		map.put("C;D", 30D);
		map.put("D;C", 30D);
		map.put("B;E", 50D);
		map.put("E;B", 50D);
		map.put("D;E", 30D);
		map.put("E;D", 30D);

		Set<String> points = new HashSet<>();
		points.add("A");
		points.add("B");
		points.add("C");
		points.add("D");
		points.add("E");

		RouteMap routeMap = new RouteMap("BH", "A", map, points);
		Algorithm force = new ForceBrute(routeMap);

		Path bestPath = force.processPath("D");

		assertThat(bestPath.getActualDistance(), equalTo(25.0));
	}
}
