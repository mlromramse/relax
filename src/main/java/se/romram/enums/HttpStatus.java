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
	, INTERNAL_SERVER_ERROR(500, "Internal Server Error")
	, NOT_IMPLEMENTED(501, "Not Implemented")
	, BAD_GATEWAY(502, "Bad Gateway")
	, SERVICE_UNAVAILABLE(503, "Service Unavailable")
	, GATEWAY_TIMEOUT(504, "Gateway Timeout")
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
		return (code == 200 || code == 201 || code == 202 || code == 302);
	}

	public String toString() {
		return "" + code + " " + description;
	}

}
