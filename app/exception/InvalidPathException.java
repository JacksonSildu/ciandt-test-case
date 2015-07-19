package exception;

public class InvalidPathException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public InvalidPathException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidPathException(String message) {
		super(message);
	}

}
