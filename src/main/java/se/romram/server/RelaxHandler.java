package se.romram.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	}

}
