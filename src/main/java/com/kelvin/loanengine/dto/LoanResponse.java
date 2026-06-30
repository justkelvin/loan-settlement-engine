package com.kelvin.loanengine.dto;

import com.kelvin.loanengine.entity.LoanStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanResponse(
		Long loanId,
		BigDecimal principalAmount,
		BigDecimal annualInterestRate,
		Integer tenorMonths,
		BigDecimal monthlyEmi,
		LoanStatus status,
		LocalDate startDate) {
}
