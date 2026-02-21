package com.blackrock_hackathon.self_savings_planner.model;

import java.util.List;

public record ReturnsResponse(Double totalTransactionAmount,
                              Double totalCeiling,
                              List<Saving> savingsByDate) {
}
