package com.kelvin.loanengine.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class FinancialMathService {

	public static final int MONEY_SCALE = 2;

	private static final int CALCULATION_SCALE = 20;
	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
	private static final BigDecimal MONTHS_IN_YEAR = new BigDecimal("12");
	private static final MathContext POW_CONTEXT = MathContext.DECIMAL128;

	public BigDecimal calculateMonthlyRate(BigDecimal annualInterestRate) {
		Objects.requireNonNull(annualInterestRate, "annualInterestRate must not be null");
		if (annualInterestRate.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("annualInterestRate must not be negative");
		}

		return annualInterestRate
				.divide(ONE_HUNDRED, CALCULATION_SCALE, RoundingMode.HALF_UP)
				.divide(MONTHS_IN_YEAR, CALCULATION_SCALE, RoundingMode.HALF_UP);
	}

	public BigDecimal calculateEmi(BigDecimal principalAmount, BigDecimal annualInterestRate, int tenorMonths) {
		return roundMoney(calculateExactEmi(principalAmount, annualInterestRate, tenorMonths));
	}

	public BigDecimal calculateExactEmi(BigDecimal principalAmount, BigDecimal annualInterestRate, int tenorMonths) {
		Objects.requireNonNull(principalAmount, "principalAmount must not be null");
		if (principalAmount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("principalAmount must be greater than zero");
		}
		if (tenorMonths <= 0) {
			throw new IllegalArgumentException("tenorMonths must be greater than zero");
		}

		BigDecimal monthlyRate = calculateMonthlyRate(annualInterestRate);
		if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
			return principalAmount.divide(BigDecimal.valueOf(tenorMonths), CALCULATION_SCALE, RoundingMode.HALF_UP);
		}

		BigDecimal rateFactor = BigDecimal.ONE.add(monthlyRate).pow(tenorMonths, POW_CONTEXT);
		BigDecimal numerator = principalAmount.multiply(monthlyRate).multiply(rateFactor);
		BigDecimal denominator = rateFactor.subtract(BigDecimal.ONE);

		return numerator.divide(denominator, CALCULATION_SCALE, RoundingMode.HALF_UP);
	}

	public BigDecimal calculateMonthlyInterest(BigDecimal openingBalance, BigDecimal annualInterestRate) {
		Objects.requireNonNull(openingBalance, "openingBalance must not be null");
		if (openingBalance.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("openingBalance must not be negative");
		}

		return roundMoney(openingBalance.multiply(calculateMonthlyRate(annualInterestRate)));
	}

	public BigDecimal roundMoney(BigDecimal amount) {
		Objects.requireNonNull(amount, "amount must not be null");
		return amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}
}
