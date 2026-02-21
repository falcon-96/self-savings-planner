package com.blackrock_hackathon.self_savings_planner.dto.common;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record TemporalData(
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime start,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime end) {
}
