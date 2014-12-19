import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.hamcrest.CoreMatchers;
import org.junit.*;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.romram.client.RelaxClient;
import se.romram.server.RelaxServer;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Created by micke on 2014-11-28.
 */
public class RelaxTest {
	private static Logger log = LoggerFactory.getLogger(RelaxTest.class);
	private static String[] DEFAULT_HEADERS = {"UserAgent:Relax"};

	@Rule
	public TestRule testRule = new TestRule() {
		@Override
		public Statement apply(Statement base, Description description) {
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
	public static void setup() {
		LogManager.getRootLogger().setLevel(Level.DEBUG);
	}

	@Test
	public void testApacheGetOneLiner() {
		assertThat(new RelaxClient().get("http://localhost").toString(), containsString("It worked"));
	}

	@Test
	public void testApacheGetOneLinerWithExceptions() {
		try {
			log.debug("Response: %s", new RelaxClient().throwExceptions().get("http://localhost/missing"));
			fail("You should not get here since an exception is expected.");
		} catch (Exception e) {
			log.debug("An expected exception '{}' was thrown with message '{}'.", e.getClass().getSimpleName(), e.getMessage());
		}
	}

	@Test
	public void testApacheGet() {
		RelaxClient relaxClient = new RelaxClient();
		relaxClient.get("http://localhost/abcdef");
		if (relaxClient.getStatus().isOK()) {
			log.debug("Response [{}]: {}", relaxClient.getStatus().getCode(), relaxClient);
			fail("Should not be found!");
		}
	}

	@Test
	public void testRelaxCreateServer() throws IOException, InterruptedException {
		RelaxServer server = new RelaxServer(2357, Paths.get("."));
		server.headers("Server: MyRelaxingServer", "Content-Type: text/html")
				.start();

		RelaxClient relaxClient = new RelaxClient();
		relaxClient.headers(DEFAULT_HEADERS)
				.throwExceptions()
				.get("http://localhost:2357");
		log.debug("Response [{}]: {}", relaxClient.getStatus().getCode(), relaxClient);
		relaxClient.headers("Content-Type:application/x-www-form-urlencoded")
				.body("Post content")
				.post("http://localhost:2357/temp.html");
		log.debug("Response [{}]: {}", relaxClient.getStatus().getCode(), relaxClient);

//		Thread.sleep(10000);
		server.end();
		Thread.sleep(500);
	}

	@Test
	@Ignore
	public void startServer() throws IOException, InterruptedException {
		new RelaxServer(2357, Paths.get("."))
				.registerHandler("*")
				.start();
		new RelaxServer(2358, Paths.get(".")).start();
		Thread.sleep(1000000);
	}
}

