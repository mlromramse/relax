package se.romram.helpers;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by micke on 2015-04-28.
 */
public class RelaxIOTest {

	@Test
	public void testTwoNewlinesInARowLFLF() {
		String testString = "1234\n\n5678";
		byte[] lastFour = "abcd".getBytes();
		for (int i=0; i<testString.length(); i++) {
			if (new RelaxIO().twoNewLinesInARow((byte)testString.charAt(i), lastFour)) {
				System.out.printf("Position is at %s\n", i);
				break;
			}
		}
		assertEquals((byte)'\n', lastFour[2]);
		assertEquals((byte)'\n', lastFour[3]);
	}

	@Test
	public void testTwoNewlinesInARowCRCR() {
		String testString = "1234\r\r5678";
		byte[] lastFour = "abcd".getBytes();
		for (int i=0; i<testString.length(); i++) {
			if (new RelaxIO().twoNewLinesInARow((byte)testString.charAt(i), lastFour)) {
				System.out.printf("Position is at %s\n", i);
				break;
			}
		}
		assertEquals((byte)'\r', lastFour[2]);
		assertEquals((byte)'\r', lastFour[3]);
	}

	@Test
	public void testTwoNewlinesInARowCRLFCRLF() {
		String testString = "1234\r\n\r\n5678";
		byte[] lastFour = "abcd".getBytes();
		for (int i=0; i<testString.length(); i++) {
			if (new RelaxIO().twoNewLinesInARow((byte)testString.charAt(i), lastFour)) {
				System.out.printf("Position is at %s\n", i);
				break;
			}
		}
		assertEquals("\r\n\r\n", new String(lastFour));
	}

	@Test
	public void testTwoNewlinesInARowLFCRLFCR() {
		String testString = "1234\n\r\n\r5678";
		byte[] lastFour = "abcd".getBytes();
		for (int i=0; i<testString.length(); i++) {
			if (new RelaxIO().twoNewLinesInARow((byte)testString.charAt(i), lastFour)) {
				System.out.printf("Position is at %s\n", i);
				break;
			}
		}
		assertEquals("\n\r\n\r", new String(lastFour));
	}

	@Test
	public void testReadInputStreamUntilTwoNewLines() throws IOException {
		String data = "Rad ett\nRad två\nRad tre\n\nPayload data comes here.";
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data.getBytes());
		byte[] header = new RelaxIO().readInputStreamUntilTwoNewLines(byteArrayInputStream);
		byte[] payload = new RelaxIO().readInputStream(byteArrayInputStream);
		System.out.println(new String(header));
		System.out.println("--------------------------");
		System.out.println(new String(payload));
	}

}
