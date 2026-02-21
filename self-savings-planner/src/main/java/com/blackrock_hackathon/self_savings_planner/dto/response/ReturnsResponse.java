package com.blackrock_hackathon.self_savings_planner.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ReturnsResponse(
        @JsonProperty("totalTransactionAmount") Double totalTransactionAmount,
        @JsonProperty("totalCeiling") Double totalCeiling,
        @JsonProperty("savingsByDates") List<Saving> savingsByDates
) {
}
