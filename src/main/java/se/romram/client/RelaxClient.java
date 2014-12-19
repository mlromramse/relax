package se.romram.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.romram.enums.HttpMethod;
import se.romram.enums.HttpStatus;
import se.romram.exceptions.UncheckedHttpStatusCodeException;
import se.romram.exceptions.UncheckedMalformedURLException;

import java.io.*;
import java.net.*;
import java.util.Base64;

/**
 * Created by micke on 2014-12-02.
 */
public class RelaxClient {
	private Logger log = LoggerFactory.getLogger(RelaxClient.class);
	private HttpMethod httpMethod;
	private int timeOutMillis = 30000;
	private String charsetName = "UTF8";
	private byte[] payload = null;
	private StringBuffer result;
	private HttpStatus httpStatus;
	private boolean isExceptionsToBeThrown = false;
	private URL url = null;

	public RelaxClient headers(String... default_headers) {
		return this;
	}

	public RelaxClient useCookies() {
		return this;
	}

	public RelaxClient throwExceptions() {
		isExceptionsToBeThrown = true;
		return this;
	}

	public RelaxClient body(String body) {
		return this;
	}


	public RelaxClient get(String urlAsString) {
		httpMethod = HttpMethod.GET;
		setUrl(urlAsString);
		return doRequest();
	}

	public RelaxClient post(String urlAsString) {
		httpMethod = HttpMethod.POST;
		return doRequest();
	}

	public HttpStatus getStatus() {
		return httpStatus;
	}

	public String toString() {
		return result.toString();
	}

	/* Getters and Setters below */

	private boolean hasPayload() {
		if (payload == null) {
			return false;
		}
		return true;
	}

	private RelaxClient setUrl(String urlAsString) {
		try {
			url = new URL(urlAsString);
		} catch (MalformedURLException e) {
			if (isExceptionsToBeThrown) {
				throw new UncheckedMalformedURLException(e);
			}
		}
		return this;
	}

	/**
	 * This method does the actual communication to simplify the methods above.
	 */
	private RelaxClient doRequest() {
		result = new StringBuffer();
		URLConnection urlConnection = null;
//		response.setRequestObject(request);

//		request.updateCookies();

		httpStatus = HttpStatus.OK;

		try {
			urlConnection = (URLConnection) url.openConnection();
			urlConnection.setDoInput(true);
			if (urlConnection instanceof HttpURLConnection) {
				if (hasPayload() && httpMethod == HttpMethod.DELETE) {
					((HttpURLConnection) urlConnection).setRequestMethod(HttpMethod.POST.name());
				} else {
					((HttpURLConnection) urlConnection).setRequestMethod(httpMethod.name());
				}
			}
			urlConnection.setConnectTimeout(timeOutMillis);
			urlConnection.setReadTimeout(timeOutMillis);

			if (url.getUserInfo() != null) {
				String basicAuth = "Basic "
						+ new String(Base64.getEncoder().encode(url.getUserInfo().getBytes()));
				urlConnection.setRequestProperty("Authorization", basicAuth);
			}
			urlConnection.setRequestProperty("Content-Length", "0");
//			for (String key : request.getProperties().keySet()) {
//				urlConnection.setRequestProperty(key, request.getProperties().get(key));
//			}

			if (hasPayload()) {
				if (httpMethod == HttpMethod.DELETE) {
					urlConnection.setRequestProperty("X-HTTP-Method-Override", "DELETE");
				}
				urlConnection.setDoOutput(true);
				urlConnection.setRequestProperty("Content-Length",
						"" + payload.length);

				OutputStreamWriter outputStreamWriter =
						new OutputStreamWriter(urlConnection.getOutputStream(), charsetName);
				outputStreamWriter.write(new String(payload));
				// TODO Remove new String()
				outputStreamWriter.flush();
				outputStreamWriter.close();
			}

//			urlConnection.getHeaderFields();
			httpStatus = HttpStatus.valueOfCode(((HttpURLConnection) urlConnection).getResponseCode());

			if (httpStatus.isOK()) {
				readInputStream(result, urlConnection.getInputStream(), charsetName);
			} else {
				if (isExceptionsToBeThrown)
					throw new UncheckedHttpStatusCodeException(httpStatus);
				else
					log.error("{} Url: '{}'", httpStatus.toString(), url);
			}

		} catch (SocketTimeoutException e) {
			httpStatus = HttpStatus.REQUEST_TIMEOUT;
			log.error(httpStatus.toString());
			if (isExceptionsToBeThrown)
				throw new UncheckedHttpStatusCodeException(httpStatus, e);
		} catch (RuntimeException e) {
			if (! (e instanceof UncheckedHttpStatusCodeException)) {
				httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
				log.error(httpStatus.toString());
			}
			if (isExceptionsToBeThrown)
				throw new UncheckedHttpStatusCodeException(httpStatus, e);
		} catch (MalformedURLException e) {
			httpStatus = HttpStatus.BAD_REQUEST;
			log.error(httpStatus.toString());
			if (isExceptionsToBeThrown)
				throw new UncheckedHttpStatusCodeException(httpStatus, e);
		} catch (IOException e) {
			if (urlConnection instanceof HttpURLConnection) {
				try {
					readInputStream(result, ((HttpURLConnection) urlConnection).getErrorStream(),
							charsetName);
				} catch (IOException e1) {
					//TODO FIX
					httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
					if (isExceptionsToBeThrown)
						throw new UncheckedHttpStatusCodeException(httpStatus, e);
					log.error("No response from server at {}", url);
//					if (((HttpURLConnection) urlConnection).getResponseCode() < 0) {
//						response.setResponseCodeAndMessage(500, "Internal server error.");
//					}
//					if (request.isExceptionsToBeThrown()) {
//						if (e instanceof FileNotFoundException) {
//							throw new JCurlFileNotFoundException(e);
//						} else {
//							throw new JCurlIOException(e);
//						}
//					}
				}
			} else {
				httpStatus = HttpStatus.NOT_FOUND;
				if (isExceptionsToBeThrown)
					throw new UncheckedHttpStatusCodeException(httpStatus, e);
			}
		} finally {
			if (urlConnection != null && urlConnection instanceof HttpURLConnection) {
				((HttpURLConnection) urlConnection).disconnect();
			}
		}
//		response.setResponseString(result);
		return this;
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
