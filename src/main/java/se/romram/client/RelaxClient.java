package se.romram.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URLConnection;

/**
 * Created by micke on 2014-12-02.
 */
public class RelaxClient {
	private Logger log = LoggerFactory.getLogger(RelaxClient.class);
	public RelaxClient headers(String... default_headers) {
		return this;
	}

	public RelaxClient useCookies() {
		return this;
	}

	public RelaxClient body(String body) {
		return this;
	}

	public RelaxClient get(String urlAsString) {
		return this;
	}

	public RelaxClient post(String urlAsString) {
		return this;
	}

	/**
	 * This method does the actual communication to simplify the methods above.
	 *
	 * @param request
	 *            A populated JCurlRequest object to the wanted resource.
	 * @param response
	 *            An instantiated JCurlResponse object.
	 */
	private static void doHttpCall(JCurlRequest request, JCurlResponse response) {
		Logger log = LoggerFactory.getLogger(JCurl.class);
		StringBuffer result = new StringBuffer();
		URLConnection urlConnection = null;
		response.setRequestObject(request);

		request.updateCookies();

		try {
			urlConnection = (URLConnection) request.getURL().openConnection();
			urlConnection.setDoInput(true);
			if (urlConnection instanceof HttpURLConnection) {
				if (request.hasPayload() && request.getMethod().equals(JCurlRequest.DELETE)) {
					((HttpURLConnection) urlConnection).setRequestMethod(JCurlRequest.POST);
				} else {
					((HttpURLConnection) urlConnection).setRequestMethod(request.getMethod());
				}
			}
			urlConnection.setConnectTimeout(request.getTimeOutMillis());
			urlConnection.setReadTimeout(request.getTimeOutMillis());

			if (request.getURL().getUserInfo() != null) {
				String basicAuth = "Basic "
						+ new String(new Base64().encode(request.getURL().getUserInfo().getBytes()));
				urlConnection.setRequestProperty("Authorization", basicAuth);
			}
			urlConnection.setRequestProperty("Content-Length", "0");
			for (String key : request.getProperties().keySet()) {
				urlConnection.setRequestProperty(key, request.getProperties().get(key));
			}

			if (request.hasPayload()) {
				if (request.getMethod().equals(JCurlRequest.DELETE)) {
					urlConnection.setRequestProperty("X-HTTP-Method-Override", "DELETE");
				}
				urlConnection.setDoOutput(true);
				urlConnection.setRequestProperty("Content-Length",
						"" + request.getPayload().getBytes(request.getCharsetName()).length);

				OutputStreamWriter outputStreamWriter = new OutputStreamWriter(urlConnection.getOutputStream(),
						request.getCharsetName());
				outputStreamWriter.write(request.getPayload());
				outputStreamWriter.flush();
				outputStreamWriter.close();
			}

			response.updateFromUrlConnection(urlConnection);

			readInputStream(result, urlConnection.getInputStream(), request.getCharsetName());

		} catch (SocketTimeoutException e) {
			response.setResponseCodeAndMessage(408, "The socket connection timed out.");
			log.error("The socket timed out after {} milliseconds.", request.getTimeOutMillis());
			if (request.isExceptionsToBeThrown())
				throw new JCurlSocketTimeoutException(e);
		} catch (RuntimeException e) {
			response.setResponseCodeAndMessage(500, "Internal server error.");
			log.error(e.getMessage());
			if (request.isExceptionsToBeThrown())
				throw e;
		} catch (MalformedURLException e) {
			response.setResponseCodeAndMessage(400, "The url is malformed.");
			log.error("The url '{}' is malformed.", request.getUrlAsString());
			if (request.isExceptionsToBeThrown())
				throw new JCurlMalformedURLException(e);
		} catch (IOException e) {
			if (urlConnection instanceof HttpURLConnection) {
				try {
					readInputStream(result, ((HttpURLConnection) urlConnection).getErrorStream(),
							request.getCharsetName());
				} catch (IOException e1) {
					if (response.getResponseCode() < 0) {
						response.setResponseCodeAndMessage(500, "Internal server error.");
					}
					if (request.isExceptionsToBeThrown()) {
						if (e instanceof FileNotFoundException) {
							throw new JCurlFileNotFoundException(e);
						} else {
							throw new JCurlIOException(e);
						}
					}
				}
			} else {
				response.setResponseCodeAndMessage(404, "Not Found.");
				if (request.isExceptionsToBeThrown())
					throw new JCurlFileNotFoundException(e);
			}
		} finally {
			if (urlConnection != null && urlConnection instanceof HttpURLConnection) {
				((HttpURLConnection) urlConnection).disconnect();
			}
		}
		response.setResponseString(result);
	}

	/**
	 * Local helper method that reads data from an input stream.
	 *
	 * @param result
	 *            The read text.
	 * @param inputStream
	 *            The stream to read.
	 * @param charsetName
	 *            The name of the char-set to be used to convert the read pay-load.
	 * @throws java.io.UnsupportedEncodingException
	 * @throws IOException
	 */
	private static void readInputStream(StringBuffer result, InputStream inputStream, String charsetName)
			throws UnsupportedEncodingException, IOException {
		if (inputStream == null)
			throw new IOException("No working inputStream.");
		InputStreamReader streamReader = new InputStreamReader(inputStream, charsetName);
		BufferedReader bufferedReader = new BufferedReader(streamReader);

		String row;
		while ((row = bufferedReader.readLine()) != null) {
			result.append(row);
			result.append("\n");
		}

		bufferedReader.close();
		streamReader.close();
	}



}
