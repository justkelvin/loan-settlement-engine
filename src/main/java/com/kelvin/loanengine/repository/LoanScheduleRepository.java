package com.kelvin.loanengine.repository;

import com.kelvin.loanengine.entity.LoanSchedule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanScheduleRepository extends JpaRepository<LoanSchedule, Long> {

	List<LoanSchedule> findByLoanIdOrderByInstallmentNumberAsc(Long loanId);

	Optional<LoanSchedule> findByLoanIdAndInstallmentNumber(Long loanId, Integer installmentNumber);
}
