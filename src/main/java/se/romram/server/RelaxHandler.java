package se.romram.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Created by micke on 2014-12-02.
 */
public class RelaxHandler extends Thread {
	private Logger log = LoggerFactory.getLogger(RelaxHandler.class);
	private RelaxServer relaxServer;

	public RelaxHandler(RelaxServer relaxServer) {
		this.relaxServer = relaxServer;
	}

	public void run() {
		log.info("A request has been received and will be handled.");
		try {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(relaxServer.socket.getInputStream()));
			BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(relaxServer.socket.getOutputStream());
			String request = bufferedReader.readLine();
			log.debug(request);
			bufferedOutputStream.write(request.getBytes());
			bufferedOutputStream.flush();
			bufferedOutputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		}
	}

}
