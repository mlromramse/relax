package se.romram.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.romram.helpers.RelaxIO;

import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Created by micke on 2014-12-31.
 */
public class RelaxRequest {
    private Logger log = LoggerFactory.getLogger(RelaxRequest.class);

    private Socket socket;
    private RelaxServer relaxServer;

    private StringBuffer requestBuffer = null;
    private byte[] payloadBuffer = null;

	private String schema;
	private String userinfo;
    private String host;
	private int port;
    private String path = "";
    private String queryString;
	private String fragment;
    private String method;
    private String pathAndQuery;
    private String userAgent;
    private String accept;
    private int contentLength = -1;
	private String contentType;
	private Locale locale;

    private Map<String, List<String>> queryMap;
	private Map<String, String> headerMap;

    public RelaxRequest(Socket socket, RelaxServer relaxServer) {
		this.headerMap = new HashMap<>();
        this.socket = socket;
        this.relaxServer = relaxServer;
    }

    public Socket getSocket() {
        return socket;
    }

    public RelaxServer getRelaxServer() {
        return relaxServer;
    }

    public String getMethod() {
        parseRequest();
        return method;
    }

    public String getPath() {
        parseRequest();
        return path;
    }

    public String getRequestURL() {
        parseRequest();
        String protocol = socket.getPort()==443 ? "https://" : "http://";
        return protocol + host + path;
    }

    public String getQueryString() {
        parseRequest();
        return queryString;
    }

    public String getUserAgent() {
        parseRequest();
        return userAgent;
    }

    public String getAccept() {
        parseRequest();
        return accept;
    }

	public Map<String, String> getHeaderMap() {
		parseRequest();
		return headerMap;
	}

	public String getFromHeader(String name, String defaultValue) {
		String value = getHeaderMap().get(name);
		if (value != null) {
			return value;
		}
		return defaultValue;
	}

	public int getIntFromHeader(String name, int defaultValue) {
		String value = getHeaderMap().get(name);
		if (value != null) {
			return Integer.parseInt(value);
		}
		return defaultValue;
	}

	public int getContentLength() {
		parseRequest();
        return contentLength;
    }

	/**
	 * Picks the first value to the parameter with key=key.
	 * <p>
	 * <i>Simplified picker method to use when you expect only one value from a parameter.</i>
	 * <p>
	 *     For more advanced use see getQueryMap()
	 * </p>
	 * @param key The key of the parameter.
	 * @return The value of the parameter or null if missing.
	 */
	public String getParameter(String key) {
		if (getQueryMap() != null) {
			List<String> resultList = getQueryMap().get(key);
			if (resultList != null) {
				return resultList.get(0);
			}
		}
		return null;
	}

	/**
	 * Returns the complete map of query parameters.
	 * Each parameter can be reused, i.e. <code>http://domain.io/path?key=val1&key=val2</code>
	 * Therefore the values are held in a List of Strings.
	 * <p>
	 *     <i>For a simpler method when only one value is expected see getParameter(key)</i>
	 * </p>
	 * @return
	 */
	public Map<String, List<String>> getQueryMap() {
        parseRequest();
        if (queryMap == null && !queryString.isEmpty()) {
            queryMap = new HashMap<>();
            for(String query : queryString.split("&")) {
                String[] queryArr = query.split("=");
                String queryKey = queryArr[0].trim();
                String queryValue = "true";
                if (queryArr.length > 1) {
                    queryValue = queryArr[1].trim();
                }
                List<String> queryItemList = queryMap.get(queryKey);
                if (queryItemList == null) {
                    queryItemList = new ArrayList<>();
                }
                queryItemList.add(queryValue);
                queryMap.put(queryKey, queryItemList);
            }
        }
        return queryMap;
    }

