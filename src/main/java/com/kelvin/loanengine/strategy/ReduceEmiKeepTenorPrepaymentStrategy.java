package com.kelvin.loanengine.strategy;

import com.kelvin.loanengine.entity.PrepaymentStrategyType;
import org.springframework.stereotype.Component;

@Component
public class ReduceEmiKeepTenorPrepaymentStrategy implements PrepaymentStrategy {

	@Override
	public PrepaymentStrategyType getStrategyType() {
		return PrepaymentStrategyType.REDUCE_EMI_KEEP_TENOR;
	}
}
