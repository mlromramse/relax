package se.romram.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.romram.handler.AbstractHandler;

/**
 * Created by micke on 2015-01-01.
 */
public class RelaxServerHandler extends AbstractHandler {
    Logger log = LoggerFactory.getLogger(RelaxServerHandler.class);
    @Override
    public boolean handle(RelaxRequest relaxRequest, RelaxResponse relaxResponse) {
        if (!relaxRequest.getSocket().isClosed()) {
            log.info("A {} request for resource {} with queryParameters '{}' has been received by {} from user agent '{}'."
                    , relaxRequest.getMethod()
                    , relaxRequest.getRequestURL()
                    , relaxRequest.getQueryString()
                    , this.getClass().getSimpleName()
                    , relaxRequest.getUserAgent());
            if ("GET".equalsIgnoreCase(relaxRequest.getMethod())) {
                if ("/serverstats".equalsIgnoreCase(relaxRequest.getPath())) {
                    relaxResponse.respond(200, relaxRequest.getRelaxServer().getStats());
                    return true;
                }
            }
            if ("POST".equalsIgnoreCase(relaxRequest.getMethod()) || "GET".equalsIgnoreCase(relaxRequest.getMethod())) {
                if ("/echo".equalsIgnoreCase(relaxRequest.getPath())) {
                    relaxResponse.respond(200, relaxRequest.getRequestAsString());
                    return true;
                }
            }
            if ("TRACE".equalsIgnoreCase(relaxRequest.getMethod())) {
                relaxResponse.respond(200, "--ECHO-->" + relaxRequest.getRequestAsString() + "<--ECHO--\n\n");
                return true;
            }
            if ("OPTIONS".equalsIgnoreCase(relaxRequest.getMethod())) {
                relaxResponse.addHeaders("Allow: HEAD,GET,POST,PUT,DELETE,TRACE,OPTIONS,PATCH").respond(200, "");
                return true;
            }
        }
        log.debug("This request was not handled by {}.", this.getClass().getSimpleName());
        return false;
    }

}
