package exception;

/**
 * 
 * @author Sildu
 *
 */
public class WorkerInterruptException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public WorkerInterruptException(String message, Throwable cause) {
		super(message, cause);
	}

	public WorkerInterruptException(String message) {
		super(message);
	}

}
