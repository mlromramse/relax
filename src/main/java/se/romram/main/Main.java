package se.romram.main;

import se.romram.handler.DefaultFileHandler;
import se.romram.handler.RelaxHandler;
import se.romram.server.RelaxServer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Created by micke on 2015-01-26.
 */
public class Main {

    public static final void main(String[] args) throws IOException {
        Properties props = new Properties(args);
        RelaxServer server = new RelaxServer(props.port, props.handler);
		if (props.threads > 10) {
			server.setExecutor(Executors.newFixedThreadPool(props.threads));
		}
        server.start();
    }

}
