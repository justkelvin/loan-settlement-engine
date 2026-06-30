package com.kelvin.loanengine.dto;

import com.kelvin.loanengine.entity.ScheduleStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanScheduleResponse(
		Long id,
		Long loanId,
		Integer installmentNumber,
		LocalDate dueDate,
		BigDecimal openingBalance,
		BigDecimal emiAmount,
		BigDecimal interestComponent,
		BigDecimal principalComponent,
		BigDecimal closingBalance,
		ScheduleStatus status) {
}
