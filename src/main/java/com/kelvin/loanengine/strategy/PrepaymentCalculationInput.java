package com.kelvin.loanengine.strategy;

import java.math.BigDecimal;

public record PrepaymentCalculationInput(
		Long loanId,
		BigDecimal annualInterestRate,
		Integer tenorMonths,
		BigDecimal oldEmi,
		Integer afterInstallmentNumber,
		BigDecimal prepaymentAmount,
		BigDecimal balanceBefore) {
}
