import org.junit.*;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;
import se.romram.client.RelaxClient;
import se.romram.handler.DefaultFileHandler;
import se.romram.handler.RelaxHandler;
import se.romram.handler.TestHandler;
import se.romram.server.RelaxRequest;
import se.romram.server.RelaxResponse;
import se.romram.server.RelaxServer;

import java.io.IOException;
import java.util.concurrent.Executors;

/**
 * Created by micke on 2014-11-28.
 */
public class RelaxServerTest extends AbstractTest {
	private static String[] DEFAULT_HEADERS = {"UserAgent:Relax"};
    private static int port = 2356;
    private static String domain = "localhost";
    private static String baseUrl = getBaseUrl(port);

    private static String getBaseUrl(int port) {
        return "http://" + domain + ":" + port;
    }

	@BeforeClass
	public static void setup() throws IOException {
		System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "ERROR");
        new RelaxServer(port, new TestHandler()).start();
	}

	@Test
	public void testGetOneLiner() {
		assertThat(new RelaxClient().get(baseUrl).toString(), containsString("It worked"));
	}

	@Test
	public void testGetOneLinerWithExceptions() {
		try {
			new RelaxClient().throwExceptions().get(baseUrl + "/404?key=value1&key=value2");
			fail("You should not get here since an exception is expected.");
		} catch (Exception e) {
		}
	}

	@Test
	public void testGet() {
		RelaxClient relaxClient = new RelaxClient();
		relaxClient.get(baseUrl + "/500");
		if (relaxClient.getStatus().isOK()) {
			log.debug("Response [{}]: {}", relaxClient.getStatus().getCode(), relaxClient);
			fail("Should not be found!");
		}
	}

    @Test
    public void testGetServerStats() {
        RelaxClient relaxClient = new RelaxClient();
        relaxClient.get(baseUrl + "/serverstats");
        if (relaxClient.getStatus().isOK()) {
            log.debug(relaxClient.toString());
            assertThat(relaxClient.toString(), containsString("os"));
        }
    }

    @Test
    public void testPost() throws InterruptedException {
        RelaxClient relaxClient = new RelaxClient();
        relaxClient.setPayload("This is the payload!").post(baseUrl + "/echo");
        assertTrue(relaxClient.getStatus().isOK());
        assertThat(relaxClient.toString(), containsString("This is the payload!"));
    }

    @Test
    public void testRequestHeaderGet() {
        RelaxClient relaxClient = new RelaxClient();
        relaxClient.addRequestHeaders("Accept: text/plain").post(baseUrl + "/echo");
        assertTrue(relaxClient.getStatus().isOK());
        assertThat(relaxClient.toString(), containsString("Accept: text/plain"));
    }

    @Test
    public void testOptions() {
        RelaxClient relaxClient = new RelaxClient();
        relaxClient.options(baseUrl);

        assertTrue(relaxClient.getStatus().isOK());
        assertThat(relaxClient.getResponseHeaderFieldsAsFormattedString(), containsString("Allow: HEAD,GET,POST,PUT"));
    }

    @Test
    public void testCookie() throws IOException {
        RelaxServer server = new RelaxServer(2367, new TestHandler()).addHeaders(
                "Set-Cookie: aCookie=aValue"
                , "Set-Cookie: expiredCookie=expired; Expires=Wed, 13 Jan 2001 22:23:01 GMT"
                , "Set-Cookie: newCookie=cookieValue"
        );
        server.start();
        RelaxClient relaxClient = new RelaxClient().useDefaultCookieManager();
        relaxClient.get(getBaseUrl(2367));
        System.out.println(relaxClient.getResponseHeaderFieldsAsFormattedString());
        assertThat(relaxClient.getResponseHeaderFieldsAsFormattedString(), containsString("aCookie=aValue"));
        assertThat(relaxClient.getResponseHeaderFieldsAsFormattedString(), containsString("expiredCookie=expired"));
        relaxClient.get(getBaseUrl(2367));
        String cookieRequestHeader = relaxClient.getCookieManager().getCookieRequestHeaderBuffer(relaxClient.getUrl()).toString();
        assertThat(cookieRequestHeader, containsString("aCookie=aValue"));
        assertThat(cookieRequestHeader, not(containsString("expiredCookie=expired")));
		server.end();
    }

    @Test
	public void testRelaxCreateServer() throws IOException, InterruptedException {
		RelaxServer server = new RelaxServer(2360, new RelaxHandler() {
            @Override
            public boolean handle(RelaxRequest request, RelaxResponse response) {
                /* Simple echo handler */
                response.respond(200, request.getRequestAsString());
                return true;
            }
        });
		server
                .addHeaders("Server: MyRelaxingServer")
                .setContentType("text/html")
				.start();

		RelaxClient relaxClient = new RelaxClient();
		relaxClient.addRequestHeaders(DEFAULT_HEADERS)
				.throwExceptions()
				.get("http://localhost:2360");
		log.debug("Response [{}]: {}", relaxClient.getStatus().getCode(), relaxClient);
		relaxClient.addRequestHeaders("Content-Type:application/x-www-form-urlencoded")
				.setPayload("Post content")
				.post("http://localhost:2360/temp.html");
		log.debug("Response [{}]: {}", relaxClient.getStatus().getCode(), relaxClient);

//		Thread.sleep(10000);
		server.end();
		Thread.sleep(500);
	}

    @Test
    public void testRelaxCreateServerWithNotFoundMessage() throws IOException, InterruptedException {
        RelaxServer server = new RelaxServer(2361, new RelaxHandler() {
            @Override
            public boolean handle(RelaxRequest request, RelaxResponse response) {
                /* Simple not found handler */
                response.respond(404, "");
                return true;
            }
        });
        server
                .addHeaders("Server: MyRelaxingServer")
                .setContentType("text/html")
                .start();

        RelaxClient relaxClient = new RelaxClient();
        relaxClient.addRequestHeaders(DEFAULT_HEADERS)
//                .throwExceptions()
                .get("http://localhost:2361");
        log.debug("Response [{}]: {}", relaxClient.getStatus().getCode(), relaxClient);
        relaxClient.addRequestHeaders("Content-Type:application/x-www-form-urlencoded")
                .setPayload("Post content")
                .post("http://localhost:2361/temp.html");
        log.debug("Response [{}]: {}", relaxClient.getStatus().getCode(), relaxClient);

//		Thread.sleep(10000);
        server.end();
        Thread.sleep(500);
    }

    @Test
	@Ignore
	public void startServer() throws IOException, InterruptedException {
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
		new RelaxServer(2357, new RelaxHandler() {
            @Override
            public boolean handle(RelaxRequest request, RelaxResponse response) {
//                response.addHeaders("Set-Cookie: session=" + request.getQueryMap().get("thread").get(0));
                log.info("A {} request for resource {} has been received."
                        , request.getMethod()
                        , request.getRequestURL()
                );
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                response.addHeaders("thread: " + Thread.currentThread().getId());
                response.respond(200, "Hello \n" + request.getRelaxServer().getStats());
                return true;
            }
        }, Executors.newFixedThreadPool(30)).start();
		new RelaxServer(2358, new DefaultFileHandler("src/test/resources")).start();
		Thread.sleep(1000000);
	}
}

