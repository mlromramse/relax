import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;
import se.romram.handler.TestHandler;
import se.romram.server.RelaxServer;

import java.io.IOException;

/**
 * Created by micke on 2015-02-08.
 */
public class AbstractTest {
    protected Logger log = LoggerFactory.getLogger(RelaxServerTest.class);
	protected static int port = 1361;
	protected static String protocol = "http://";
	protected static String domain = "localhost";
	protected static String url = String.format("%s%s:%s", protocol, domain, port);

    @Rule
    public TestRule testRule = new TestRule() {
        @Override
        public Statement apply(final Statement base, final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    log.debug("======================================================================");
                    log.debug(description.getDisplayName());
                    base.evaluate();
                }
            };
        }
    };

	@BeforeClass
	public static void beforeClass() throws IOException {
		System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "info");
		System.setProperty(SimpleLogger.SHOW_DATE_TIME_KEY, "true");
		System.setProperty(SimpleLogger.DATE_TIME_FORMAT_KEY, "dd HH:mm:ss.SSS");
	}

	protected String buildUrl(String path) {
		return buildUrl(path, "");
	}

	protected String buildUrl(String path, String query) {
		if (!query.isEmpty()) {
			if (query.charAt(0) != '/') {
				query = "/" + query;
			}
		}
		return String.format("%s%s:%s%s", protocol, domain, port, path, query);
	}


}
