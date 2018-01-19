package com.itemis.p2m.backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Invalid query parameters.")
public class InvalidInputException extends RuntimeException {
	private static final long serialVersionUID = 880181876216586047L;
}
