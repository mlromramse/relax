package se.romram.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.text.ParseException;
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
    private StringBuffer payloadBuffer = null;

    private String method;
    private String pathAndQuery;
    private String path = "";
    private String queryString;
    private String host;
    private String userAgent;
    private String accept;
    private int contentLength = -1;

    private Map<String, List<String>> queryMap;

    public RelaxRequest(Socket socket, RelaxServer relaxServer) {
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

    public int getContentLength() {
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
            int b = 0;
            int e = getRequestBuffer().indexOf(" ", b);
            if (e == -1) return;
            method = getRequestBuffer().substring(b, e);

            b = e + 1;
            e = getRequestBuffer().indexOf(" ", b);
            if (e == -1) return;
            pathAndQuery = getRequestBuffer().substring(b, e);
            parsePathQuery(pathAndQuery);

            b = getRequestBuffer().indexOf("Host:", e) + 5;
            if (b == -1) return;
            host = getRequestBuffer().substring(b, getRequestBuffer().indexOf("\n", b)).trim();

            b = getRequestBuffer().indexOf("User-Agent:", e) + 11;
            if (b == -1) return;
            userAgent = getRequestBuffer().substring(b, getRequestBuffer().indexOf("\n", b)).trim();

            b = getRequestBuffer().indexOf("Accept:", e) + 7;
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
		int qm = pathAndQuery.indexOf('?');
        if (qm==-1) {
            queryString = "";
            path = pathAndQuery;
        } else {
            queryString = pathAndQuery.substring(qm+1);
            path = pathAndQuery.substring(0, qm);
        }
    }

    public StringBuffer getPayload() {
        return payloadBuffer;
    }

    public String getRequestAsString() {
        return getRequestBuffer().toString();
    }

    public StringBuffer getRequestBuffer() {
        if (requestBuffer == null) {
            try {
                InputStream inputStream = socket.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                //ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream();

				if (inputStream == null) {
					System.out.println("BREAK!");
				}

                requestBuffer = new StringBuffer();
                String line;
                for (; (line = bufferedReader.readLine())!=null && !line.isEmpty(); ) {
                    requestBuffer.append(line);
                    requestBuffer.append("\n");
                    log.debug(line);
                }
                log.debug(line);
                requestBuffer.append("\n");

                writeContinue();

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
                    payloadBuffer = new StringBuffer();
                    char[] buf = new char[contentLength];
                    char[] data = new char[200];
                    int totRead = 0;
                    int read = 1;
                    while (read > 0 && totRead < contentLength) {
                        read = bufferedReader.read(buf, totRead, contentLength - totRead);
                        totRead = read > 0 ? totRead + read : totRead;
                        log.debug("{} bytes of {} total was read.", totRead, contentLength);
                        writeContinue();
                    }
                    payloadBuffer.append(buf);
                    requestBuffer.append(payloadBuffer);
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

    private void writeContinue() {
        BufferedOutputStream bufferedOutputStream;

        try {
            bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());
            bufferedOutputStream.write("HTTP/1.1 100 Continue\r\n\r\n".getBytes());
            log.info("Continue sent!");
        } catch (IOException e) {
            log.error("Failed to write continue.");
        }

    }


}
