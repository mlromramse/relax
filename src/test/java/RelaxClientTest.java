import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.impl.SimpleLogger;
import se.romram.client.RelaxClient;
import se.romram.enums.HttpStatus;
import se.romram.handler.TestHandler;
import se.romram.server.RelaxServer;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Created by micke on 2015-02-08.
 */
public class RelaxClientTest extends AbstractTest {
    private String baseUrl = "http://localhost";
    private static int port = 2357;
    String url = String.format("%s:%s", baseUrl, port);

    @BeforeClass
    public static void beforeClass() throws IOException {
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
        new RelaxServer(port, new TestHandler()).start();
    }

    @Test
    public void testTimeout() {
        RelaxClient relaxClient = new RelaxClient()
                .setTimeout(500)
                .get(url + "?delay=500");
        if (relaxClient.getStatus().getCode() != 408) {
            fail("Request Timeout was not issued.");
        }
    }

    @Test
    public void testHTTPStatusCodes() {
        RelaxClient relaxClient = new RelaxClient();
        for (HttpStatus httpStatus : HttpStatus.values()) {
            log.debug("Verifying status code {}.", httpStatus.getCode());
            relaxClient.get(url + "/" + httpStatus.getCode());
            if (relaxClient.getStatus().getCode() != httpStatus.getCode()) {
                assertEquals(httpStatus.getCode(), relaxClient.getStatus().getCode());
            }
        }
    }

    @Test
    public void testStatistics() {
        RelaxClient relaxClient = new RelaxClient();
        relaxClient.get(url);
        log.debug("Statistics: latency={}, sendtime={}, receivetime={}, total={}"
                , relaxClient.getLatency()
                , relaxClient.getSendTime()
                , relaxClient.getReceiveTime()
                , relaxClient.getTotal()
        );
    }

    @Test
    public void testStatisticsWikipedie() {
        RelaxClient relaxClient = new RelaxClient();
        relaxClient.get("http://en.wikipedia.org/wiki/List_of_HTTP_status_codes");
        log.debug("Statistics: latency={}, sendtime={}, receivetime={}, total={}"
                , relaxClient.getLatency()
                , relaxClient.getSendTime()
                , relaxClient.getReceiveTime()
                , relaxClient.getTotal()
        );
    }

}
