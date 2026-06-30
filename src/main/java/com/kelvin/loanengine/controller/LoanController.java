package com.kelvin.loanengine.controller;

import com.kelvin.loanengine.dto.CreateLoanRequest;
import com.kelvin.loanengine.dto.LoanResponse;
import com.kelvin.loanengine.dto.LoanScheduleResponse;
import com.kelvin.loanengine.service.LoanService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

	private final LoanService loanService;

	public LoanController(LoanService loanService) {
		this.loanService = loanService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public LoanResponse createLoan(@Valid @RequestBody CreateLoanRequest request) {
		return loanService.createLoan(request);
	}

	@GetMapping("/{loanId}")
	public LoanResponse getLoan(@PathVariable Long loanId) {
		return loanService.getLoan(loanId);
	}

	@GetMapping("/{loanId}/schedule")
	public List<LoanScheduleResponse> getSchedule(@PathVariable Long loanId) {
		return loanService.getSchedule(loanId);
	}
}
