package com.kelvin.loanengine.repository;

import com.kelvin.loanengine.entity.LoanTransaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanTransactionRepository extends JpaRepository<LoanTransaction, Long> {

	List<LoanTransaction> findByLoanIdOrderByCreatedAtAsc(Long loanId);
}
