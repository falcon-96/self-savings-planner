package com.blackrock_hackathon.self_savings_planner.model.validation;

import java.util.List;

public record TransactionValidationResult(List<ValidTransaction> validTransactions,
                                          List<InvalidTransaction> invalidTransactions) {
}
