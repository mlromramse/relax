package se.romram.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.romram.client.RelaxClient;
import se.romram.helpers.SimpleJson;
import se.romram.server.RelaxServer;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.NoSuchElementException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by micke on 2015-01-26.
 */
public class Main {
	private static Logger log = LoggerFactory.getLogger(Main.class);
	private static int activeThreadsCount;
    private static int threadsCount;

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
        Path path = FileSystems.getDefault().getPath(execute);

        try {
            String jsonAsString = new String(Files.readAllBytes(path));
			final SimpleJson json = new SimpleJson(jsonAsString);
			long virtualUsers = json.getLong("virtualUsers", 1);
            threadsCount = (int) virtualUsers;
			long delayPerUser = virtualUsers>1
                    ? (json.getLong("rampUp", 0) * 1000) / (virtualUsers-1)
                    : 0;
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					try {
						incrementActiveThreadsCount();
                        log.info("Starting virtual user thread {}.", Thread.currentThread().getName());
						executeJson(json);
						decrementActiveThreadsCount();
                        log.info("Ending virtual user thread {}.", Thread.currentThread().getName());
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
			};
			System.out.println("timeStamp,elapsed,label,responseCode,responseMessage,success,grpThreads,allThreads,Latency");
			Executor executor = Executors.newFixedThreadPool((int) virtualUsers);
			for (int i = 0; i<virtualUsers; i++) {
				executor.execute(runnable);
				if (i<virtualUsers-1) sleep(delayPerUser);
			}
            while (threadsCount > 0) {
                sleep(1);
            }
            System.exit(0);
		} catch (ParseException e) {
			log.error("The json is not parseable.");
		} catch (IOException e) {
            log.error("File {} not found.", path.toAbsolutePath());
        }

    }

    private static synchronized void decrementActiveThreadsCount() {
		activeThreadsCount--;
        threadsCount--;
	}

	private static synchronized void incrementActiveThreadsCount() {
		activeThreadsCount++;
	}

	private static void executeJson(SimpleJson json) throws ParseException {
		long loops = json.getLong("loop", 1);
		loops = loops==-1 ? Long.MAX_VALUE : loops;
        SimpleJson tasks = json.get("tasks", null);
		for (int loop=0; loop<loops; loop++) {
			int taskLength = tasks.length();
			for (int i = 0; i < taskLength; i++) {
				SimpleJson taskJson = tasks.get(i);
				if (taskJson.getBoolean("active", true)) {
					executeTaskLoopJson(taskJson);
				}
			}
			sleep(json);
		}
	}

	private static void executeTaskLoopJson(SimpleJson taskJson) throws ParseException {
		long loops = taskJson.getLong("loop", 1);
		for (long loop=0; loop<loops; loop++) {
			executeTaskJson(taskJson);
		}
	}

	private static void executeTaskJson(SimpleJson taskJson) throws ParseException {
		String url = taskJson.get("url").toObject().toString();
		RelaxClient relaxClient = new RelaxClient();
		long timestamp = System.currentTimeMillis();
		relaxClient.get(url);
		boolean validateResult = taskJson.get("validate", null) == null || validateRelaxClient(relaxClient, taskJson.get("validate"));
		System.out.println(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s"
				, timestamp
				, relaxClient.getTotal()
				, taskJson.getString("name", null)
				, relaxClient.getStatus().getCode()
				, relaxClient.getStatus().getDescription()
				, validateResult
				, activeThreadsCount
				, activeThreadsCount
				, relaxClient.getTotal()
		));
		sleep(taskJson);
	}

	private static boolean validateRelaxClient(RelaxClient relaxClient, SimpleJson validateJson) throws ParseException {
		boolean containsAll = validateRelaxClientContains(relaxClient, validateJson);
		boolean statusAny = validateRelaxClientStatus(relaxClient, validateJson);
		return containsAll & statusAny;
	}

	private static boolean validateRelaxClientStatus(RelaxClient relaxClient, SimpleJson validateJson) throws ParseException {
		SimpleJson statuses = validateJson.get("status", null);
        if (statuses != null) {
            int statusesLength = statuses.length();
            for (int i = 0; i < statusesLength; i++) {
                Long couldBe = statuses.getLong(i);
                if (relaxClient.getStatus().getCode() == couldBe) {
                    return true;
                }
            }
            return false;
        }
        return true;
	}

	private static boolean validateRelaxClientContains(RelaxClient relaxClient, SimpleJson validateJson) throws ParseException {
		String data = relaxClient.toString();
		SimpleJson contains = validateJson.get("contains", null);
        if (contains != null) {
            int containsLength = contains.length();
            for (int i = 0; i < containsLength; i++) {
				if (contains.getString(i).charAt(0) == '!') {
					Pattern pattern = Pattern.compile(contains.getString(i).substring(1));
					Matcher matcher = pattern.matcher(data);
					if (matcher.find()) {
						return false;
					}
				} else {
					Pattern pattern = Pattern.compile(contains.getString(i));
					Matcher matcher = pattern.matcher(data);
					if (!matcher.find()) {
						return false;
					}
				}
            }
        }
		return true;
	}

    private static void sleep(long sleepMillis) throws ParseException {
        try {
            if (sleepMillis > 0) Thread.sleep(sleepMillis);
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

}
