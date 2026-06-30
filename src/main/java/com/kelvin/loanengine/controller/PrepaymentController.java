package com.kelvin.loanengine.controller;

import com.kelvin.loanengine.dto.PrepaymentRequest;
import com.kelvin.loanengine.dto.PrepaymentResponse;
import com.kelvin.loanengine.service.PrepaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loans/{loanId}/prepayments")
public class PrepaymentController {

	private final PrepaymentService prepaymentService;

	public PrepaymentController(PrepaymentService prepaymentService) {
		this.prepaymentService = prepaymentService;
	}

	@PostMapping
	public PrepaymentResponse applyPrepayment(
			@PathVariable Long loanId,
			@Valid @RequestBody PrepaymentRequest request) {
		return prepaymentService.applyPrepayment(loanId, request);
	}
}
