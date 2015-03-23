package se.romram.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by micke on 2014-12-31.
 */
public class RelaxRequest {
    private Logger log = LoggerFactory.getLogger(RelaxRequest.class);

    private Socket socket;
    private RelaxServer relaxServer;

    private StringBuffer requestBuffer = null;
    private byte[] payloadBuffer = null;

    private String method;
    private String pathAndQuery;
    private String path = "";
    private String queryString;
    private String host;
    private String userAgent;
    private String accept;
    private int contentLength = -1;

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

    public Map<String, List<String>> getQueryMap() {
        parseRequest();
        if (queryMap == null && !queryString.isEmpty()) {
            queryMap = new HashMap<>();
            for(String query : queryString.split("&")) {
                String[] queryArr = query.split("=");
                String queryKey = queryArr[0].trim();
                String queryValue = queryArr[1].trim();
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
        if (method == null) {
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
			pathAndQuery = pathAndQuery.replaceAll("https?://", "");
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
        if (requestBuffer == null) {
            try {
                InputStream inputStream = socket.getInputStream();

				if (inputStream == null) {
					log.error("The inputStream is null!");
				}

				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
				BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

                //ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream();

				requestBuffer = getHeadersFromStream(bufferedInputStream);
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
                    payloadBuffer = read(bufferedInputStream, contentLength);
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

	private byte[] read(BufferedInputStream bufferedInputStream, int numChars) throws IOException {
		byte[] buf = new byte[numChars];
		int totRead = 0;
		int read = 1;
		while (read > 0 && totRead < numChars) {
			read = bufferedInputStream.read(buf, totRead, numChars - totRead);
			totRead = read > 0 ? totRead + read : totRead;
			log.debug("{} bytes of {} total was read.", totRead, numChars);
		}
		return buf;
	}

	private StringBuffer getHeadersFromStream(BufferedInputStream bufferedInputStream) throws IOException {
		StringBuffer result = new StringBuffer();

		String line;
		while (!(line = readLine(bufferedInputStream)).isEmpty()) {
			result.append(line);
			result.append("\n");
		}
		result.append("\n");

		return result;
	}

	private String readLine(BufferedInputStream bufferedInputStream) throws IOException {
		ByteBuffer byteBuffer = ByteBuffer.allocate(8000);

		boolean done = false;
		byte[] byteArray = new byte[1];
		while (!done && bufferedInputStream.read(byteArray) > 0) {
			if (byteArray[0] == '\n') {
				break;
			}
			if (byteArray[0] != '\r') {
				byteBuffer.put(byteArray[0]);
			}
		}
		int pos = byteBuffer.position();
		byte[] tmp = new byte[pos];
		byteBuffer.position(0);
		byteBuffer.get(tmp, 0, pos);
		byteBuffer.position(pos);
		String result = new String(tmp, "utf8");
		return result;
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
