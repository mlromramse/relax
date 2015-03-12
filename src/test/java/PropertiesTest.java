
import org.junit.Test;
import se.romram.main.Properties;

import static org.junit.Assert.assertEquals;

/**
 * Created by micke on 2015-03-12.
 */
public class PropertiesTest extends AbstractTest {

	@Test
	public void testDefaultArgs() {
		String[] args = {"port=1234", "path=.", "threads=20", "execute=doit.json"};
		Properties properties = new Properties(args);
		assertEquals(1234, properties.port);
		assertEquals(".", properties.path);
		assertEquals(20, properties.threads);
		assertEquals("doit.json", properties.execute);
	}

	@Test
	public void testArbitraryArgs() {
		String[] args = {"arg1=something", "arg2=otherthing", "arg3=123", "arg4"};
		Properties properties = new Properties(args);
		assertEquals("something", properties.argMap.get("arg1"));
		assertEquals("otherthing", properties.argMap.get("arg2"));
		assertEquals("123", properties.argMap.get("arg3"));
		assertEquals("true", properties.argMap.get("arg4"));
		assertEquals(null, properties.argMap.get("arg5"));
	}
}
