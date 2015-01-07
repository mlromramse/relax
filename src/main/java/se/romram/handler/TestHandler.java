package se.romram.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.romram.enums.HttpStatus;
import se.romram.exceptions.UncheckedHttpStatusCodeException;
import se.romram.server.RelaxRequest;
import se.romram.server.RelaxResponse;

/**
 * Created by micke on 2014-12-02.
 */
public class TestHandler extends AbstractHandler {
	private Logger log = LoggerFactory.getLogger(TestHandler.class);

	public TestHandler() {
        log.info("This simple handler returns an http status code in case one is requested, e.g. /404. Always with the text 'It worked'");

	}

    @Override
    public boolean handle(RelaxRequest request, RelaxResponse response) {
        StringBuffer requestString = request.getRequestBuffer();
        log.debug("A {} request for resource {} with queryParameters '{}' has been received from user agent '{}'."
                , request.getMethod()
                , request.getRequestURL()
                , request.getQueryString()
                , request.getUserAgent());
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
        response.respond(status, "It worked!");
        return true;
    }

}
