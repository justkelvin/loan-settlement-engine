package com.kelvin.loanengine.strategy;

import com.kelvin.loanengine.entity.PrepaymentStrategyType;
import com.kelvin.loanengine.exception.UnsupportedPrepaymentStrategyException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PrepaymentStrategyResolver {

	private final Map<PrepaymentStrategyType, PrepaymentStrategy> strategies;

	public PrepaymentStrategyResolver(List<PrepaymentStrategy> strategies) {
		this.strategies = new EnumMap<>(PrepaymentStrategyType.class);
		for (PrepaymentStrategy strategy : strategies) {
			PrepaymentStrategy previous = this.strategies.put(strategy.getStrategyType(), strategy);
			if (previous != null) {
				throw new IllegalStateException("Duplicate prepayment strategy: " + strategy.getStrategyType());
			}
		}
	}

	public PrepaymentStrategy resolve(PrepaymentStrategyType strategyType) {
		PrepaymentStrategy strategy = strategies.get(strategyType);
		if (strategy == null) {
			throw new UnsupportedPrepaymentStrategyException(String.valueOf(strategyType));
		}

		return strategy;
	}

	public PrepaymentStrategy resolve(String strategyName) {
		if (strategyName == null || strategyName.isBlank()) {
			throw new UnsupportedPrepaymentStrategyException(strategyName);
		}

		try {
			return resolve(PrepaymentStrategyType.valueOf(strategyName.trim()));
		} catch (IllegalArgumentException exception) {
			throw new UnsupportedPrepaymentStrategyException(strategyName);
		}
	}
}
