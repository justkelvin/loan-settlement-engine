package com.kelvin.loanengine.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateLoanRequest(
		@NotNull
		@DecimalMin(value = "0.00", inclusive = false)
		BigDecimal principalAmount,

		@NotNull
		@DecimalMin(value = "0.00", inclusive = false)
		BigDecimal annualInterestRate,

		@NotNull
		@Min(1)
		Integer tenorMonths,

		@NotNull
		LocalDate startDate) {
}
