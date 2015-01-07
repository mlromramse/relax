package se.romram.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.romram.handler.RelaxHandler;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by micke on 2014-12-02.
 */
public class RelaxServer extends Thread {
	private static Logger log = LoggerFactory.getLogger(RelaxServer.class);
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

	public RelaxServer(int port, RelaxHandler handler) throws IOException {
		this.port = port;
        addRelaxHandler(handler);
		serverSocket = new ServerSocket(port);
	}

    public RelaxServer(int port, RelaxHandler handler, Executor executor) throws IOException {
        this(port, handler);
        this.executor = executor;
    }

	public void run() {
		active = true;
		log.info("The server is active and monitors port {}", port);
		while (active) {
			try {
//				log.debug("Waiting for request!");
				final Socket socket = serverSocket.accept();
//				log.debug("Socket accept!");
				socket.setSoTimeout(timeoutMillis);
                RelaxServer server = this;

                Runnable request = new Runnable() {
                    @Override
                    public void run() {
                        incrementActiveThreadsCount();
                        RelaxRequest relaxRequest = new RelaxRequest(socket, server);
                        RelaxResponse relaxResponse = new RelaxResponse(relaxRequest, server);
                        if (!new RelaxServerHandler().handle(relaxRequest, relaxResponse)) {
                            for (RelaxHandler handler : relaxHandlerList) {
                                if (handler.handle(relaxRequest, relaxResponse)) {
                                    break;
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
        int pid = 0;
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
        return pid;
    }

    public synchronized String getStats() {
        StringBuffer buf = new StringBuffer("{\n");
        buf.append(serverValue("server", this.getClass().getSimpleName()));
        buf.append(addServerValue("pid", getProcessId()));
        buf.append(addServerValue("activeThreads", getActiveThreadsCount()));
        buf.append(addServerValue("requestCount", getRequestCount()));
        buf.append("\n}");
        return buf.toString();
    }

    private String addServerValue(String key, Object value) {
        return String.format(",\n%s", serverValue(key, value));
    }

    private String serverValue(String key, Object value) {
        return String.format("\"%s\": \"%s\"", key, value.toString());
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

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }


}
