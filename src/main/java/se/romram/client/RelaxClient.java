package se.romram.client;

import org.ow2.util.base64.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.romram.cookie.RelaxCookie;
import se.romram.cookie.RelaxCookieManager;
import se.romram.enums.HttpMethod;
import se.romram.enums.HttpStatus;
import se.romram.exceptions.UncheckedHttpStatusCodeException;
import se.romram.exceptions.UncheckedMalformedURLException;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
    private RelaxCookieManager cookieManager;
    private Map<String, List<String>> requestHeaderFields = new HashMap<>();
    private Map<String, List<String>> responseHeaderFields;

	public RelaxClient addRequestHeaders(String... headersArr) {
        for (String compoundHeader : headersArr) {
            String[] splitHeader = compoundHeader.split(":");
            String key = splitHeader[0].trim();
            String value = splitHeader[1].trim();
            List<String> valueList = requestHeaderFields.get(key);
            if (valueList == null) {
                valueList = new ArrayList<>();
            }
            valueList.add(value);
            requestHeaderFields.put(key, valueList);
        }
		return this;
	}

	public RelaxClient useDefaultCookieManager() {
		return useCookieManager(new RelaxCookieManager());
	}

    public RelaxClient useCookieManager(RelaxCookieManager relaxCookieManager) {
        cookieManager = relaxCookieManager;
        return this;
    }

    public RelaxCookieManager getCookieManager() {
        return cookieManager;
    }

    public RelaxClient throwExceptions() {
		isExceptionsToBeThrown = true;
		return this;
	}

	public RelaxClient setPayload(String payload) {
        try {
            this.payload = payload.getBytes(charsetName);
        } catch (UnsupportedEncodingException e) {
            log.error("The selected charset '{}' is not supported.", charsetName);
        }
        return this;
	}

    public RelaxClient head(String urlAsString) {
        httpMethod = HttpMethod.HEAD;
        setUrl(urlAsString);
        return doRequest();
    }

	public RelaxClient get(String urlAsString) {
		httpMethod = HttpMethod.GET;
		setUrl(urlAsString);
		return doRequest();
	}

	public RelaxClient post(String urlAsString) {
		httpMethod = HttpMethod.POST;
        setUrl(urlAsString);
		return doRequest();
	}

    public RelaxClient put(String urlAsString) {
        httpMethod = HttpMethod.PUT;
        setUrl(urlAsString);
        return doRequest();
    }

    public RelaxClient delete(String urlAsString) {
        httpMethod = HttpMethod.DELETE;
        setUrl(urlAsString);
        return doRequest();
    }

    public RelaxClient trace(String urlAsString) {
        httpMethod = HttpMethod.TRACE;
        setUrl(urlAsString);
        return doRequest();
    }

    public RelaxClient options(String urlAsString) {
        httpMethod = HttpMethod.OPTIONS;
        setUrl(urlAsString);
        return doRequest();
    }

    public RelaxClient connect(String urlAsString) {
        httpMethod = HttpMethod.CONNECT;
        setUrl(urlAsString);
        return doRequest();
    }

    public RelaxClient patch(String urlAsString) {
        httpMethod = HttpMethod.PATCH;
        setUrl(urlAsString);
        return doRequest();
    }

	/* Getters and Setters below */

	public HttpStatus getStatus() {
		return httpStatus;
	}

    public URL getUrl() {
        return url;
    }

    public Map<String, List<String>> getResponseHeaderFields() {
        return responseHeaderFields;
    }

    public String getResponseHeaderFieldsAsFormattedString() {
        StringBuilder result = new StringBuilder();
        for (String key : getResponseHeaderFields().keySet()) {
            if (key != null) {
                result.append(key);
                result.append(": ");
                List<String> values = getResponseHeaderFields().get(key);
                for (String value : values) {
                    result.append(result.charAt(result.length() - 2) == ':' ? value : ", " + value);
                }
            } else {
                result.append(getResponseHeaderFields().get(key).get(0));
            }
            result.append("\r\n");
        }
        return result.toString();
    }

	public String toString() {
		return result == null ? "null" : result.toString();
	}

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

		httpStatus = HttpStatus.OK;

		try {
            urlConnection = getUrlConnection(urlConnection);

            authorize(urlConnection);

            setRequestHeaderFields(urlConnection);

			if (hasPayload()) {
				if (httpMethod == HttpMethod.DELETE) {
					urlConnection.setRequestProperty("X-HTTP-Method-Override", "DELETE");
                    //TODO: Manage correct DELETE if available in current java version.
				}
				urlConnection.setDoOutput(true);
				urlConnection.setRequestProperty("Content-Length",
						"" + payload.length);

                urlConnection.getOutputStream().write(payload);
                urlConnection.getOutputStream().flush();
                urlConnection.getOutputStream().close();
			}

			responseHeaderFields = urlConnection.getHeaderFields();
			httpStatus = HttpStatus.valueOfCode(((HttpURLConnection) urlConnection).getResponseCode());
            updateCookiesFromResponse(responseHeaderFields);

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

    private void updateCookiesFromResponse(Map<String, List<String>> responseHeaderFields) {
        if (cookieManager != null) {
            cookieManager.updateCookiesFromHeaderFields(responseHeaderFields);
        }
    }

    private void setRequestHeaderFields(URLConnection urlConnection) {
        urlConnection.setRequestProperty("Content-Length", "0");
        updateRequestHeaderFieldsWithCookies();
        for (String key : requestHeaderFields.keySet()) {
            String values = "";
            for (String value : requestHeaderFields.get(key)) {
                values += values.length()==0 ? value : "," + value;
            }
            urlConnection.setRequestProperty(key, values);
        }
    }

    private void updateRequestHeaderFieldsWithCookies() {
        if (cookieManager != null) {
            StringBuffer cookieString = cookieManager.getCookieRequestHeaderBuffer(url);
            if (cookieString.length() != 0) {
                addRequestHeaders("Cookie: " + cookieString.toString());
            }
        }
    }

    private void authorize(URLConnection urlConnection) {
        if (url.getUserInfo() != null) {
            String basicAuth = "Basic "
                    + new String(Base64.encode(url.getUserInfo().getBytes()));
            urlConnection.setRequestProperty("Authorization", basicAuth);
        }
    }

    private URLConnection getUrlConnection(URLConnection urlConnection) throws IOException {
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
        return urlConnection;
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
