package com.kelvin.loanengine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
		name = "loan_schedules",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_loan_schedules_loan_installment",
				columnNames = {"loan_id", "installment_number"}))
public class LoanSchedule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "loan_id", nullable = false)
	private Loan loan;

	@Column(name = "installment_number", nullable = false)
	private Integer installmentNumber;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	@Column(name = "opening_balance", nullable = false, precision = 19, scale = 2)
	private BigDecimal openingBalance;

	@Column(name = "emi_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal emiAmount;

	@Column(name = "interest_component", nullable = false, precision = 19, scale = 2)
	private BigDecimal interestComponent;

	@Column(name = "principal_component", nullable = false, precision = 19, scale = 2)
	private BigDecimal principalComponent;

	@Column(name = "closing_balance", nullable = false, precision = 19, scale = 2)
	private BigDecimal closingBalance;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ScheduleStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
