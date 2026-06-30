package com.kelvin.loanengine.exception;

public class LoanNotFoundException extends RuntimeException {

	public LoanNotFoundException(Long loanId) {
		super("Loan not found: " + loanId);
	}
}
