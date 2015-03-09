import org.junit.Test;
import se.romram.client.RelaxClient;
import se.romram.enums.HttpMethod;
import se.romram.handler.RegExpRouteHandler;
import se.romram.handler.RelaxHandler;
import se.romram.server.RelaxRequest;
import se.romram.server.RelaxResponse;
import se.romram.server.RelaxServer;

import java.io.IOException;

/**
 * Created by micke on 2015-03-05.
 */
public class RegExpRouteHandlerTest extends AbstractTest {

	@Test
	public void testDefaultHandler() throws IOException {
		RelaxServer relaxServer = new RelaxServer(2357, new RegExpRouteHandler(HttpMethod.GET, "", new RelaxHandler() {
			@Override
			public boolean handle(RelaxRequest relaxRequest, RelaxResponse relaxResponse) {
				relaxResponse.respond(200, "Default response");
				return true;
			}
		}).addRoute(HttpMethod.GET, "^/test/(.*)", new RelaxHandler() {
			@Override
			public boolean handle(RelaxRequest relaxRequest, RelaxResponse relaxResponse) {
				relaxResponse.respond(200, "Routed response to test");
				return true;
			}
		}).addRoute(HttpMethod.GET, "^/image/(.*)", new RelaxHandler() {
			@Override
			public boolean handle(RelaxRequest relaxRequest, RelaxResponse relaxResponse) {
				relaxResponse.respond(200, "Routed response to image");
				return true;
			}
		})
		);
		relaxServer.start();
		RelaxClient relaxClient;
		relaxClient = new RelaxClient().get("http://localhost:2357/");
		log.info(relaxClient.toString());
        relaxClient = new RelaxClient().get("http://localhost:2357/image/image.jpg");
        log.info(relaxClient.toString());
		relaxClient = new RelaxClient().get("http://localhost:2357/test/somefile.txt");
		log.info(relaxClient.toString());
        relaxClient = new RelaxClient().post("http://localhost:2357/test/somefile.txt");
        log.info(relaxClient.toString());
	}

}
