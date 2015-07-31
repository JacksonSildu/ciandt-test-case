package exception;

/**
 * 
 * @author Sildu
 *
 */
public class InvalidProtocolException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public InvalidProtocolException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidProtocolException(String message) {
		super(message);
	}

}
