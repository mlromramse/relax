package se.romram.exceptions;

/**
 * Created by micke on 2014-12-03.
 */
public class UncheckedNotApplicableException extends RuntimeException {

	public UncheckedNotApplicableException(Throwable t) {
		super(t);
	}

    public UncheckedNotApplicableException(String message) {
        super(message);
    }

}