    private void parseRequest() {
        if (method == null && getRequestBuffer() != null) {
			StringBuffer lowerCaseRequestBuffer = new StringBuffer(getRequestBuffer().toString().toLowerCase());
            int b = 0;
            int e = getRequestBuffer().indexOf(" ", b);
            if (e == -1) return;
            method = getRequestBuffer().substring(b, e);

            b = e + 1;
            e = lowerCaseRequestBuffer.indexOf(" ", b);
            if (e == -1) return;
            pathAndQuery = getRequestBuffer().substring(b, e);
            parsePathQuery(pathAndQuery);

            b = lowerCaseRequestBuffer.indexOf("host:", e) + 5;
            if (b == -1) return;
            host = getRequestBuffer().substring(b, getRequestBuffer().indexOf("\n", b)).trim();

            b = lowerCaseRequestBuffer.indexOf("user-agent:", e) + 11;
            if (b == -1) return;
            userAgent = getRequestBuffer().substring(b, getRequestBuffer().indexOf("\n", b)).trim();

            b = lowerCaseRequestBuffer.indexOf("accept:", e) + 7;
            if (b == -1) return;
            accept = getRequestBuffer().substring(b, getRequestBuffer().indexOf("\n", b)).trim();

        }
    }

    private void parsePathQuery(String pathAndQuery) {
		try {
			pathAndQuery = URLDecoder.decode(pathAndQuery, relaxServer.charsetName);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		if (pathAndQuery.indexOf("http") == 0) {
			pathAndQuery = pathAndQuery.replaceFirst("https?://", "");
		}
		int p=pathAndQuery.indexOf("/");
		p = p==-1 ? pathAndQuery.length() : p;
		pathAndQuery = pathAndQuery.substring(p);

		int qm = pathAndQuery.indexOf('?');
        if (qm==-1) {
            queryString = "";
            path = pathAndQuery;
        } else {
            queryString = pathAndQuery.substring(qm+1);
            path = pathAndQuery.substring(0, qm);
        }
    }

    public byte[] getPayload() {
		getRequestBuffer();
        return payloadBuffer;
    }

    public String getRequestAsString() {
        return getRequestBuffer().toString();
    }

    public StringBuffer getRequestBuffer() {
        if (requestBuffer == null && !socket.isClosed()) {
            try {
                InputStream inputStream = socket.getInputStream();

				if (inputStream == null) {
					log.error("The inputStream is null!");
				}

				requestBuffer = getHeadersFromStream(inputStream);
				parseHeaders(requestBuffer);

				boolean doWriteContinue = requestBuffer.indexOf("Expect:")!=-1 && requestBuffer.indexOf("100-continue")!=-1;

				if (doWriteContinue) writeContinue();

                contentLength = 0;
                int b = requestBuffer.indexOf("Content-Length:", 0) + 15;
                if (b > 14) {
                    String temp = requestBuffer.substring(b, requestBuffer.indexOf("\n", b)).trim();
                    try {
                        contentLength = Integer.parseInt(temp);
                    } catch (NumberFormatException ex) {
                        // No worries!
                    }
                }

                log.debug("About to read {} bytes", contentLength);

                if (contentLength > 0) {
					payloadBuffer = new RelaxIO().readInputStream(inputStream, contentLength);
                    requestBuffer.append(new String(payloadBuffer, relaxServer.charsetName));
				}
            } catch (IOException ex) {
                log.error("An exception of type {} with message '{}' was encountered when reading the inputstream from socket [{}]."
                        , ex.getClass().getSimpleName()
                        , ex.getMessage()
						, socket.isClosed() ? "closed" : "open"
                );
            }
        }
        return requestBuffer;
    }

	private void parseHeaders(StringBuffer requestBuffer) {
		String[] headerRows = requestBuffer.toString().split("\n");
		for (String row : headerRows) {
			String[] rowSplit = row.split(":");
			if (rowSplit.length > 1) {
				String name = rowSplit[0].trim();
				String value = "";
				for (int i=1; i<rowSplit.length; i++) {
					value += i==1 ? rowSplit[i] : ":" + rowSplit[i];
				}

				headerMap.put(name, value);
			}
		}
	}

	private StringBuffer getHeadersFromStream(InputStream inputStream) throws IOException {
		return new StringBuffer(new String(new RelaxIO().readInputStreamUntilTwoNewLines(inputStream)));
	}

	private void writeContinue() {
        BufferedOutputStream bufferedOutputStream;

        try {
            bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());
            bufferedOutputStream.write("HTTP/1.1 100 Continue\r\n\r\n".getBytes());
            log.debug("Continue sent!");
        } catch (IOException e) {
            log.error("Failed to write continue.");
        }

    }


}
