package com.fino.dms.exception;

public class DMSException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String message;
	
	public DMSException() {
		
	}

	public DMSException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

	public DMSException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public DMSException(String message) {
		super(message);
	
	}

	public DMSException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}
	
	
	

}
