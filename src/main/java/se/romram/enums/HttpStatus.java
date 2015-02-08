package se.romram.enums;

/**
 * Created by micke on 2014-12-03.
 */
public enum HttpStatus {
	  OK(200, "OK")
	, CREATED(201, "Created")
	, ACCEPTED(202, "Accepted")
	, NO_CONTENT(204, "No Content")
	, RESET_CONTENT(205, "Reset Content")
    , PARTIAL_CONTENT(206, "Partial Content")
	, MULTIPLE_CHOICES(300, "Multiple Choices")
	, MOVED_PERMANENTLY(301, "Moved Permanently")
	, FOUND(302, "Found")
	, NOT_MODIFIED(304, "Not Modified")
	, BAD_REQUEST(400, "Bad Request")
	, UNAUTHORIZED(401, "Unauthorized")
	, PAYMENT_REQUIRED(402, "Payment Required")
	, FORBIDDEN(403, "Forbidden")
	, NOT_FOUND(404, "Not Found")
	, METHOD_NOT_ALLOWED(405, "Method Not Allowed")
	, REQUEST_TIMEOUT(408, "Request Timeout")
	, CONFLICT(409, "Conflict")
    , GONE(410, "Gone")
    , LENGTH_REQUIRED(411, "Length Required")
    , PRECONDITIONS_FAILED(412, "Preconditions Failed")
    , REQUEST_ENTITY_TOO_LARGE(413, "Request Entity Too Large")
    , REQUEST_URI_TOO_LONG(414, "Request URI Too Long")
    , UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type")
    , REQUESTED_RANGE_NOT_SATISFIABLE(416, "Requested Range Not Satisfiable")
    , EXPECTATIONS_FAILED(417, "Expectations Failed")
    , I_AM_A_TEAPOT(418, "I\'m a Teapot")
    , AUTHENTICATION_TIMEOUT(419, "Authentication Timeout")
    , METHOD_FAILURE(420, "Method Failure")
    , UN_PROCESSABLE_ENTITY(422, "Un-processable Entity")
    , LOCKED(423, "Locked")
    , FAILED_DEPENDENCY(424, "Failed Dependency")
    , UPGRADE_REQUIRED(424, "Upgrade Required")
    , PRECONDITION_REQUIRED(426, "Precondition Required")
    , TOO_MANY_REQUESTS(429, "Too Many Requests")
    , REQUEST_HEADER_FIELDS_TOO_LARGE(431, "Request Header Fields Too Large")
    , LOGIN_TIMEOUT(440, "Login Timeout")
    , NO_RESPONSE(444, "No Response")
	, INTERNAL_SERVER_ERROR(500, "Internal Server Error")
	, NOT_IMPLEMENTED(501, "Not Implemented")
	, BAD_GATEWAY(502, "Bad Gateway")
	, SERVICE_UNAVAILABLE(503, "Service Unavailable")
	, GATEWAY_TIMEOUT(504, "Gateway Timeout")
    , HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version Not Supported")
	, UNKNOWN_ERROR(999, "Unknown error")
	;

	private int code;
	private String description;

	private HttpStatus(int code, String description) {
		this.code = code;
		this.description = description;
	}

	public int getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}

	public static HttpStatus valueOfCode(int code) {
		for (HttpStatus status : HttpStatus.values()) {
			if (status.code == code) {
				return status;
			}
		}
		return UNKNOWN_ERROR;
	}

	public boolean isOK() {
		return (code == 200 || code == 201 || code == 202 || code == 302 || code == 304);
	}

	public String toString() {
		return "" + code + " " + description;
	}

}
