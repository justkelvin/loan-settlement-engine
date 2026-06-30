package com.kelvin.loanengine.service;

import com.kelvin.loanengine.dto.PrepaymentRequest;
import com.kelvin.loanengine.dto.PrepaymentResponse;
import com.kelvin.loanengine.entity.Loan;
import com.kelvin.loanengine.entity.LoanSchedule;
import com.kelvin.loanengine.entity.LoanStatus;
import com.kelvin.loanengine.entity.LoanTransaction;
import com.kelvin.loanengine.entity.ScheduleStatus;
import com.kelvin.loanengine.entity.TransactionType;
import com.kelvin.loanengine.exception.InvalidInstallmentNumberException;
import com.kelvin.loanengine.exception.InvalidLoanStatusException;
import com.kelvin.loanengine.exception.InvalidPrepaymentAmountException;
import com.kelvin.loanengine.exception.LoanNotFoundException;
import com.kelvin.loanengine.repository.LoanRepository;
import com.kelvin.loanengine.repository.LoanScheduleRepository;
import com.kelvin.loanengine.repository.LoanTransactionRepository;
import com.kelvin.loanengine.strategy.PrepaymentCalculationInput;
import com.kelvin.loanengine.strategy.PrepaymentCalculationResult;
import com.kelvin.loanengine.strategy.PrepaymentStrategy;
import com.kelvin.loanengine.strategy.PrepaymentStrategyResolver;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrepaymentService {

	private final LoanRepository loanRepository;
	private final LoanScheduleRepository loanScheduleRepository;
	private final LoanTransactionRepository loanTransactionRepository;
	private final FinancialMathService financialMathService;
	private final ScheduleGenerationService scheduleGenerationService;
	private final PrepaymentStrategyResolver prepaymentStrategyResolver;

	public PrepaymentService(
			LoanRepository loanRepository,
			LoanScheduleRepository loanScheduleRepository,
			LoanTransactionRepository loanTransactionRepository,
			FinancialMathService financialMathService,
			ScheduleGenerationService scheduleGenerationService,
			PrepaymentStrategyResolver prepaymentStrategyResolver) {
		this.loanRepository = loanRepository;
		this.loanScheduleRepository = loanScheduleRepository;
		this.loanTransactionRepository = loanTransactionRepository;
		this.financialMathService = financialMathService;
		this.scheduleGenerationService = scheduleGenerationService;
		this.prepaymentStrategyResolver = prepaymentStrategyResolver;
	}

	@Transactional
	public PrepaymentResponse applyPrepayment(Long loanId, PrepaymentRequest request) {
		Loan loan = loanRepository.findById(loanId)
				.orElseThrow(() -> new LoanNotFoundException(loanId));
		validateLoanIsActive(loan);

		PrepaymentStrategy strategy = prepaymentStrategyResolver.resolve(request.strategy());
		BigDecimal prepaymentAmount = validateAndRoundAmount(request.amount());
		int afterInstallmentNumber = validateInstallmentNumber(request.afterInstallmentNumber(), loan);

		LoanSchedule paidThroughSchedule = loanScheduleRepository
				.findByLoanIdAndInstallmentNumber(loanId, afterInstallmentNumber)
				.orElseThrow(() -> new InvalidInstallmentNumberException(
						"Schedule row not found for installment: " + afterInstallmentNumber));

		BigDecimal balanceBefore = paidThroughSchedule.getClosingBalance();
		validateAmountIsLessThanOutstandingBalance(prepaymentAmount, balanceBefore);

		PrepaymentCalculationResult result = strategy.calculate(new PrepaymentCalculationInput(
				loan.getId(),
				loan.getAnnualInterestRate(),
				loan.getTenorMonths(),
				loan.getMonthlyEmi(),
				afterInstallmentNumber,
				prepaymentAmount,
				balanceBefore));

		updateLoanAndSchedule(loan, paidThroughSchedule, result);
		saveTransaction(loan, result);

		return toResponse(result);
	}

	private void validateLoanIsActive(Loan loan) {
		if (loan.getStatus() != LoanStatus.ACTIVE) {
			throw new InvalidLoanStatusException("Prepayment is only allowed for ACTIVE loans");
		}
	}

	private BigDecimal validateAndRoundAmount(BigDecimal amount) {
		BigDecimal roundedAmount = financialMathService.roundMoney(amount);
		if (roundedAmount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new InvalidPrepaymentAmountException("Prepayment amount must be greater than zero");
		}

		return roundedAmount;
	}

	private int validateInstallmentNumber(Integer afterInstallmentNumber, Loan loan) {
		if (afterInstallmentNumber == null
				|| afterInstallmentNumber < 1
				|| afterInstallmentNumber >= loan.getTenorMonths()) {
			throw new InvalidInstallmentNumberException(
					"afterInstallmentNumber must be between 1 and " + (loan.getTenorMonths() - 1));
		}

		return afterInstallmentNumber;
	}

	private void validateAmountIsLessThanOutstandingBalance(BigDecimal amount, BigDecimal balanceBefore) {
		if (amount.compareTo(balanceBefore) >= 0) {
			throw new InvalidPrepaymentAmountException(
					"Prepayment amount must be less than outstanding balance");
		}
	}

	private void updateLoanAndSchedule(
			Loan loan,
			LoanSchedule paidThroughSchedule,
			PrepaymentCalculationResult result) {
		List<LoanSchedule> persistedSchedule =
				loanScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loan.getId());
		Map<Integer, LoanSchedule> persistedScheduleByInstallment = persistedSchedule.stream()
				.collect(Collectors.toMap(LoanSchedule::getInstallmentNumber, Function.identity()));

		for (LoanSchedule schedule : persistedSchedule) {
			if (schedule.getInstallmentNumber() <= result.afterInstallmentNumber()) {
				schedule.setStatus(ScheduleStatus.PAID);
			}
		}

		List<LoanSchedule> recalculatedFutureSchedule = scheduleGenerationService.generateScheduleSegment(
				result.balanceAfter(),
				loan.getAnnualInterestRate(),
				result.remainingMonths(),
				result.newEmi(),
				paidThroughSchedule.getDueDate().plusMonths(1),
				result.afterInstallmentNumber() + 1,
				ScheduleStatus.ADJUSTED);

		for (LoanSchedule recalculatedSchedule : recalculatedFutureSchedule) {
			LoanSchedule persistedScheduleRow =
					persistedScheduleByInstallment.get(recalculatedSchedule.getInstallmentNumber());
			if (persistedScheduleRow == null) {
				throw new InvalidInstallmentNumberException(
						"Schedule row not found for installment: " + recalculatedSchedule.getInstallmentNumber());
			}
			copyScheduleValues(recalculatedSchedule, persistedScheduleRow);
		}

		loan.setMonthlyEmi(result.newEmi());
	}

	private void copyScheduleValues(LoanSchedule source, LoanSchedule target) {
		target.setDueDate(source.getDueDate());
		target.setOpeningBalance(source.getOpeningBalance());
		target.setEmiAmount(source.getEmiAmount());
		target.setInterestComponent(source.getInterestComponent());
		target.setPrincipalComponent(source.getPrincipalComponent());
		target.setClosingBalance(source.getClosingBalance());
		target.setStatus(source.getStatus());
	}

	private void saveTransaction(Loan loan, PrepaymentCalculationResult result) {
		LoanTransaction transaction = new LoanTransaction();
		transaction.setLoan(loan);
		transaction.setTransactionType(TransactionType.PREPAYMENT);
		transaction.setStrategyType(result.strategy());
		transaction.setInstallmentNumber(result.afterInstallmentNumber());
		transaction.setAmount(result.prepaymentAmount());
		transaction.setBalanceBefore(result.balanceBefore());
		transaction.setBalanceAfter(result.balanceAfter());
		transaction.setDescription("Partial prepayment applied after installment "
				+ result.afterInstallmentNumber() + " using " + result.strategy());

		loanTransactionRepository.save(transaction);
	}

	private PrepaymentResponse toResponse(PrepaymentCalculationResult result) {
		return new PrepaymentResponse(
				result.loanId(),
				result.strategy(),
				result.afterInstallmentNumber(),
				result.balanceBefore(),
				result.prepaymentAmount(),
				result.balanceAfter(),
				result.remainingMonths(),
				result.oldEmi(),
				result.newEmi());
	}
}
