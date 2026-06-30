package com.kelvin.loanengine.exception;

public class InvalidPrepaymentAmountException extends BadRequestException {

	public InvalidPrepaymentAmountException(String message) {
		super(message);
	}
}
