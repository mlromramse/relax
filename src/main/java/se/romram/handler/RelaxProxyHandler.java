package se.romram.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.romram.client.RelaxClient;
import se.romram.server.RelaxRequest;
import se.romram.server.RelaxResponse;


/**
 * Created by micke on 2015-05-12.
 */
public class RelaxProxyHandler implements RelaxHandler {
	Logger log = LoggerFactory.getLogger(RelaxProxyHandler.class);
	Map<String, Item> cache = new ConcurrentHashMap<>();

	public boolean handle(RelaxRequest request, RelaxResponse response) {
		if (request.getMethod().equalsIgnoreCase("connect")) {
			response.respond(200, "");
			return true;
		}
		if (request.getMethod().equalsIgnoreCase("delete") && request.getHeaderMap().containsKey("invalidate-cache")) {
			cache = new ConcurrentHashMap<>();
			response.respond(200, "Cache invalidate!\n\n");
			return true;
		}
		RelaxClient client = new RelaxClient();
		copyHeaders(request, client);

		String url = request.getRequestURL().concat(request.getQueryString().isEmpty() ? "" : "?"+request.getQueryString());
		url = url.replaceAll("\\[", "%5B").replaceAll("\\]", "%5D");
		Item item = cache.get(url);
		if (item == null) {
			client.setPayload(request.getPayload());
			client.perform(request.getMethod(), url);
			String payload = client.getBytes() == null ? "" : new String(client.getBytes()).replaceAll("performance.", "perf.");
			item = new Item(client.getStatus().getCode(), payload.getBytes());
			log.info("Cache miss!");
			log.debug("-------------------- KEY\n{}\n{}", url, item);
			if (client.getStatus().isOK()) {
				cache.put(url, item);
			}
		} else {
			log.info("Cache hit!");
		}
		try {
			Thread.sleep(0);
		} catch(InterruptedException e) {
		}
		response.respond(item.status, item.payload);
		return true;
	}

	private void copyHeaders(RelaxRequest request, RelaxClient client) {
		if (request.getHeaderMap().size() > 0) {
			log.debug("-------------------- Headers >>");
			for (String key : request.getHeaderMap().keySet()) {
				String header = key + ":" + request.getHeaderMap().get(key);
				if (!key.startsWith("GET")) {
					client.addRequestHeaders(header);
				}
				log.debug(header);
			}
			log.debug("-------------------- Headers <<");
		}
	}

	class Item {
		public byte[] payload;
		public int status;
		public Item(int status, byte[] payload) {
			this.status = status;
			this.payload = payload;
		}
		public String toString() {
			return String.format("-------------------- ITEM\n%s - %s\n--------------------\n"
					, status
					, payload!=null ? new String(payload) : ""
			);
		}
	}


}
