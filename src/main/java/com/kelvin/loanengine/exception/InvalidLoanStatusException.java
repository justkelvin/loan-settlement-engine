package com.kelvin.loanengine.exception;

public class InvalidLoanStatusException extends BadRequestException {

	public InvalidLoanStatusException(String message) {
		super(message);
	}
}
