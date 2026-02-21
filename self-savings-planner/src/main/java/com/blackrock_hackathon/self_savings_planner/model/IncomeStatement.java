package com.blackrock_hackathon.self_savings_planner.model;

import java.util.List;

public record IncomeStatement(Double wage, List<TransactionCandidate> transactions) {
}
