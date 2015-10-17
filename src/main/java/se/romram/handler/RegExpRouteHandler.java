package se.romram.handler;

import se.romram.enums.HttpMethod;
import se.romram.server.RelaxRequest;
import se.romram.server.RelaxResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by micke on 2015-03-05.
 */
public class RegExpRouteHandler implements RelaxHandler {
	List<RelaxRoute> routeList = new ArrayList<>();

	public RegExpRouteHandler() {

	}

	public RegExpRouteHandler(HttpMethod httpMethod, String regExp, RelaxHandler defaultHandler) {
		addRoute(httpMethod, regExp, defaultHandler);
	}

	@Override
	public boolean handle(RelaxRequest relaxRequest, RelaxResponse relaxResponse) {
		RelaxHandler handler = findHandler(relaxRequest);
		return handler==null ? false : handler.handle(relaxRequest, relaxResponse);
	}

	public RegExpRouteHandler addRoute(HttpMethod httpMethod, String regExp, RelaxHandler relaxHandler) {
		int pos = routeList.size()==0 ? 0 : routeList.size()-1;
		routeList.add(pos, new RelaxRoute(httpMethod, regExp, relaxHandler));
		return this;
	}

	private RelaxHandler findHandler(RelaxRequest relaxRequest) {
		if (routeList.size() == 0) {
			return null;
		}
		for (RelaxRoute route: routeList) {
			if (route.match(relaxRequest)) {
				return route.getRelaxHandler();
			}
		}
		return routeList.get(routeList.size()-1).getRelaxHandler();
	}

}

class RelaxRoute {
	private HttpMethod httpMethod;
	private String regExpAsString;
	private RelaxHandler relaxHandler;

	public RelaxRoute(HttpMethod httpMethod, String regExpAsString, RelaxHandler relaxHandler) {
		this.httpMethod = httpMethod;
		this.regExpAsString = regExpAsString;
		this.relaxHandler = relaxHandler;
	}

	public boolean match(RelaxRequest relaxRequest) {
		if (relaxRequest.getMethod().equalsIgnoreCase(httpMethod.name())) {
			if (relaxRequest.getPath().matches(regExpAsString)) {
				return true;
			}
		}
		return false;
	}

	public HttpMethod getHttpMethod() {
		return httpMethod;
	}

	public RelaxHandler getRelaxHandler() {
		return relaxHandler;
	}

}
