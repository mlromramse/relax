import org.junit.Test;
import se.romram.client.RelaxClient;
import se.romram.server.RelaxServer;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Created by micke on 2014-11-28.
 */
public class RelaxTest {
	private static String[] DEFAULT_HEADERS = {"UserAgent:Relax"};

	@Test
	public void testRelaxCreateServer() throws IOException, InterruptedException {
		RelaxServer server = new RelaxServer(2357, Paths.get("."));
		server.headers("Server: MyRelaxingServer", "Content-Type: text/html")
				.start();

		RelaxClient relax = new RelaxClient();
		relax.headers(DEFAULT_HEADERS)
				.get("http://localhost:2357");
		relax.headers("Content-Type:application/x-www-form-urlencoded")
				.body("Post content")
				.post("http://localhost:2357/temp.html");

		Thread.sleep(10000);
		server.end();
		Thread.sleep(5000);
	}
}

