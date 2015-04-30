package se.romram.helpers;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Created by micke on 2015-03-17.
 */
public class RelaxIO {

	public byte[] readInputStream(InputStream inputStream) throws IOException {
		return readInputStream(inputStream, Integer.MAX_VALUE);
	}
	/**
	 * Local helper method that reads data from an input stream.
	 *
	 * @param inputStream
	 *            The stream to read.
	 * @throws java.io.IOException
	 */
	public byte[] readInputStream(InputStream inputStream, int maxlen) throws IOException {
		if (inputStream == null)
			throw new IOException("No working inputStream.");
		BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

		int totRead = 0;
		int bytesRead = 0;
		byte[] buf = new byte[8192];
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		while ((bytesRead = bufferedInputStream.read(buf, 0, maxlen-totRead>buf.length?buf.length:maxlen-totRead)) != -1) {
			byteArrayOutputStream.write(Arrays.copyOf(buf, bytesRead));
			totRead += bytesRead;
			if (totRead==maxlen) break;
		}

		return byteArrayOutputStream.toByteArray();

	}

	public byte[] readInputStreamUntilTwoNewLines(InputStream inputStream) throws IOException {
		if (inputStream == null)
			throw new IOException("No working inputStream.");
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		byte[] lastFour = new byte[4];
		byte[] oneByte = new byte[1];
		while (inputStream.read(oneByte) != -1) {
			byteArrayOutputStream.write(oneByte);
			if (twoNewLinesInARow(oneByte[0], lastFour)) {
				break;
			}
		}
		return byteArrayOutputStream.toByteArray();
	}

	protected boolean twoNewLinesInARow(byte oneByte, byte[] lastFour) {
		String[] matches = {"\n\n", "\r\r", "\n\r\n\r", "\r\n\r\n"};
		for (int i=0; i<3; i++) {
			lastFour[i] = lastFour[i+1];
		}
		lastFour[3] = oneByte;
		for (int index=0; index<matches.length; index++) {
			byte[] match = matches[index].getBytes();
			int diff = lastFour.length-match.length;
			byte[] compare = Arrays.copyOfRange(lastFour, diff, lastFour.length);
			if (Arrays.equals(match, compare)) {
				return true;
			}
		}
		return false;
	}


}
