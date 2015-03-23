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
	/**
	 * Local helper method that reads data from an input stream.
	 *
	 * @param inputStream
	 *            The stream to read.
	 * @throws java.io.IOException
	 */
	public static byte[] readInputStream(InputStream inputStream) throws IOException {
		if (inputStream == null)
			throw new IOException("No working inputStream.");
		BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

		int bytesRead = 0;
		byte[] buf = new byte[8192];
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		while ((bytesRead = bufferedInputStream.read(buf)) != -1) {
			byteArrayOutputStream.write(Arrays.copyOf(buf, bytesRead));
		}

		return byteArrayOutputStream.toByteArray();

	}


}
