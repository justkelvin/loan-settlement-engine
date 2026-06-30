package com.kelvin.loanengine;

import org.springframework.boot.SpringApplication;

public class TestLoanEngineApplication {

	public static void main(String[] args) {
		SpringApplication.from(LoanEngineApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
