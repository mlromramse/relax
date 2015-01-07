package se.romram.handler;

import se.romram.server.RelaxRequest;
import se.romram.server.RelaxResponse;

import java.io.IOException;

/**
 * Created by micke on 2014-12-30.
 */
public interface RelaxHandler {

    public boolean handle(RelaxRequest relaxRequest, RelaxResponse relaxResponse);
}
