package com.itemis.p2m.backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, reason = "A target platform with the given installable units cannot be constructed from the given repositories.")
public class TargetPlatformException extends RuntimeException {

	private static final long serialVersionUID = 1722047179151675601L;
	
	public TargetPlatformException(String message) {
		super(message);
	}
}