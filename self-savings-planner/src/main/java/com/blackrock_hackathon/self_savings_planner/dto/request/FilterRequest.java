package com.blackrock_hackathon.self_savings_planner.dto.request;

import com.blackrock_hackathon.self_savings_planner.dto.period.K;
import com.blackrock_hackathon.self_savings_planner.dto.period.P;
import com.blackrock_hackathon.self_savings_planner.dto.period.Q;

import java.util.List;

public record FilterRequest(List<Q> q, List<P> p, List<K> k, Double wage,
                             List<TransactionInput> transactions) {
}
