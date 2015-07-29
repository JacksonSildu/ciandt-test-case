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
		map.put("AB", 10D);
		map.put("BA", 10D);
		map.put("BD", 15D);
		map.put("DB", 15D);
		map.put("AC", 20D);
		map.put("CA", 20D);
		map.put("CD", 30D);
		map.put("DC", 30D);
		map.put("BE", 50D);
		map.put("EB", 50D);
		map.put("DE", 30D);
		map.put("ED", 30D);

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
