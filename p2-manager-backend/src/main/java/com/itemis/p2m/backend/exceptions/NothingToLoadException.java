package com.itemis.p2m.backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NO_CONTENT, reason = "No additional resources available.")
public class NothingToLoadException extends RuntimeException {
	private static final long serialVersionUID = 2208091003546854116L;

}
