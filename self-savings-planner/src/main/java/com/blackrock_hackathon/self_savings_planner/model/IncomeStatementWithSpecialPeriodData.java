package com.blackrock_hackathon.self_savings_planner.model;

import com.blackrock_hackathon.self_savings_planner.model.specialPeriod.K;
import com.blackrock_hackathon.self_savings_planner.model.specialPeriod.P;
import com.blackrock_hackathon.self_savings_planner.model.specialPeriod.Q;

import java.util.List;

public record IncomeStatementWithSpecialPeriodData(List<Q> q, List<P> p, List<K> k, Double wage,
                                                   List<TransactionCandidate> transactions) {
}
