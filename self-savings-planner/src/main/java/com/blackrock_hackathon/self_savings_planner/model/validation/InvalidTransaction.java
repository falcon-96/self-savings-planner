package com.blackrock_hackathon.self_savings_planner.model.validation;

import java.time.LocalDateTime;

public record InvalidTransaction(LocalDateTime date, Double amount, String message) {
}
