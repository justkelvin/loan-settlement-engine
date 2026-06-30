package com.kelvin.loanengine.exception;

public class UnsupportedPrepaymentStrategyException extends BadRequestException {

	public UnsupportedPrepaymentStrategyException(String strategy) {
		super("Unsupported prepayment strategy: " + strategy);
	}
}
