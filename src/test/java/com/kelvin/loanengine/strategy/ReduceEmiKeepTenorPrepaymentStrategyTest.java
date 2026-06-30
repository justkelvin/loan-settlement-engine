package com.kelvin.loanengine.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import com.kelvin.loanengine.entity.PrepaymentStrategyType;
import com.kelvin.loanengine.service.FinancialMathService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ReduceEmiKeepTenorPrepaymentStrategyTest {

	private final ReduceEmiKeepTenorPrepaymentStrategy strategy =
			new ReduceEmiKeepTenorPrepaymentStrategy(new FinancialMathService());

	@Test
	void calculatesReducedEmiWhileKeepingRemainingTenorFixed() {
		PrepaymentCalculationResult result = strategy.calculate(new PrepaymentCalculationInput(
				1L,
				new BigDecimal("12"),
				60,
				new BigDecimal("22244.45"),
				24,
				new BigDecimal("200000.00"),
				new BigDecimal("669724.82")));

		assertThat(result.loanId()).isEqualTo(1L);
		assertThat(result.strategy()).isEqualTo(PrepaymentStrategyType.REDUCE_EMI_KEEP_TENOR);
		assertThat(result.afterInstallmentNumber()).isEqualTo(24);
		assertThat(result.balanceBefore()).isEqualByComparingTo(new BigDecimal("669724.82"));
		assertThat(result.prepaymentAmount()).isEqualByComparingTo(new BigDecimal("200000.00"));
		assertThat(result.balanceAfter()).isEqualByComparingTo(new BigDecimal("469724.82"));
		assertThat(result.remainingMonths()).isEqualTo(36);
		assertThat(result.oldEmi()).isEqualByComparingTo(new BigDecimal("22244.45"));
		assertThat(result.newEmi()).isEqualByComparingTo(new BigDecimal("15601.59"));
	}
}
