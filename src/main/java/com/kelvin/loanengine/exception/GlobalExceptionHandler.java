package com.kelvin.loanengine.exception;

import com.kelvin.loanengine.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(LoanNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleLoanNotFound(
			LoanNotFoundException exception,
			HttpServletRequest request) {
		return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request.getRequestURI(), Map.of());
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ErrorResponse> handleBadRequest(
			BadRequestException exception,
			HttpServletRequest request) {
		return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI(), Map.of());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationFailure(
			MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
			fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
		}

		return buildResponse(
				HttpStatus.BAD_REQUEST,
				"Request validation failed",
				request.getRequestURI(),
				fieldErrors);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleUnreadableMessage(
			HttpMessageNotReadableException exception,
			HttpServletRequest request) {
		return buildResponse(HttpStatus.BAD_REQUEST, "Malformed JSON request", request.getRequestURI(), Map.of());
	}

	@ExceptionHandler({
			ConstraintViolationException.class,
			MethodArgumentTypeMismatchException.class,
			IllegalArgumentException.class
	})
	public ResponseEntity<ErrorResponse> handleInvalidRequest(
			Exception exception,
			HttpServletRequest request) {
		return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI(), Map.of());
	}

	private ResponseEntity<ErrorResponse> buildResponse(
			HttpStatus status,
			String message,
			String path,
			Map<String, String> fieldErrors) {
		return ResponseEntity
				.status(status)
				.body(new ErrorResponse(
						Instant.now(),
						status.value(),
						status.getReasonPhrase(),
						message,
						path,
						fieldErrors));
	}
}
