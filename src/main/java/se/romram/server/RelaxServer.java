package se.romram.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.romram.handler.RelaxFaviconHandler;
import se.romram.handler.RelaxHandler;
import se.romram.handler.RelaxStatsHandler;
import se.romram.helpers.SimpleJson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by micke on 2014-12-02.
 */
public class RelaxServer extends Thread {
    private static final String UNIX_GET_PROCESS_DATA_ONELINER = "top -bp%1$s -n1|grep %1$s|tr -s \" \"|sed \"s/^ *//\"|cut -d \" \" -f 1- --output-delimiter \",\"";
    private String[] processDataNames = {"", "", "", "", "", "residentMem", "sharedMem", "", "cpu%", "mem%", ""};
    private Logger log = LoggerFactory.getLogger(RelaxServer.class);
	private volatile boolean active = false;
	private int port;
	protected List<RelaxHandler> relaxHandlerList = Collections.synchronizedList(new ArrayList<RelaxHandler>());
	private ServerSocket serverSocket;
	public String charsetName = "UTF8";
	private int timeoutMillis = 30000;
    private Executor executor = Executors.newFixedThreadPool(10);
    private int activeThreadsCount = 0;
    private long requestCount = 0;
    private List<String> headerList = Collections.synchronizedList(new ArrayList<String>());
    private String contentType = "text/plain; charset=utf8";
    private long pid = -1;
	private SimpleJson processJson;
	private long lastProcessJsonTimeStamp = 0;
	private RelaxStatsHandler relaxStatsHandler = new RelaxStatsHandler();
	private RelaxFaviconHandler relaxFaviconHandler = new RelaxFaviconHandler();

	public RelaxServer(int port) throws IOException {
		this(port, 50);
	}
    public RelaxServer(int port, int queue) throws IOException {
        this.port = port;
        serverSocket = new ServerSocket(port, queue);
    }

	public RelaxServer(int port, RelaxHandler handler) throws IOException {
		this(port);
		addRelaxHandler(handler);
	}
	public RelaxServer(int port, int queue, RelaxHandler handler) throws IOException {
		this(port, queue);
        addRelaxHandler(handler);
	}

    public RelaxServer(int port, RelaxHandler handler, Executor executor) throws IOException {
        this(port, handler);
        this.executor = executor;
    }

	public RelaxServer(int port, int queue, RelaxHandler handler, Executor executor) throws IOException {
		this(port, queue, handler);
		this.executor = executor;
	}

	public RelaxServer setExecutor(Executor executor) {
        this.executor = executor;
        return this;
    }

    public RelaxServer addHeaders(String... headerArr) {
        for (String header : headerArr) {
            getHeaderList().add(header);
        }
        return this;
    }

    public List<String> getHeaderList() {
        return headerList;
    }

    public RelaxServer addRelaxHandler(RelaxHandler handler) {
        if (handler != null) {
            relaxHandlerList.add(handler);
        }
        return this;
    }

	public int getPort() {
		return port;
	}

    public String getContentType() {
        return contentType;
    }

