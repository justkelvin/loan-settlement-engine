package com.kelvin.loanengine.service;

import com.kelvin.loanengine.dto.CreateLoanRequest;
import com.kelvin.loanengine.dto.LoanResponse;
import com.kelvin.loanengine.dto.LoanScheduleResponse;
import com.kelvin.loanengine.entity.Loan;
import com.kelvin.loanengine.entity.LoanSchedule;
import com.kelvin.loanengine.entity.LoanStatus;
import com.kelvin.loanengine.exception.LoanNotFoundException;
import com.kelvin.loanengine.repository.LoanRepository;
import com.kelvin.loanengine.repository.LoanScheduleRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanService {

	private final LoanRepository loanRepository;
	private final LoanScheduleRepository loanScheduleRepository;
	private final FinancialMathService financialMathService;
	private final ScheduleGenerationService scheduleGenerationService;

	public LoanService(
			LoanRepository loanRepository,
			LoanScheduleRepository loanScheduleRepository,
			FinancialMathService financialMathService,
			ScheduleGenerationService scheduleGenerationService) {
		this.loanRepository = loanRepository;
		this.loanScheduleRepository = loanScheduleRepository;
		this.financialMathService = financialMathService;
		this.scheduleGenerationService = scheduleGenerationService;
	}

	@Transactional
	public LoanResponse createLoan(CreateLoanRequest request) {
		BigDecimal principalAmount = financialMathService.roundMoney(request.principalAmount());
		BigDecimal monthlyEmi = financialMathService.calculateEmi(
				principalAmount,
				request.annualInterestRate(),
				request.tenorMonths());

		Loan loan = new Loan();
		loan.setPrincipalAmount(principalAmount);
		loan.setAnnualInterestRate(request.annualInterestRate());
		loan.setTenorMonths(request.tenorMonths());
		loan.setMonthlyEmi(monthlyEmi);
		loan.setStatus(LoanStatus.ACTIVE);
		loan.setStartDate(request.startDate());

		List<LoanSchedule> schedules = scheduleGenerationService.generateSchedule(
				principalAmount,
				request.annualInterestRate(),
				request.tenorMonths(),
				monthlyEmi,
				request.startDate());
		schedules.forEach(loan::addSchedule);

		return toLoanResponse(loanRepository.save(loan));
	}

	@Transactional(readOnly = true)
	public LoanResponse getLoan(Long loanId) {
		return loanRepository.findById(loanId)
				.map(this::toLoanResponse)
				.orElseThrow(() -> new LoanNotFoundException(loanId));
	}

	@Transactional(readOnly = true)
	public List<LoanScheduleResponse> getSchedule(Long loanId) {
		if (!loanRepository.existsById(loanId)) {
			throw new LoanNotFoundException(loanId);
		}

		return loanScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId)
				.stream()
				.map(this::toScheduleResponse)
				.toList();
	}

	private LoanResponse toLoanResponse(Loan loan) {
		return new LoanResponse(
				loan.getId(),
				loan.getPrincipalAmount(),
				loan.getAnnualInterestRate(),
				loan.getTenorMonths(),
				loan.getMonthlyEmi(),
				loan.getStatus(),
				loan.getStartDate());
	}

	private LoanScheduleResponse toScheduleResponse(LoanSchedule schedule) {
		return new LoanScheduleResponse(
				schedule.getId(),
				schedule.getLoan().getId(),
				schedule.getInstallmentNumber(),
				schedule.getDueDate(),
				schedule.getOpeningBalance(),
				schedule.getEmiAmount(),
				schedule.getInterestComponent(),
				schedule.getPrincipalComponent(),
				schedule.getClosingBalance(),
				schedule.getStatus());
	}
}
