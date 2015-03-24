package se.romram.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.romram.server.RelaxRequest;
import se.romram.server.RelaxResponse;

/**
 * Created by micke on 2015-01-01.
 */
public class RelaxStatsHandler extends AbstractHandler {
    Logger log = LoggerFactory.getLogger(RelaxStatsHandler.class);
    @Override
    public boolean handle(RelaxRequest relaxRequest, RelaxResponse relaxResponse) {
        if (!relaxRequest.getSocket().isClosed()) {
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
        return false;
    }

}
