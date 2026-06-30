package com.kelvin.loanengine.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kelvin.loanengine.entity.PrepaymentStrategyType;
import com.kelvin.loanengine.exception.UnsupportedPrepaymentStrategyException;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrepaymentStrategyResolverTest {

	@Test
	void resolvesSupportedStrategyType() {
		ReduceEmiKeepTenorPrepaymentStrategy strategy = new ReduceEmiKeepTenorPrepaymentStrategy();
		PrepaymentStrategyResolver resolver = new PrepaymentStrategyResolver(List.of(strategy));

		PrepaymentStrategy resolved = resolver.resolve(PrepaymentStrategyType.REDUCE_EMI_KEEP_TENOR);

		assertThat(resolved).isSameAs(strategy);
	}

	@Test
	void resolvesSupportedStrategyName() {
		ReduceEmiKeepTenorPrepaymentStrategy strategy = new ReduceEmiKeepTenorPrepaymentStrategy();
		PrepaymentStrategyResolver resolver = new PrepaymentStrategyResolver(List.of(strategy));

		PrepaymentStrategy resolved = resolver.resolve("REDUCE_EMI_KEEP_TENOR");

		assertThat(resolved).isSameAs(strategy);
	}

	@Test
	void rejectsUnsupportedStrategyName() {
		PrepaymentStrategyResolver resolver =
				new PrepaymentStrategyResolver(List.of(new ReduceEmiKeepTenorPrepaymentStrategy()));

		assertThatThrownBy(() -> resolver.resolve("REDUCE_TENOR_KEEP_EMI"))
				.isInstanceOf(UnsupportedPrepaymentStrategyException.class)
				.hasMessage("Unsupported prepayment strategy: REDUCE_TENOR_KEEP_EMI");
	}

	@Test
	void rejectsMissingStrategyName() {
		PrepaymentStrategyResolver resolver =
				new PrepaymentStrategyResolver(List.of(new ReduceEmiKeepTenorPrepaymentStrategy()));

		assertThatThrownBy(() -> resolver.resolve((String) null))
				.isInstanceOf(UnsupportedPrepaymentStrategyException.class)
				.hasMessage("Unsupported prepayment strategy: null");

		assertThatThrownBy(() -> resolver.resolve(" "))
				.isInstanceOf(UnsupportedPrepaymentStrategyException.class)
				.hasMessage("Unsupported prepayment strategy:  ");
	}

	@Test
	void rejectsDuplicateRegisteredStrategies() {
		ReduceEmiKeepTenorPrepaymentStrategy first = new ReduceEmiKeepTenorPrepaymentStrategy();
		ReduceEmiKeepTenorPrepaymentStrategy second = new ReduceEmiKeepTenorPrepaymentStrategy();

		assertThatThrownBy(() -> new PrepaymentStrategyResolver(List.of(first, second)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Duplicate prepayment strategy: REDUCE_EMI_KEEP_TENOR");
	}
}
