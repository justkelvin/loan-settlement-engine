package com.kelvin.loanengine;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kelvin.loanengine.repository.LoanRepository;
import com.kelvin.loanengine.repository.LoanScheduleRepository;
import com.kelvin.loanengine.repository.LoanTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ExceptionHandlingIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private LoanRepository loanRepository;

	@Autowired
	private LoanScheduleRepository loanScheduleRepository;

	@Autowired
	private LoanTransactionRepository loanTransactionRepository;

	@BeforeEach
	void cleanDatabase() {
		loanTransactionRepository.deleteAll();
		loanScheduleRepository.deleteAll();
		loanRepository.deleteAll();
	}

	@Test
	void validationFailureReturnsCleanJsonError() throws Exception {
		String request = """
				{
				  "principalAmount": 0,
				  "annualInterestRate": 12,
				  "tenorMonths": 60,
				  "startDate": "2024-07-24"
				}
				""";

		mockMvc.perform(post("/api/loans")
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.message").value("Request validation failed"))
				.andExpect(jsonPath("$.path").value("/api/loans"))
				.andExpect(jsonPath("$.fieldErrors.principalAmount").value("must be greater than 0.00"))
				.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void malformedJsonReturnsCleanJsonError() throws Exception {
		mockMvc.perform(post("/api/loans")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"principalAmount\":"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.message").value("Malformed JSON request"))
				.andExpect(jsonPath("$.path").value("/api/loans"))
				.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void unknownLoanReturnsCleanJsonError() throws Exception {
		mockMvc.perform(get("/api/loans/{loanId}", 999999L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.error").value("Not Found"))
				.andExpect(jsonPath("$.message").value("Loan not found: 999999"))
				.andExpect(jsonPath("$.path").value("/api/loans/999999"))
				.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void invalidPathVariableReturnsCleanJsonError() throws Exception {
		mockMvc.perform(get("/api/loans/not-a-number"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.path").value("/api/loans/not-a-number"))
				.andExpect(jsonPath("$.timestamp").exists());
	}
}
