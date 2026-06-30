package com.kelvin.loanengine.service;

import com.kelvin.loanengine.entity.LoanSchedule;
import com.kelvin.loanengine.entity.ScheduleStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ScheduleGenerationService {

	private final FinancialMathService financialMathService;

	public ScheduleGenerationService(FinancialMathService financialMathService) {
		this.financialMathService = financialMathService;
	}

	public List<LoanSchedule> generateSchedule(
			BigDecimal principalAmount,
			BigDecimal annualInterestRate,
			int tenorMonths,
			BigDecimal monthlyEmi,
			LocalDate startDate) {
		validateInputs(principalAmount, annualInterestRate, tenorMonths, monthlyEmi, startDate);

		return generateScheduleRows(
				principalAmount,
				annualInterestRate,
				tenorMonths,
				monthlyEmi,
				startDate,
				1,
				ScheduleStatus.PENDING);
	}

	public List<LoanSchedule> generateScheduleSegment(
			BigDecimal openingPrincipal,
			BigDecimal annualInterestRate,
			int installmentCount,
			BigDecimal monthlyEmi,
			LocalDate firstDueDate,
			int firstInstallmentNumber,
			ScheduleStatus status) {
		validateInputs(openingPrincipal, annualInterestRate, installmentCount, monthlyEmi, firstDueDate);
		Objects.requireNonNull(status, "status must not be null");
		if (firstInstallmentNumber <= 0) {
			throw new IllegalArgumentException("firstInstallmentNumber must be greater than zero");
		}

		return generateScheduleRows(
				openingPrincipal,
				annualInterestRate,
				installmentCount,
				monthlyEmi,
				firstDueDate,
				firstInstallmentNumber,
				status);
	}

	private List<LoanSchedule> generateScheduleRows(
			BigDecimal principalAmount,
			BigDecimal annualInterestRate,
			int installmentCount,
			BigDecimal monthlyEmi,
			LocalDate firstDueDate,
			int firstInstallmentNumber,
			ScheduleStatus status) {
		List<LoanSchedule> schedules = new ArrayList<>(installmentCount);
		BigDecimal openingBalance = principalAmount;
		BigDecimal exactMonthlyEmi = financialMathService.calculateExactEmi(
				principalAmount,
				annualInterestRate,
				installmentCount);
		BigDecimal monthlyRate = financialMathService.calculateMonthlyRate(annualInterestRate);

		for (int offset = 0; offset < installmentCount; offset++) {
			int installmentNumber = firstInstallmentNumber + offset;
			BigDecimal exactInterestComponent = openingBalance.multiply(monthlyRate);
			BigDecimal exactPrincipalComponent = exactMonthlyEmi.subtract(exactInterestComponent);

			if (offset == installmentCount - 1 || exactPrincipalComponent.compareTo(openingBalance) > 0) {
				exactPrincipalComponent = openingBalance;
			}

			BigDecimal closingBalance = openingBalance.subtract(exactPrincipalComponent);

			LoanSchedule schedule = new LoanSchedule();
			schedule.setInstallmentNumber(installmentNumber);
			schedule.setDueDate(firstDueDate.plusMonths(offset));
			schedule.setOpeningBalance(financialMathService.roundMoney(openingBalance));
			schedule.setEmiAmount(monthlyEmi);
			schedule.setInterestComponent(financialMathService.roundMoney(exactInterestComponent));
			schedule.setPrincipalComponent(financialMathService.roundMoney(exactPrincipalComponent));
			schedule.setClosingBalance(financialMathService.roundMoney(closingBalance));
			schedule.setStatus(status);
			schedules.add(schedule);

			openingBalance = closingBalance;
		}

		return schedules;
	}

	private void validateInputs(
			BigDecimal principalAmount,
			BigDecimal annualInterestRate,
			int tenorMonths,
			BigDecimal monthlyEmi,
			LocalDate startDate) {
		Objects.requireNonNull(principalAmount, "principalAmount must not be null");
		Objects.requireNonNull(annualInterestRate, "annualInterestRate must not be null");
		Objects.requireNonNull(monthlyEmi, "monthlyEmi must not be null");
		Objects.requireNonNull(startDate, "startDate must not be null");

		if (principalAmount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("principalAmount must be greater than zero");
		}
		if (annualInterestRate.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("annualInterestRate must not be negative");
		}
		if (tenorMonths <= 0) {
			throw new IllegalArgumentException("tenorMonths must be greater than zero");
		}
		if (monthlyEmi.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("monthlyEmi must be greater than zero");
		}
	}
}
