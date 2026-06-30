package com.kelvin.loanengine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kelvin.loanengine.entity.Loan;
import com.kelvin.loanengine.entity.LoanSchedule;
import com.kelvin.loanengine.entity.LoanStatus;
import com.kelvin.loanengine.entity.LoanTransaction;
import com.kelvin.loanengine.entity.PrepaymentStrategyType;
import com.kelvin.loanengine.entity.ScheduleStatus;
import com.kelvin.loanengine.entity.TransactionType;
import com.kelvin.loanengine.repository.LoanRepository;
import com.kelvin.loanengine.repository.LoanScheduleRepository;
import com.kelvin.loanengine.repository.LoanTransactionRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PrepaymentApiIntegrationTest {

	private static final String BASE_LOAN_REQUEST = """
			{
			  "principalAmount": 1000000,
			  "annualInterestRate": 12,
			  "tenorMonths": 60,
			  "startDate": "2024-07-24"
			}
			""";

	private static final String VALID_PREPAYMENT_REQUEST = """
			{
			  "afterInstallmentNumber": 24,
			  "amount": 200000,
			  "strategy": "REDUCE_EMI_KEEP_TENOR"
			}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

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
	void applyReduceEmiKeepTenorPrepaymentRecalculatesFutureSchedule() throws Exception {
		Long loanId = createBaseLoan();

		MvcResult result = mockMvc.perform(post("/api/loans/{loanId}/prepayments", loanId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(VALID_PREPAYMENT_REQUEST))
				.andExpect(status().isOk())
				.andReturn();

		JsonNode response = readJson(result);
		assertThat(response.path("loanId").longValue()).isEqualTo(loanId);
		assertThat(response.path("strategy").asString()).isEqualTo("REDUCE_EMI_KEEP_TENOR");
		assertThat(response.path("afterInstallmentNumber").intValue()).isEqualTo(24);
		assertThat(response.path("balanceBefore").asDecimal()).isEqualByComparingTo(new BigDecimal("669724.82"));
		assertThat(response.path("prepaymentAmount").asDecimal()).isEqualByComparingTo(new BigDecimal("200000.00"));
		assertThat(response.path("balanceAfter").asDecimal()).isEqualByComparingTo(new BigDecimal("469724.82"));
		assertThat(response.path("remainingMonths").intValue()).isEqualTo(36);
		assertThat(response.path("oldEmi").asDecimal()).isEqualByComparingTo(new BigDecimal("22244.45"));
		assertThat(response.path("newEmi").asDecimal()).isEqualByComparingTo(new BigDecimal("15601.59"));

		Loan loan = loanRepository.findById(loanId).orElseThrow();
		assertThat(loan.getMonthlyEmi()).isEqualByComparingTo(new BigDecimal("15601.59"));
		assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);

		List<LoanSchedule> schedule = loanScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);
		assertThat(schedule).hasSize(60);
		assertThat(schedule.subList(0, 24))
				.allSatisfy(row -> assertThat(row.getStatus()).isEqualTo(ScheduleStatus.PAID));
		assertThat(schedule.subList(24, 60))
				.allSatisfy(row -> assertThat(row.getStatus()).isEqualTo(ScheduleStatus.ADJUSTED));

		LoanSchedule paidThroughSchedule = schedule.get(23);
		assertThat(paidThroughSchedule.getInstallmentNumber()).isEqualTo(24);
		assertThat(paidThroughSchedule.getClosingBalance()).isEqualByComparingTo(new BigDecimal("669724.82"));
		assertThat(paidThroughSchedule.getEmiAmount()).isEqualByComparingTo(new BigDecimal("22244.45"));

		LoanSchedule firstAdjustedSchedule = schedule.get(24);
		assertThat(firstAdjustedSchedule.getInstallmentNumber()).isEqualTo(25);
		assertThat(firstAdjustedSchedule.getDueDate()).isEqualTo(LocalDate.of(2026, 7, 24));
		assertThat(firstAdjustedSchedule.getOpeningBalance()).isEqualByComparingTo(new BigDecimal("469724.82"));
		assertThat(firstAdjustedSchedule.getEmiAmount()).isEqualByComparingTo(new BigDecimal("15601.59"));
		assertThat(firstAdjustedSchedule.getInterestComponent()).isEqualByComparingTo(new BigDecimal("4697.25"));
		assertThat(firstAdjustedSchedule.getPrincipalComponent()).isEqualByComparingTo(new BigDecimal("10904.34"));
		assertThat(firstAdjustedSchedule.getClosingBalance()).isEqualByComparingTo(new BigDecimal("458820.48"));

		LoanSchedule finalAdjustedSchedule = schedule.get(59);
		assertThat(finalAdjustedSchedule.getInstallmentNumber()).isEqualTo(60);
		assertThat(finalAdjustedSchedule.getDueDate()).isEqualTo(LocalDate.of(2029, 6, 24));
		assertThat(finalAdjustedSchedule.getClosingBalance()).isEqualByComparingTo(new BigDecimal("0.00"));

		List<LoanTransaction> transactions = loanTransactionRepository.findByLoanIdOrderByCreatedAtAsc(loanId);
		assertThat(transactions).hasSize(1);
		LoanTransaction transaction = transactions.get(0);
		assertThat(transaction.getTransactionType()).isEqualTo(TransactionType.PREPAYMENT);
		assertThat(transaction.getStrategyType()).isEqualTo(PrepaymentStrategyType.REDUCE_EMI_KEEP_TENOR);
		assertThat(transaction.getInstallmentNumber()).isEqualTo(24);
		assertThat(transaction.getAmount()).isEqualByComparingTo(new BigDecimal("200000.00"));
		assertThat(transaction.getBalanceBefore()).isEqualByComparingTo(new BigDecimal("669724.82"));
		assertThat(transaction.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("469724.82"));
	}

	@Test
	void loanAndScheduleQueriesReflectAppliedPrepayment() throws Exception {
		Long loanId = createBaseLoan();

		mockMvc.perform(post("/api/loans/{loanId}/prepayments", loanId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(VALID_PREPAYMENT_REQUEST))
				.andExpect(status().isOk());

		MvcResult loanResult = mockMvc.perform(get("/api/loans/{loanId}", loanId))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode loan = readJson(loanResult);
		assertThat(loan.path("loanId").longValue()).isEqualTo(loanId);
		assertThat(loan.path("monthlyEmi").asDecimal()).isEqualByComparingTo(new BigDecimal("15601.59"));
		assertThat(loan.path("status").asString()).isEqualTo("ACTIVE");

		MvcResult scheduleResult = mockMvc.perform(get("/api/loans/{loanId}/schedule", loanId))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode schedule = readJson(scheduleResult);
		assertThat(schedule).hasSize(60);

		JsonNode paidThroughSchedule = schedule.path(23);
		assertThat(paidThroughSchedule.path("installmentNumber").intValue()).isEqualTo(24);
		assertThat(paidThroughSchedule.path("closingBalance").asDecimal())
				.isEqualByComparingTo(new BigDecimal("669724.82"));
		assertThat(paidThroughSchedule.path("status").asString()).isEqualTo("PAID");

		JsonNode firstAdjustedSchedule = schedule.path(24);
		assertThat(firstAdjustedSchedule.path("installmentNumber").intValue()).isEqualTo(25);
		assertThat(firstAdjustedSchedule.path("openingBalance").asDecimal())
				.isEqualByComparingTo(new BigDecimal("469724.82"));
		assertThat(firstAdjustedSchedule.path("emiAmount").asDecimal())
				.isEqualByComparingTo(new BigDecimal("15601.59"));
		assertThat(firstAdjustedSchedule.path("status").asString()).isEqualTo("ADJUSTED");
	}

	@Test
	void invalidPrepaymentAmountReturnsBadRequest() throws Exception {
		Long loanId = createBaseLoan();

		String request = """
				{
				  "afterInstallmentNumber": 24,
				  "amount": 0,
				  "strategy": "REDUCE_EMI_KEEP_TENOR"
				}
				""";

		mockMvc.perform(post("/api/loans/{loanId}/prepayments", loanId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request validation failed"));
	}

	@Test
	void amountGreaterThanOrEqualToOutstandingBalanceReturnsBadRequest() throws Exception {
		Long loanId = createBaseLoan();

		String request = """
				{
				  "afterInstallmentNumber": 24,
				  "amount": 669724.82,
				  "strategy": "REDUCE_EMI_KEEP_TENOR"
				}
				""";

		mockMvc.perform(post("/api/loans/{loanId}/prepayments", loanId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Prepayment amount must be less than outstanding balance"));
	}

	@Test
	void unsupportedStrategyReturnsBadRequest() throws Exception {
		Long loanId = createBaseLoan();

		String request = """
				{
				  "afterInstallmentNumber": 24,
				  "amount": 200000,
				  "strategy": "UNSUPPORTED"
				}
				""";

		mockMvc.perform(post("/api/loans/{loanId}/prepayments", loanId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Unsupported prepayment strategy: UNSUPPORTED"));
	}

	@Test
	void invalidInstallmentNumberReturnsBadRequest() throws Exception {
		Long loanId = createBaseLoan();

		String request = """
				{
				  "afterInstallmentNumber": 60,
				  "amount": 200000,
				  "strategy": "REDUCE_EMI_KEEP_TENOR"
				}
				""";

		mockMvc.perform(post("/api/loans/{loanId}/prepayments", loanId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("afterInstallmentNumber must be between 1 and 59"));
	}

	@Test
	void unknownLoanReturnsNotFound() throws Exception {
		mockMvc.perform(post("/api/loans/{loanId}/prepayments", 999999L)
						.contentType(MediaType.APPLICATION_JSON)
						.content(VALID_PREPAYMENT_REQUEST))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Loan not found: 999999"));
	}

	@Test
	void prepaymentOnNonActiveLoanReturnsBadRequest() throws Exception {
		Long loanId = createBaseLoan();
		Loan loan = loanRepository.findById(loanId).orElseThrow();
		loan.setStatus(LoanStatus.CLOSED);
		loanRepository.save(loan);

		mockMvc.perform(post("/api/loans/{loanId}/prepayments", loanId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(VALID_PREPAYMENT_REQUEST))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Prepayment is only allowed for ACTIVE loans"));
	}

	private Long createBaseLoan() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/loans")
						.contentType(MediaType.APPLICATION_JSON)
						.content(BASE_LOAN_REQUEST))
				.andExpect(status().isCreated())
				.andReturn();

		return readJson(result).path("loanId").longValue();
	}

	private JsonNode readJson(MvcResult result) throws Exception {
		String responseBody = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
		return objectMapper.readTree(responseBody);
	}
}
