package com.blackrock_hackathon.self_savings_planner.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record PerformanceReport(
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
        LocalDateTime time,
        String memory,
        Integer threads
) {
}
