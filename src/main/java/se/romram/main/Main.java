package se.romram.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.romram.client.RelaxClient;
import se.romram.helpers.SimpleJson;
import se.romram.server.RelaxServer;

import java.io.IOException;
import java.text.ParseException;
import java.util.NoSuchElementException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by micke on 2015-01-26.
 */
public class Main {
	private static Logger log = LoggerFactory.getLogger(Main.class);
	private static int activeThreadsCount;

    public static final void main(String[] args) throws IOException {
        Properties props = new Properties(args);

        RelaxServer server = new RelaxServer(props.port, props.handler);
		if (props.threads > 10) {
			server.setExecutor(Executors.newFixedThreadPool(props.threads));
		}
        server.start();

		if (props.execute != null) {
			executeJsonFile(props.execute);
		}
    }

	private static void executeJsonFile(String execute) {
		log.info("Current dir: {}", System.getProperty("user.dir"));
		String filename = execute;
		RelaxClient relaxClient = new RelaxClient().get(filename);
		if (relaxClient.getStatus().getCode() == 404) {
			log.error("The resource {} was not found!", filename);
			return;
		}
		String jsonAsString = relaxClient.toString();
		try {
			final SimpleJson json = new SimpleJson(jsonAsString);
			long virtualUsers = json.getLong("virtualUsers");
			long delayPerUser = (json.getLong("rampUp") * 1000) / virtualUsers;
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					try {
						incrementActiveThreadsCount();
						executeJson(json);
						decrementActiveThreadsCount();
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
			};
			System.out.println("timeStamp,elapsed,label,responseCode,responseMessage,success,grpThreads,allThreads,Latency");
			Executor executor = Executors.newFixedThreadPool((int) virtualUsers);
			for (int i = 0; i<virtualUsers; i++) {
				executor.execute(runnable);
				sleep(delayPerUser);
			}
		} catch (ParseException e) {
			log.error("The json is not parseable.");
		}

	}

	private static synchronized void decrementActiveThreadsCount() {
		activeThreadsCount--;
	}

	private static synchronized void incrementActiveThreadsCount() {
		activeThreadsCount++;
	}

	private static void executeJson(SimpleJson json) throws ParseException {
		long loops = ((Long) json.get("loop").toObject());
		for (int loop=0; loop<loops; loop++) {
			int tasks = json.get("execute").length();
			for (int i = 0; i < tasks; i++) {
				SimpleJson taskJson = json.get("execute").get(i);
				if (taskJson.getBoolean("active", true) == true) {
					executeTaskLoopJson(taskJson);
				}
			}
			log.info("Loop done!");
			sleep(json);
		}
		log.info("Done!");
	}

	private static void sleep(long sleepMillis) throws ParseException {
		try {
			Thread.sleep(sleepMillis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static void sleep(SimpleJson json) throws ParseException {
		try {
			Long sleepMillis = (Long) json.get("delay").toObject();
			sleep(sleepMillis);
		} catch (NoSuchElementException e) {
			// Do not sleep!
		}
	}

	private static void executeTaskLoopJson(SimpleJson taskJson) throws ParseException {
		long loops = taskJson.getLong("loop");
		for (long loop=0; loop<loops; loop++) {
			executeTaskJson(taskJson);
		}
	}

	private static void executeTaskJson(SimpleJson taskJson) throws ParseException {
		String url = taskJson.get("url").toObject().toString();
		log.info("Getting '{}'", url);
		RelaxClient relaxClient = new RelaxClient();
		long timestamp = System.currentTimeMillis();
		relaxClient.get(url);
		boolean validateResult = validateRelaxClient(relaxClient, taskJson.get("validate"));
		System.out.println(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s"
				, timestamp
				, relaxClient.getTotal()
				, "name"
				, relaxClient.getStatus().getCode()
				, relaxClient.getStatus().getDescription()
				, relaxClient.getStatus().isOK()
				, activeThreadsCount
				, activeThreadsCount
				, relaxClient.getTotal()
		));
//		log.info("Validateresult was {}. Stats: timestamp={} latency={} sendtime={} waittime={} receivetime={} total={}"
//				, validateResult
//				, timestamp
//				, relaxClient.getLatency()
//				, relaxClient.getSendTime()
//				, relaxClient.getWaitTime()
//				, relaxClient.getReceiveTime()
//				, relaxClient.getTotal()
//		);
		sleep(taskJson);
	}

	private static boolean validateRelaxClient(RelaxClient relaxClient, SimpleJson validateJson) throws ParseException {
		boolean containsAll = validateRelaxClientContains(relaxClient, validateJson);
		boolean statusAny = validateRelaxClientStatus(relaxClient, validateJson);
		return containsAll & statusAny;
	}

	private static boolean validateRelaxClientStatus(RelaxClient relaxClient, SimpleJson validateJson) throws ParseException {
		SimpleJson statuses = validateJson.get("status");
		int statusesLength = statuses.length();
		for (int i=0; i<statusesLength; i++) {
			Long couldBe = statuses.getLong(i);
			if (relaxClient.getStatus().getCode() == couldBe) {
				return true;
			}
		}
		return false;
	}

	private static boolean validateRelaxClientContains(RelaxClient relaxClient, SimpleJson validateJson) throws ParseException {
		SimpleJson contains = validateJson.get("contains");
		int containsLength = contains.length();
		for (int i=0; i<containsLength; i++) {
			String shouldContain = contains.getString(i);
			if (!relaxClient.toString().contains(shouldContain)) {
				return false;
			}
		}
		return true;
	}

}
