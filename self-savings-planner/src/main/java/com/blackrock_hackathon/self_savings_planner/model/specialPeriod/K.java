package com.blackrock_hackathon.self_savings_planner.model.specialPeriod;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record K(
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end
) {
}
