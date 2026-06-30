package com.kelvin.loanengine.strategy;

import com.kelvin.loanengine.entity.PrepaymentStrategyType;

public interface PrepaymentStrategy {

	PrepaymentStrategyType getStrategyType();

	PrepaymentCalculationResult calculate(PrepaymentCalculationInput input);
}
