package com.kelvin.loanengine.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PrepaymentRequest(
		@NotNull
		@Min(1)
		Integer afterInstallmentNumber,

		@NotNull
		@DecimalMin(value = "0.00", inclusive = false)
		BigDecimal amount,

		@NotBlank
		String strategy) {
}
