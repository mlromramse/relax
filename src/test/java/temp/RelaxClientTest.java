package temp;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;
import se.romram.client.RelaxClient;
import se.romram.handler.TestHandler;
import se.romram.server.RelaxServer;

import java.io.IOException;

/**
 * Created by micke on 2015-02-09.
 */
public class RelaxClientTest {
	Logger log = LoggerFactory.getLogger(RelaxClientTest.class);
	private static int port = 2357;
	private static String protocol = "http://";
	private static String domain = "localhost";
	private static String url = String.format("%s%s:%s", protocol, domain, port);

	@BeforeClass
	public static void beforeClass() throws IOException {
		System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "info");
		System.setProperty(SimpleLogger.SHOW_DATE_TIME_KEY, "true");
		System.setProperty(SimpleLogger.DATE_TIME_FORMAT_KEY, "dd HH:mm:ss.SSS");
		new RelaxServer(port, new TestHandler()).start();
	}

	@Test
	public void testOK() {
		RelaxClient relaxClient = new RelaxClient();
		for (int i=0; i<20; i++) {
			relaxClient.get(buildUrl("/200"));
			logStats(relaxClient);
			log.info("{} {}", relaxClient.getStatus().toString(), relaxClient.toString());
		}
	}

	@Test
	public void testCreated() {
		RelaxClient relaxClient = new RelaxClient();
		for (int i=0; i<20; i++) {
			relaxClient.get(buildUrl("/201"));
			logStats(relaxClient);
			log.info("{} {}", relaxClient.getStatus().toString(), relaxClient.toString());
		}
	}

	private void logStats(RelaxClient relaxClient) {
		log.info("Request stats: latency={}, sendtime={}, receivetime={}, total={}"
				, relaxClient.getLatency()
				, relaxClient.getSendTime()
				, relaxClient.getReceiveTime()
				, relaxClient.getTotal()
		);
	}


	private String buildUrl(String path) {
		return buildUrl(path, "");
	}

	private String buildUrl(String path, String query) {
		if (!query.isEmpty()) {
			if (query.charAt(0) != '/') {
				query = "/" + query;
			}
		}
		return String.format("%s%s:%s%s", protocol, domain, port, path, query);
	}

}
