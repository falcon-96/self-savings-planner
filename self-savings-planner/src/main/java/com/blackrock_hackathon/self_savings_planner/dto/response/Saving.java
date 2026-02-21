package com.blackrock_hackathon.self_savings_planner.dto.response;

import com.blackrock_hackathon.self_savings_planner.dto.common.TemporalData;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record Saving(
        @JsonUnwrapped TemporalData temporalData,
        Double amount,
        Double profit,
        Double taxBenefit
) {
}
