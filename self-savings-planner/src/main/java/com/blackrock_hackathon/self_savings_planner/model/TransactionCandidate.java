package com.blackrock_hackathon.self_savings_planner.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record TransactionCandidate(
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime date,
        double amount,
        double ceiling,
        double remnant
) {
}
