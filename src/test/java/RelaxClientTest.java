import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.impl.SimpleLogger;
import se.romram.client.RelaxClient;
import se.romram.enums.HttpStatus;
import se.romram.handler.DefaultFileHandler;
import se.romram.handler.RelaxTestHandler;
import se.romram.server.RelaxServer;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

/**
 * Created by micke on 2015-02-08.
 */
public class RelaxClientTest extends AbstractTest {
	private long sumTotal = 0;
	private int iterations = 0;

    @BeforeClass
    public static void beforeClass() throws IOException {
		AbstractTest.beforeClass();
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
        new RelaxServer(port, new RelaxTestHandler()).start();
		new RelaxServer(1360, new DefaultFileHandler("src/test/resources")).start();
	}

    @Test
    public void testTimeout() {
        RelaxClient relaxClient = new RelaxClient()
                .setTimeout(500)
                .get(url + "/test?status=200&delay=500");
        if (relaxClient.getStatus().getCode() != 408) {
            fail("Request Timeout was not issued.");
        }
    }

    @Test
    public void testHTTPStatusCodes() {
        RelaxClient relaxClient = new RelaxClient();
        for (HttpStatus httpStatus : HttpStatus.values()) {
            log.debug("Verifying status code {}.", httpStatus.getCode());
            relaxClient.get(url + "/test?status=" + httpStatus.getCode());
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
    public void testStatisticsWikipedia() {
        RelaxClient relaxClient = new RelaxClient();
        relaxClient.get("http://en.wikipedia.org/wiki/List_of_HTTP_status_codes");
        log.debug("Statistics: latency={}, sendtime={}, receivetime={}, total={}"
                , relaxClient.getLatency()
                , relaxClient.getSendTime()
                , relaxClient.getReceiveTime()
                , relaxClient.getTotal()
        );
    }

	@Test
	public void testOK() {
		RelaxClient relaxClient = new RelaxClient();
		for (int i=0; i<20; i++) {
			relaxClient.get(buildUrl("/test?status=200"));
			logStats(relaxClient);
			log.info("{} {}", relaxClient.getStatus().toString(), relaxClient.toString());
		}
	}

	@Test
	public void testCreated() {
		RelaxClient relaxClient = new RelaxClient();
		for (int i=0; i<20; i++) {
			relaxClient.get(buildUrl("/test?status=201"));
			logStats(relaxClient);
			log.info("{} {}", relaxClient.getStatus().toString(), relaxClient.toString());
		}
	}

	private void logStats(RelaxClient relaxClient) {
		iterations++;
		sumTotal = sumTotal + relaxClient.getTotal()*1000;
		log.info("Request stats: latency={}, sendtime={}, receivetime={}, total={}, avg={}"
				, relaxClient.getLatency()
				, relaxClient.getSendTime()
				, relaxClient.getReceiveTime()
				, relaxClient.getTotal()
				, (sumTotal + 500)/iterations/1000
		);
	}

	@Test
	public void testGetGif() throws IOException {
		RelaxClient relaxClient = new RelaxClient().get("http://localhost:1360/java.gif");
		System.out.println(relaxClient.getBytes().length);
		byte[] binaryData = Files.readAllBytes(FileSystems.getDefault().getPath("src/test/resources/java.gif"));
		assertEquals(binaryData.length, relaxClient.getBytes().length);
		assertArrayEquals(binaryData, relaxClient.getBytes());
	}

	@Test
	public void testGetIco() throws IOException {
		RelaxClient relaxClient = new RelaxClient().get("http://localhost:1360/favicon.ico");
		byte[] binaryData = Files.readAllBytes(FileSystems.getDefault().getPath("src/main/resources/se/romram/handler/romram.ico"));
		assertEquals(binaryData.length, relaxClient.getBytes().length);
		assertArrayEquals(binaryData, relaxClient.getBytes());
	}

	@Test
	public void testGetByFile() throws IOException {
		Path path = FileSystems.getDefault().getPath("src/test/resources/java.gif");
		RelaxClient relaxClient = new RelaxClient().get(path.toUri().toString());
		byte[] binaryData = Files.readAllBytes(path);
		assertEquals(binaryData.length, relaxClient.getBytes().length);
		assertArrayEquals(binaryData, relaxClient.getBytes());
	}

}
