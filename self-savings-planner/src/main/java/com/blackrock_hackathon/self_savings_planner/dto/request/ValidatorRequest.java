package com.blackrock_hackathon.self_savings_planner.dto.request;

import com.blackrock_hackathon.self_savings_planner.dto.response.EnrichedTransaction;

import java.util.List;

public record ValidatorRequest(Double wage, List<EnrichedTransaction> transactions) {
}
