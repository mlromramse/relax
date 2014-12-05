import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import se.romram.client.RelaxClient;
import se.romram.server.RelaxServer;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Created by micke on 2014-11-28.
 */
public class RelaxTest {
	private static String[] DEFAULT_HEADERS = {"UserAgent:Relax"};

	@Rule
	public TestRule testRule = new TestRule() {
		@Override
		public Statement apply(Statement base, Description description) {
			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
					System.out.println(description.getDisplayName());
					base.evaluate();
				}
			};
		}
	};

	@Test
	public void testApacheGetOneliner() {
		System.out.printf("Response: %s", new RelaxClient().get("http://localhost"));
	}

	@Test
	public void testApacheGet() {
		RelaxClient relaxClient = new RelaxClient();
		relaxClient.get("http://localhost/abcdef");
		if (relaxClient.getStatus().isOK()) {
			System.out.printf("Response [%s]: %s", relaxClient.getStatus().getCode(), relaxClient);
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
		System.out.printf("Response [%s]: %s", relaxClient.getStatus().getCode(), relaxClient);
		relaxClient.headers("Content-Type:application/x-www-form-urlencoded")
				.body("Post content")
				.post("http://localhost:2357/temp.html");
		System.out.printf("Response [%s]: %s", relaxClient.getStatus().getCode(), relaxClient);

//		Thread.sleep(10000);
		server.end();
		Thread.sleep(5000);
	}

	@Test
	@Ignore
	public void startServer() throws IOException, InterruptedException {
		new RelaxServer(2357, Paths.get(".")).run();
//		Thread.sleep(1000000);
	}
}

