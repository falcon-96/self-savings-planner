package com.blackrock_hackathon.self_savings_planner.model;

import com.blackrock_hackathon.self_savings_planner.model.specialPeriod.K;
import com.blackrock_hackathon.self_savings_planner.model.specialPeriod.P;
import com.blackrock_hackathon.self_savings_planner.model.specialPeriod.Q;

import java.util.List;

public record ReturnsRequest(int age, Double wage, Double inflation, List<Q> q, List<P> p, List<K> k,
                             List<TransactionCandidate> transactions) {
}
