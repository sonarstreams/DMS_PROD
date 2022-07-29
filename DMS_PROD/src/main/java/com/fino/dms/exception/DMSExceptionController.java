package com.fino.dms.exception;

import java.nio.file.AccessDeniedException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
public class DMSExceptionController {

	@ExceptionHandler(value = DMSException.class)
	public ResponseEntity<Object> exception(DMSException exception) {
		return new ResponseEntity<>("DMS Internal Exception", HttpStatus.NOT_FOUND);
	}

	
	@ExceptionHandler({ AccessDeniedException.class })
	public ResponseEntity<Object> handleAccessDeniedException(Exception ex, WebRequest request) {
		return new ResponseEntity<Object>("Access denied message here", new HttpHeaders(), HttpStatus.FORBIDDEN);
	}
}
	