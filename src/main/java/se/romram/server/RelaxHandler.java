package se.romram.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Created by micke on 2014-12-02.
 */
public class RelaxHandler extends Thread {
	private Logger log = LoggerFactory.getLogger(RelaxHandler.class);
	private RelaxServer relaxServer;

	public RelaxHandler(RelaxServer relaxServer) {
		this.relaxServer = relaxServer;
	}

	public void run() {
		log.info("A request has been received and will be handled.");
		try {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(relaxServer.socket.getInputStream()));
			BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(relaxServer.socket.getOutputStream());

			while (!bufferedReader.ready()) ;
			String buff = bufferedReader.readLine();
			log.debug("[{}] {}", relaxServer.charsetName, buff);
			byte[] request = buff.getBytes(relaxServer.charsetName);
			log.debug(new String(request));
			writeHeaders(bufferedOutputStream, 200, request);
			bufferedOutputStream.write(request);

			bufferedOutputStream.flush();
			bufferedOutputStream.close();
			bufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		}
	}

	private static String HTTP_HEADER = "HTTP/1.1 %s OK\r\nContent-Type: text/plain\r\nContent-Length: %s\r\n\r\n";

	private void writeHeaders(BufferedOutputStream bufferedOutputStream, int code, byte[] bytes) throws IOException {
		bufferedOutputStream.write(String.format(HTTP_HEADER, code, bytes.length).getBytes());
	}

}
