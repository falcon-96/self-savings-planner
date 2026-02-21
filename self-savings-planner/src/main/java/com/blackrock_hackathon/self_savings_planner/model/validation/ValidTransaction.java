package com.blackrock_hackathon.self_savings_planner.model.validation;

import java.time.LocalDateTime;

public record ValidTransaction(LocalDateTime date, Double amount, Double ceiling, Double remnant, boolean inKPeriod) {
}
