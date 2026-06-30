package com.kelvin.loanengine.strategy;

import com.kelvin.loanengine.entity.PrepaymentStrategyType;
import java.math.BigDecimal;

public record PrepaymentCalculationResult(
		Long loanId,
		PrepaymentStrategyType strategy,
		Integer afterInstallmentNumber,
		BigDecimal balanceBefore,
		BigDecimal prepaymentAmount,
		BigDecimal balanceAfter,
		Integer remainingMonths,
		BigDecimal oldEmi,
		BigDecimal newEmi) {
}
