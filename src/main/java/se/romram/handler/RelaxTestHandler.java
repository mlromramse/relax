package se.romram.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.romram.server.RelaxRequest;
import se.romram.server.RelaxResponse;

/**
 * Created by micke on 2014-12-02.
 */
public class RelaxTestHandler extends AbstractHandler {
	private Logger log = LoggerFactory.getLogger(RelaxTestHandler.class);

	public RelaxTestHandler() {
        log.info("This simple handler returns an http status code in case one is requested, e.g. /404. Always with the text 'It worked'");
	}

    @Override
    public boolean handle(RelaxRequest request, RelaxResponse response) {
        StringBuffer requestString = request.getRequestBuffer();
		if ("GET".equalsIgnoreCase(request.getMethod())) {
			if (request.getQueryMap() != null) {
				for (String key : request.getQueryMap().keySet()) {
					String row = key + "=";
					for (String value : request.getQueryMap().get(key)) {
						row += row.substring(row.length() - 1).equals("=") ? value : ", " + value;
					}
					log.debug(row);
				}
			}
			int status = 200;
			try {
				status = Integer.parseInt(request.getPath().substring(1));
			} catch (NumberFormatException e) {
				// No worries!
			}
			log.debug(requestString.toString());
			delay(request);
			response.respond(status, "It worked!");
			return true;
		}
		return false;
    }

    private void delay(RelaxRequest request) {
        if (request.getQueryMap() != null && request.getQueryMap().containsKey("delay")) {
            try {
                long sleep = Long.parseLong(request.getQueryMap().get("delay").get(0));
                Thread.sleep(sleep);
                log.debug("Sleeping for {} milliseconds", sleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}