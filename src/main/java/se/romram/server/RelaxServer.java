package se.romram.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.romram.handler.RelaxHandler;
import se.romram.helpers.SimpleJson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
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
	private static final String SERVER_STATS = "scripts/cpu.sh %s";
    private String[] processDataNames = {"", "", "", "", "", "residentMem", "sharedMem", "", "cpu%", "mem%", ""};
    private Logger log = LoggerFactory.getLogger(RelaxServer.class);
	private boolean active = false;
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

	public RelaxServer(int port, RelaxHandler handler) throws IOException {
		this.port = port;
        addRelaxHandler(handler);
		serverSocket = new ServerSocket(port);
	}

    public RelaxServer(int port, RelaxHandler handler, Executor executor) throws IOException {
        this(port, handler);
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

    public String getContentType() {
        return contentType;
    }

    public RelaxServer setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public void run() {
		active = true;
		log.info("The server is active and monitors port {}", port);
		log.debug(" * using handler {}.", relaxHandlerList.get(0).getClass().getSimpleName());
		while (active) {
			try {
//				log.debug("Waiting for request!");
				final Socket socket = serverSocket.accept();
//				log.debug("Socket accept!" + socket.getRemoteSocketAddress());
				socket.setSoTimeout(timeoutMillis);
                final   RelaxServer server = this;

                Runnable request = new Runnable() {
                    @Override
                    public void run() {
                        incrementActiveThreadsCount();
                        RelaxRequest relaxRequest = new RelaxRequest(socket, server);
                        RelaxResponse relaxResponse = new RelaxResponse(relaxRequest, server);
                        boolean handled = new RelaxServerHandler().handle(relaxRequest, relaxResponse);
                        if (!handled) {
                            for (RelaxHandler handler : relaxHandlerList) {
                                if (handled = handler.handle(relaxRequest, relaxResponse)) {
                                    log.info("{} {}{} (handled by:{}, from:{}"
                                            , relaxRequest.getMethod()
                                            , relaxRequest.getRequestURL()
                                            , relaxRequest.getQueryString()
                                            , handler.getClass().getSimpleName().isEmpty() ? "<<inline handler>>" : handler.getClass().getSimpleName()
                                            ,relaxRequest.getUserAgent()
                                    );
                                    break;
                                }
                            }
                            if (!handled) {
                                log.info("{} {}{} (Not handled by any handler. From: {})"
                                        , relaxRequest.getMethod()
                                        , relaxRequest.getRequestURL()
                                        , relaxRequest.getQueryString()
                                        , relaxRequest.getUserAgent()
                                );
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
            }
        }
        return pid;
    }

    public String getStats() {
        StringBuffer buf = new StringBuffer("{\n");
        buf.append(serverValue("server", this.getClass().getSimpleName()));
        buf.append(addServerValue("pid", getProcessId()));
		buf.append(addServerValue("port", port));
        buf.append(addServerValue("activeThreads", getActiveThreadsCount()));
        buf.append(addServerValue("requestCount", getRequestCount()));
        osStats(buf);
        javaStats(buf);
        buf.append("\n}");
        return buf.toString();
    }

    private void javaStats(StringBuffer buf) {
        String javaName = System.getProperty("java.vm.name");
        String javaVendor = System.getProperty("java.vendor");
        String javaVersion = System.getProperty("java.version");
        buf.append(",\n\"java\": {\n");
        buf.append(serverValue("name", javaName));
        buf.append(addServerValue("arch", javaVendor));
        buf.append(addServerValue("version", javaVersion));
        buf.append("\n}\n");
    }

    private void osStats(StringBuffer buf) {
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        String osVersion = System.getProperty("os.version");
        buf.append(",\n\"os\": {\n");
        buf.append(serverValue("name", osName));
        buf.append(addServerValue("arch", osArch));
        buf.append(addServerValue("version", osVersion));
        buf.append("\n}\n");
		try {
			Number sysload = (Double) ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
			Number cors = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
			buf.append(addServerValue("sysload", sysload));
			buf.append(addServerValue("cors", cors));
		} catch (Exception e) {
			// For stability reasons
		}
		unixStats(buf, osName);
	}

	private void unixStats(StringBuffer buf, String osName) {
		if (osName.toLowerCase().contains("nux")) {
			try {
				String command = String.format(SERVER_STATS, getProcessId());
				String[] script = {"/bin/sh", "-c", command};
				log.debug("Executing: {}", script);
				long peekTime = System.currentTimeMillis();
				Process process = new ProcessBuilder(getClass().getResource("preprocrun.sh").getFile(), ""+getProcessId()).start();
				process.waitFor();
				process = new ProcessBuilder(this.getClass().getResource("cpu.sh").getFile(), ""+getProcessId()).start();
				peekTime = System.currentTimeMillis()-peekTime;
				buf.append(addServerValue("peekTime", peekTime));
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String resultLine = "";
				String line = "";
				while ((line = reader.readLine()) != null) {
					resultLine += line;
				}
				SimpleJson json = new SimpleJson(resultLine);
				buf.append(",\n\"process\": ");
				buf.append(json.toString(2));

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private String addServerValue(String key, Object value) {
        return String.format(",\n%s", serverValue(key, value));
    }

    private String serverValue(String key, Object value) {
		if (value instanceof Integer) {
			return String.format("\"%s\": %s", key, value != null ? value : "null");
		}
        return String.format("\"%s\": \"%s\"", key, value != null ? value.toString() : "null");
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
