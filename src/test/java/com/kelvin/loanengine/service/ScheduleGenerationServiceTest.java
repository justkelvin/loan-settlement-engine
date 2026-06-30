package com.kelvin.loanengine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kelvin.loanengine.entity.LoanSchedule;
import com.kelvin.loanengine.entity.ScheduleStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

class ScheduleGenerationServiceTest {

	private final FinancialMathService financialMathService = new FinancialMathService();
	private final ScheduleGenerationService scheduleGenerationService =
			new ScheduleGenerationService(financialMathService);

	@Test
	void generateScheduleCreatesSixtyRowsForBaseLoan() {
		List<LoanSchedule> schedule = generateBaseSchedule();

		assertThat(schedule).hasSize(60);
		assertThat(schedule)
				.extracting(LoanSchedule::getInstallmentNumber)
				.containsExactlyElementsOf(IntStream.rangeClosed(1, 60).boxed().toList());
	}

	@Test
	void generateScheduleMatchesFirstInstallmentCsvValues() {
		LoanSchedule firstInstallment = generateBaseSchedule().get(0);

		assertThat(firstInstallment.getDueDate()).isEqualTo(LocalDate.of(2024, 7, 24));
		assertThat(firstInstallment.getOpeningBalance()).isEqualByComparingTo(new BigDecimal("1000000.00"));
		assertThat(firstInstallment.getEmiAmount()).isEqualByComparingTo(new BigDecimal("22244.45"));
		assertThat(firstInstallment.getInterestComponent()).isEqualByComparingTo(new BigDecimal("10000.00"));
		assertThat(firstInstallment.getPrincipalComponent()).isEqualByComparingTo(new BigDecimal("12244.45"));
		assertThat(firstInstallment.getClosingBalance()).isEqualByComparingTo(new BigDecimal("987755.55"));
		assertThat(firstInstallment.getStatus()).isEqualTo(ScheduleStatus.PENDING);
	}

	@Test
	void generateScheduleMatchesTwentyFourthInstallmentCsvValues() {
		LoanSchedule installment = generateBaseSchedule().get(23);

		assertThat(installment.getDueDate()).isEqualTo(LocalDate.of(2026, 6, 24));
		assertThat(installment.getInterestComponent()).isEqualByComparingTo(new BigDecimal("6851.18"));
		assertThat(installment.getPrincipalComponent()).isEqualByComparingTo(new BigDecimal("15393.27"));
		assertThat(installment.getClosingBalance()).isEqualByComparingTo(new BigDecimal("669724.82"));
	}

	@Test
	void generateScheduleMatchesTotalInterestAndClosesBalance() {
		List<LoanSchedule> schedule = generateBaseSchedule();

		BigDecimal totalInterest = schedule.stream()
				.map(LoanSchedule::getInterestComponent)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		LoanSchedule finalInstallment = schedule.get(59);

		assertThat(totalInterest).isCloseTo(new BigDecimal("334666.86"), Offset.offset(new BigDecimal("0.05")));
		assertThat(finalInstallment.getPrincipalComponent()).isEqualByComparingTo(new BigDecimal("22024.21"));
		assertThat(finalInstallment.getInterestComponent()).isEqualByComparingTo(new BigDecimal("220.24"));
		assertThat(finalInstallment.getClosingBalance()).isEqualByComparingTo(new BigDecimal("0.00"));
	}

	@Test
	void generateScheduleKeepsMonthlyDueDatesFromStartDate() {
		List<LoanSchedule> schedule = generateBaseSchedule();

		assertThat(schedule.get(1).getDueDate()).isEqualTo(LocalDate.of(2024, 8, 24));
		assertThat(schedule.get(35).getDueDate()).isEqualTo(LocalDate.of(2027, 6, 24));
		assertThat(schedule.get(59).getDueDate()).isEqualTo(LocalDate.of(2029, 6, 24));
	}

	@Test
	void generateScheduleSegmentSupportsRecalculatedFutureInstallments() {
		List<LoanSchedule> schedule = scheduleGenerationService.generateScheduleSegment(
				new BigDecimal("469724.82"),
				new BigDecimal("12"),
				36,
				new BigDecimal("15601.57"),
				LocalDate.of(2026, 7, 24),
				25,
				ScheduleStatus.ADJUSTED);

		assertThat(schedule).hasSize(36);

		LoanSchedule firstAdjusted = schedule.get(0);
		assertThat(firstAdjusted.getInstallmentNumber()).isEqualTo(25);
		assertThat(firstAdjusted.getDueDate()).isEqualTo(LocalDate.of(2026, 7, 24));
		assertThat(firstAdjusted.getOpeningBalance()).isEqualByComparingTo(new BigDecimal("469724.82"));
		assertThat(firstAdjusted.getEmiAmount()).isEqualByComparingTo(new BigDecimal("15601.57"));
		assertThat(firstAdjusted.getInterestComponent()).isEqualByComparingTo(new BigDecimal("4697.25"));
		assertThat(firstAdjusted.getPrincipalComponent()).isEqualByComparingTo(new BigDecimal("10904.34"));
		assertThat(firstAdjusted.getClosingBalance()).isEqualByComparingTo(new BigDecimal("458820.48"));
		assertThat(firstAdjusted.getStatus()).isEqualTo(ScheduleStatus.ADJUSTED);

		LoanSchedule finalAdjusted = schedule.get(35);
		assertThat(finalAdjusted.getInstallmentNumber()).isEqualTo(60);
		assertThat(finalAdjusted.getDueDate()).isEqualTo(LocalDate.of(2029, 6, 24));
		assertThat(finalAdjusted.getClosingBalance()).isEqualByComparingTo(new BigDecimal("0.00"));
		assertThat(finalAdjusted.getStatus()).isEqualTo(ScheduleStatus.ADJUSTED);
	}

	@Test
	void generateScheduleRejectsInvalidInputs() {
		assertThatThrownBy(() -> scheduleGenerationService.generateSchedule(
				BigDecimal.ZERO,
				new BigDecimal("12"),
				60,
				new BigDecimal("22244.45"),
				LocalDate.of(2024, 7, 24)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("principalAmount must be greater than zero");

		assertThatThrownBy(() -> scheduleGenerationService.generateSchedule(
				new BigDecimal("1000000.00"),
				new BigDecimal("12"),
				0,
				new BigDecimal("22244.45"),
				LocalDate.of(2024, 7, 24)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("tenorMonths must be greater than zero");

		assertThatThrownBy(() -> scheduleGenerationService.generateSchedule(
				new BigDecimal("1000000.00"),
				new BigDecimal("12"),
				60,
				BigDecimal.ZERO,
				LocalDate.of(2024, 7, 24)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("monthlyEmi must be greater than zero");
	}

	private List<LoanSchedule> generateBaseSchedule() {
		BigDecimal principal = new BigDecimal("1000000.00");
		BigDecimal annualInterestRate = new BigDecimal("12");
		int tenorMonths = 60;
		BigDecimal monthlyEmi = financialMathService.calculateEmi(principal, annualInterestRate, tenorMonths);

		return scheduleGenerationService.generateSchedule(
				principal,
				annualInterestRate,
				tenorMonths,
				monthlyEmi,
				LocalDate.of(2024, 7, 24));
	}
}