    public RelaxServer setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public void run() {
		addRelaxHandler(relaxFaviconHandler);
		addRelaxHandler(relaxStatsHandler);
		active = true;
		try {
			log.error("The server is active with master handler {} and monitors port {} with a buffer size of {}."
					, relaxHandlerList.get(0).getClass().getSimpleName()
					, serverSocket.getLocalPort()
					, serverSocket.getReceiveBufferSize()
			);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		for (RelaxHandler handler : relaxHandlerList) {
			log.debug(" * using handler {}.", handler.getClass().getSimpleName());
		}
		final RelaxServer server = this;
		while (active) {
			try {
				final Socket socket = serverSocket.accept();
				socket.setSoTimeout(timeoutMillis);

                Runnable request = new Runnable() {
                    @Override
                    public void run() {
                        incrementActiveThreadsCount();
                        RelaxRequest relaxRequest = new RelaxRequest(socket, server);
                        RelaxResponse relaxResponse = new RelaxResponse(relaxRequest, server);
						relaxRequest.getRequestBuffer();
						long start = System.currentTimeMillis();
                        boolean handled = false;
						for (RelaxHandler handler : relaxHandlerList) {
							if (handled = handler.handle(relaxRequest, relaxResponse)) {
								log.info("{} {}{} (handled by:{} in {} ms, from:{}"
										, relaxRequest.getMethod()
										, relaxRequest.getRequestURL()
										, relaxRequest.getQueryString().isEmpty() ? "" : "?"+relaxRequest.getQueryString()
										, handler.getClass().getSimpleName().isEmpty() ? "<<inline handler>>" : handler.getClass().getSimpleName()
										, System.currentTimeMillis() - start
										, relaxRequest.getUserAgent()
								);
								break;
							}
						}
						if (!handled) {
							log.warn("{} {}{} (Not handled by any handler. From: {})"
									, relaxRequest.getMethod()
									, relaxRequest.getRequestURL()
									, relaxRequest.getQueryString()
									, relaxRequest.getUserAgent()
							);
							relaxResponse.respond(404, "Not Found!");
						}
						try {
							socket.close();
						} catch (IOException e) {
							e.printStackTrace();
						} finally {
							if (socket != null) {
								try {
									socket.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
						decrementActiveThreadsCount();
                    }
                };
                executor.execute(request);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		log.info("The server is stopped.");
	}

	private synchronized void decrementActiveThreadsCount() {
        activeThreadsCount--;
    }

    private synchronized void incrementActiveThreadsCount() {
        activeThreadsCount++;
        requestCount++;
    }

    public synchronized long getProcessId() {
        if (pid == -1) {
            try {
                java.lang.management.RuntimeMXBean runtime =
                        java.lang.management.ManagementFactory.getRuntimeMXBean();
                java.lang.reflect.Field jvm = null;
                jvm = runtime.getClass().getDeclaredField("jvm");
                jvm.setAccessible(true);
                sun.management.VMManagement mgmt =
                        (sun.management.VMManagement) jvm.get(runtime);
                java.lang.reflect.Method pid_method =
                        mgmt.getClass().getDeclaredMethod("getProcessId");
                pid_method.setAccessible(true);
                pid = (Integer) pid_method.invoke(mgmt);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (Exception e) {
            	e.printStackTrace();
			}
        }
        return pid;
    }

    public String getStats() {
		try {
			SimpleJson json = new SimpleJson("{}");
			json.put("server", this.getClass().getSimpleName());
			json.put("pid", getProcessId());
			json.put("port", port);
			json.put("activeThreads", getActiveThreadsCount());
			json.put("requestCount", getRequestCount());
			json.put("os", getOsStatsJson());
			json.put("java", getJavaStatsJson());
			json.put("process", getUnixStatsJson());
			return json.toString(2);
		} catch (ParseException e) {
			log.error("Failed to parse json");
			return "";
		}
    }

	private SimpleJson getOsStatsJson() throws ParseException {
		SimpleJson osJson = new SimpleJson("{}");
		osJson.put("name", System.getProperty("os.name"));
		osJson.put("arch", System.getProperty("os.arch"));
		osJson.put("version", System.getProperty("os.version"));
		try {
			osJson.put("sysload", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
			osJson.put("cores", ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors());
		} catch (Exception e) {
			// For stability reasons
		}
		return osJson;
	}

	private SimpleJson getJavaStatsJson() throws ParseException {
		SimpleJson javaJson = new SimpleJson("{}");
        javaJson.put("name", System.getProperty("java.vm.name"));
		javaJson.put("vendor", System.getProperty("java.vendor"));
		javaJson.put("version", System.getProperty("java.version"));
		return javaJson;
    }

	private SimpleJson getUnixStatsJson() throws ParseException {
		if (System.getProperty("os.name").toLowerCase().contains("nux")) {
			if (lastProcessJsonTimeStamp + 2000 < System.currentTimeMillis()) {
				setLastProcessJsonTimeStamp();
				processJson = getUnixStats();
			}
		} else {
			processJson = new SimpleJson("{}");
			processJson.put("comment", "No stats available for this os.");
		}
		return processJson;
	}

	private synchronized void setLastProcessJsonTimeStamp() {
		lastProcessJsonTimeStamp = System.currentTimeMillis();
	}

	private SimpleJson getUnixStats() throws ParseException {
		try {
			String command = String.format(UNIX_GET_PROCESS_DATA_ONELINER, getProcessId());
			String[] script = {"/bin/sh", "-c", command};
			log.debug("Executing: {}", script);
			long peekTime = System.currentTimeMillis();
			Process process = Runtime.getRuntime().exec(script);
			process.waitFor();
			peekTime = System.currentTimeMillis()-peekTime;

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String resultLine = "";
			String line = "";
			while ((line = reader.readLine()) != null) {
				resultLine += line;
			}

			SimpleJson json = new SimpleJson("{}");
			json.put("peekTime", peekTime);
			String[] resultArr = resultLine.split(",");
			for (int i=0; i<resultArr.length; i++) {
				if (i<processDataNames.length && !processDataNames[i].isEmpty()) {
					String value = resultArr[i];
					int multiple = value.contains("m") ? 1000000 : processDataNames[i].contains("%") ? 1 : 1000;
					multiple = value.contains("g") ? 1000000000 : multiple;
					int divisor = value.contains(".") ? 10 : 1;
					int intValue = Integer.parseInt(value.replace("m", "").replace("g", "").replace(".", "")) * multiple / divisor;
					json.put(processDataNames[i], intValue);
				}
			}
			return json;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new SimpleJson("{}");
	}

    public synchronized int getActiveThreadsCount() {
        return activeThreadsCount;
    }

    public synchronized long getRequestCount() {
        return requestCount;
    }

    public void end() {
		active = false;
	}


}
