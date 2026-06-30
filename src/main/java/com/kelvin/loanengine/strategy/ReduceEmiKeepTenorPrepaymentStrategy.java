package com.kelvin.loanengine.strategy;

import com.kelvin.loanengine.entity.PrepaymentStrategyType;
import com.kelvin.loanengine.service.FinancialMathService;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class ReduceEmiKeepTenorPrepaymentStrategy implements PrepaymentStrategy {

	private final FinancialMathService financialMathService;

	public ReduceEmiKeepTenorPrepaymentStrategy(FinancialMathService financialMathService) {
		this.financialMathService = financialMathService;
	}

	@Override
	public PrepaymentStrategyType getStrategyType() {
		return PrepaymentStrategyType.REDUCE_EMI_KEEP_TENOR;
	}

	@Override
	public PrepaymentCalculationResult calculate(PrepaymentCalculationInput input) {
		BigDecimal balanceAfter = financialMathService.roundMoney(
				input.balanceBefore().subtract(input.prepaymentAmount()));
		int remainingMonths = input.tenorMonths() - input.afterInstallmentNumber();
		BigDecimal newEmi = financialMathService.calculateEmi(
				balanceAfter,
				input.annualInterestRate(),
				remainingMonths);

		return new PrepaymentCalculationResult(
				input.loanId(),
				getStrategyType(),
				input.afterInstallmentNumber(),
				input.balanceBefore(),
				input.prepaymentAmount(),
				balanceAfter,
				remainingMonths,
				input.oldEmi(),
				newEmi);
	}
}
