package com.blackrock_hackathon.self_savings_planner.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record EnrichedTransaction(
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime date,
        Double amount,
        Double ceiling,
        Double remnant
) {
}
