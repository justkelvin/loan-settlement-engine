package com.kelvin.loanengine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FinancialMathServiceTest {

	private final FinancialMathService financialMathService = new FinancialMathService();

	@Test
	void calculateEmiForBaseLoanMatchesReferenceValue() {
		BigDecimal emi = financialMathService.calculateEmi(
				new BigDecimal("1000000.00"),
				new BigDecimal("12"),
				60);

		assertThat(emi).isEqualByComparingTo(new BigDecimal("22244.45"));
		assertThat(emi.scale()).isEqualTo(FinancialMathService.MONEY_SCALE);
	}

	@Test
	void calculateExactEmiKeepsInternalPrecisionForScheduleGeneration() {
		BigDecimal exactEmi = financialMathService.calculateExactEmi(
				new BigDecimal("1000000.00"),
				new BigDecimal("12"),
				60);

		assertThat(exactEmi).isEqualByComparingTo(new BigDecimal("22244.44768490177764962671"));
		assertThat(financialMathService.roundMoney(exactEmi)).isEqualByComparingTo(new BigDecimal("22244.45"));
	}

	@Test
	void calculateMonthlyRateConvertsAnnualPercentageToMonthlyDecimalRate() {
		BigDecimal monthlyRate = financialMathService.calculateMonthlyRate(new BigDecimal("12"));

		assertThat(monthlyRate).isEqualByComparingTo(new BigDecimal("0.01"));
	}

	@Test
	void calculateMonthlyInterestRoundsToMoneyScale() {
		BigDecimal interest = financialMathService.calculateMonthlyInterest(
				new BigDecimal("1000000.00"),
				new BigDecimal("12"));

		assertThat(interest).isEqualByComparingTo(new BigDecimal("10000.00"));
		assertThat(interest.scale()).isEqualTo(FinancialMathService.MONEY_SCALE);
	}

	@Test
	void roundMoneyUsesHalfUpAtTwoDecimalPlaces() {
		assertThat(financialMathService.roundMoney(new BigDecimal("12.345")))
				.isEqualByComparingTo(new BigDecimal("12.35"));
		assertThat(financialMathService.roundMoney(new BigDecimal("12.344")))
				.isEqualByComparingTo(new BigDecimal("12.34"));
	}

	@Test
	void calculateEmiHandlesZeroInterestRate() {
		BigDecimal emi = financialMathService.calculateEmi(
				new BigDecimal("1200.00"),
				BigDecimal.ZERO,
				12);

		assertThat(emi).isEqualByComparingTo(new BigDecimal("100.00"));
	}

	@Test
	void calculateEmiRejectsInvalidInputs() {
		assertThatThrownBy(() -> financialMathService.calculateEmi(BigDecimal.ZERO, new BigDecimal("12"), 60))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("principalAmount must be greater than zero");

		assertThatThrownBy(() -> financialMathService.calculateEmi(new BigDecimal("1000.00"), new BigDecimal("12"), 0))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("tenorMonths must be greater than zero");

		assertThatThrownBy(() -> financialMathService.calculateEmi(new BigDecimal("1000.00"), new BigDecimal("-1"), 12))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("annualInterestRate must not be negative");
	}
}
