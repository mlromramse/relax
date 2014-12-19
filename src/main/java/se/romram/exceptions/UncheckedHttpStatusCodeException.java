package se.romram.exceptions;

import se.romram.enums.HttpStatus;

/**
 * Created by micke on 2014-12-03.
 */
public class UncheckedHttpStatusCodeException extends RuntimeException {
	private HttpStatus httpStatus;

	public UncheckedHttpStatusCodeException(HttpStatus httpStatus) {
		super();
		this.httpStatus = httpStatus;
	}

	public UncheckedHttpStatusCodeException(HttpStatus httpStatus, Throwable t) {
		super(t);
		this.httpStatus = httpStatus;
	}

	@Override
	public String getMessage() {
		return String.format("UncheckedHttpStatusCodeException since [%s] %s."
				, httpStatus.getCode()
				, httpStatus.getDescription()
		);
	}

}
