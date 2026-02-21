package com.blackrock_hackathon.self_savings_planner.dto.period;

import com.blackrock_hackathon.self_savings_planner.dto.common.TemporalData;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record K(@JsonUnwrapped TemporalData temporalData) {
}
