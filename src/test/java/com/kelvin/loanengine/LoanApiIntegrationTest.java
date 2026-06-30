package com.kelvin.loanengine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kelvin.loanengine.entity.Loan;
import com.kelvin.loanengine.entity.LoanSchedule;
import com.kelvin.loanengine.entity.LoanStatus;
import com.kelvin.loanengine.entity.ScheduleStatus;
import com.kelvin.loanengine.repository.LoanRepository;
import com.kelvin.loanengine.repository.LoanScheduleRepository;
import com.kelvin.loanengine.repository.LoanTransactionRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
class LoanApiIntegrationTest {

	private static final String BASE_LOAN_REQUEST = """
			{
			  "principalAmount": 1000000,
			  "annualInterestRate": 12,
			  "tenorMonths": 60,
			  "startDate": "2024-07-24"
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
	void createLoanPersistsLoanAndFullSchedule() throws Exception {
		JsonNode response = createBaseLoan();
		Long loanId = response.path("loanId").longValue();

		assertThat(loanId).isNotNull();
		assertThat(response.path("principalAmount").asDecimal()).isEqualByComparingTo(new BigDecimal("1000000.00"));
		assertThat(response.path("annualInterestRate").asDecimal()).isEqualByComparingTo(new BigDecimal("12"));
		assertThat(response.path("tenorMonths").intValue()).isEqualTo(60);
		assertThat(response.path("monthlyEmi").asDecimal()).isEqualByComparingTo(new BigDecimal("22244.45"));
		assertThat(response.path("status").asString()).isEqualTo("ACTIVE");
		assertThat(response.path("startDate").asString()).isEqualTo("2024-07-24");

		Loan savedLoan = loanRepository.findById(loanId).orElseThrow();
		assertThat(savedLoan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
		assertThat(savedLoan.getMonthlyEmi()).isEqualByComparingTo(new BigDecimal("22244.45"));

		List<LoanSchedule> schedule = loanScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);
		assertThat(schedule).hasSize(60);
		assertThat(schedule).allSatisfy(row -> assertThat(row.getStatus()).isEqualTo(ScheduleStatus.PENDING));
	}

	@Test
	void getLoanReturnsPersistedLoanDetails() throws Exception {
		Long loanId = createBaseLoan().path("loanId").longValue();

		MvcResult result = mockMvc.perform(get("/api/loans/{loanId}", loanId))
				.andExpect(status().isOk())
				.andReturn();

		JsonNode response = readJson(result);
		assertThat(response.path("loanId").longValue()).isEqualTo(loanId);
		assertThat(response.path("monthlyEmi").asDecimal()).isEqualByComparingTo(new BigDecimal("22244.45"));
		assertThat(response.path("status").asString()).isEqualTo("ACTIVE");
	}

	@Test
	void getScheduleReturnsRowsOrderedByInstallmentNumber() throws Exception {
		Long loanId = createBaseLoan().path("loanId").longValue();

		MvcResult result = mockMvc.perform(get("/api/loans/{loanId}/schedule", loanId))
				.andExpect(status().isOk())
				.andReturn();

		JsonNode schedule = readJson(result);
		assertThat(schedule).hasSize(60);

		JsonNode firstInstallment = schedule.path(0);
		assertThat(firstInstallment.path("loanId").longValue()).isEqualTo(loanId);
		assertThat(firstInstallment.path("installmentNumber").intValue()).isEqualTo(1);
		assertThat(firstInstallment.path("dueDate").asString()).isEqualTo("2024-07-24");
		assertThat(firstInstallment.path("interestComponent").asDecimal()).isEqualByComparingTo(new BigDecimal("10000.00"));
		assertThat(firstInstallment.path("principalComponent").asDecimal()).isEqualByComparingTo(new BigDecimal("12244.45"));
		assertThat(firstInstallment.path("closingBalance").asDecimal()).isEqualByComparingTo(new BigDecimal("987755.55"));

		JsonNode installment = schedule.path(23);
		assertThat(installment.path("installmentNumber").intValue()).isEqualTo(24);
		assertThat(installment.path("interestComponent").asDecimal()).isEqualByComparingTo(new BigDecimal("6851.18"));
		assertThat(installment.path("principalComponent").asDecimal()).isEqualByComparingTo(new BigDecimal("15393.27"));
		assertThat(installment.path("closingBalance").asDecimal()).isEqualByComparingTo(new BigDecimal("669724.82"));
	}

	@Test
	void unknownLoanReturnsNotFound() throws Exception {
		mockMvc.perform(get("/api/loans/{loanId}", 999999L))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/loans/{loanId}/schedule", 999999L))
				.andExpect(status().isNotFound());
	}

	@Test
	void invalidCreateLoanRequestReturnsBadRequest() throws Exception {
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
				.andExpect(status().isBadRequest());
	}

	private JsonNode createBaseLoan() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/loans")
						.contentType(MediaType.APPLICATION_JSON)
						.content(BASE_LOAN_REQUEST))
				.andExpect(status().isCreated())
				.andReturn();

		return readJson(result);
	}

	private JsonNode readJson(MvcResult result) throws Exception {
		String responseBody = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
		return objectMapper.readTree(responseBody);
	}
}
