package se.romram.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.stream.Collectors;

/**
 * Created by micke on 2014-12-02.
 */
public class RelaxHandler extends Thread {
	private Logger log = LoggerFactory.getLogger(RelaxHandler.class);
	private RelaxServer relaxServer;
	private Socket socket;

	public RelaxHandler(Socket socket, RelaxServer relaxServer) {
		this.relaxServer = relaxServer;
		this.socket = socket;
	}

	public void run() {
		BufferedReader bufferedReader;
		BufferedOutputStream bufferedOutputStream;
		log.info("A request has been received from {} and will be handled by thread {}."
				, socket.getInetAddress().getHostAddress()
				, Thread.currentThread().getId()
		);
		try {
			bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());

			log.debug("The input string for thread {} is opened."
					, Thread.currentThread().getId()
			);

			StringBuffer buf = new StringBuffer();
			for (String line; !(line = bufferedReader.readLine()).isEmpty();) {
				buf.append(line);
				buf.append("\n");
			}
			//String buff = bufferedReader.lines().parallel().collect(Collectors.joining("\n"));
			log.debug("[{}] {}", relaxServer.charsetName, buf);
			byte[] request = buf.toString().getBytes(relaxServer.charsetName);
			log.debug(new String(request));

			writeHeaders(bufferedOutputStream, 200, request);
			bufferedOutputStream.write(request);

			bufferedOutputStream.flush();
			bufferedOutputStream.close();
			log.debug("The input string for thread {} is closed."
					, Thread.currentThread().getId()
			);
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
			log.debug("The handler serving thread {} is done."
					, Thread.currentThread().getId()
			);
		}
	}

	private static String HTTP_HEADER = "HTTP/1.1 %s OK\r\nContent-Type: text/plain\r\nContent-Length: %s\r\n\r\n";

	private void writeHeaders(BufferedOutputStream bufferedOutputStream, int code, byte[] bytes) throws IOException {
		bufferedOutputStream.write(String.format(HTTP_HEADER, code, bytes.length).getBytes());
	}

}
