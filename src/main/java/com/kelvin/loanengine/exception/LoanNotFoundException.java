package com.kelvin.loanengine.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class LoanNotFoundException extends RuntimeException {

	public LoanNotFoundException(Long loanId) {
		super("Loan not found: " + loanId);
	}
}
