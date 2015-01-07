package se.romram.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.romram.enums.HttpMethod;
import se.romram.enums.HttpStatus;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by micke on 2014-12-31.
 */
public class RelaxResponse {
    Logger log = LoggerFactory.getLogger(RelaxResponse.class);

    private RelaxRequest relaxRequest;
    private RelaxServer relaxServer;
    private List<String> headerList = new ArrayList<>();
    private String contentType;

    public RelaxResponse(RelaxRequest relaxRequest, RelaxServer relaxServer) {
        this.relaxRequest = relaxRequest;
        this.relaxServer = relaxServer;
        this.contentType = relaxServer.getContentType();
        this.headerList.addAll(relaxServer.getHeaderList());
    }

    public RelaxResponse addHeaders(String... headerArr) {
        for (String header : headerArr) {
            headerList.add(header);
        }
        return this;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    protected String getHeaders() {
        StringBuilder result = new StringBuilder();
        for (String header : headerList) {
            result.append(header);
            result.append("\r\n");
        }
        return result.toString();
    }

    public RelaxResponse respond(int status, String responseAsString, Object... values) {
        byte[] response;
        try {
            response = responseAsString.getBytes(relaxServer.charsetName);
        } catch (UnsupportedEncodingException e) {
            log.warn("UnsupportedEncodingException occurred, response payload not encoded!");
            response = responseAsString.getBytes();
        }
        return respond(status, response, values);
    }

    public RelaxResponse respond(int status, byte[] response, Object... values) {
        Socket socket = relaxRequest.getSocket();
        BufferedOutputStream bufferedOutputStream;
//		log.info("A request has been received from {} and will be handled by thread {}."
//				, socket.getInetAddress().getHostAddress()
//				, Thread.currentThread().getId()
//		);
        try {
            bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());

            int contentLength = 0;

            if (response.length > 0) {
                if (values.length > 0) {
                    response = String.format(new String(response), values).getBytes(relaxServer.charsetName);
                }
//                try {
//                    response = response.getBytes(relaxServer.charsetName);
                    contentLength = response.length;
//                } catch (UnsupportedEncodingException e) {
//                    e.printStackTrace();
//                }
            } else if (values.length > 0) {
                contentLength = Integer.parseInt(String.format("%s", values));
            }
            addHeaders("Content-Length: " + contentLength);
            addHeaders("Content-Type: " + contentType);
            writeHeaders(bufferedOutputStream, status);
            bufferedOutputStream.write(response);

            bufferedOutputStream.flush();
            bufferedOutputStream.close();
//			log.debug("The input string for thread {} is closed."
//					, Thread.currentThread().getId()
//			);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
//			log.debug("The handler serving thread {} is done."
//					, Thread.currentThread().getId()
//			);
        }
        return this;
    }

    private static String HTTP_HEADER = "HTTP/1.1 %s %s\r\n%s\r\n";

    private void writeHeaders(BufferedOutputStream bufferedOutputStream, int code) throws IOException {
        bufferedOutputStream.write(String.format(HTTP_HEADER, code, HttpStatus.valueOfCode(code).getDescription(), getHeaders()).getBytes());
    }


}
