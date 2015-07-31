package models;

import enums.StatusEnum;

/**
 * 
 * @author Sildu
 *
 */
public class StatusMap {
	private String		name;
	private StatusEnum	status;
	private String		message;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public StatusEnum getStatus() {
		return status;
	}

	public void setStatus(StatusEnum status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
