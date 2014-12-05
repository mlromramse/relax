package se.romram.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;

/**
 * Created by micke on 2014-12-02.
 */
public class RelaxServer extends Thread {
	private static Logger log = LoggerFactory.getLogger(RelaxServer.class);
	private boolean active = false;
	private int port;
	protected Path path;
	private ServerSocket serverSocket;
	protected String charsetName = "UTF8";
	private int timeoutMillis = 30000;

	public RelaxServer(int port, Path path) throws IOException {
		this.port = port;
		this.path = path;
		serverSocket = new ServerSocket(port);
	}

	public void run() {
		active = true;
		log.info("The server is active and monitors port {}", port);
		while (active) {
			try {
				log.debug("Waiting for request!");
				final Socket socket = serverSocket.accept();
				log.debug("Socket accept!");
				socket.setSoTimeout(timeoutMillis);
				RelaxHandler handler = new RelaxHandler(socket, this);
				handler.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		log.info("The server is stopped.");
	}

	public void end() {
		active = false;
	}

	public RelaxServer headers(String... headerStrings) {
		// TODO Handle headers
		return this;
	}

}
