package com.kelvin.loanengine.exception;

public class InvalidInstallmentNumberException extends BadRequestException {

	public InvalidInstallmentNumberException(String message) {
		super(message);
	}
}
