package com.blackrock_hackathon.self_savings_planner.dto.response;

import java.util.List;

public record ValidationResult(List<ValidTransaction> validTransactions,
                                List<InvalidTransaction> invalidTransactions) {
}
