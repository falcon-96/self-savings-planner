package com.blackrock_hackathon.self_savings_planner.service;

import com.blackrock_hackathon.self_savings_planner.dto.common.TemporalData;
import com.blackrock_hackathon.self_savings_planner.dto.period.K;
import com.blackrock_hackathon.self_savings_planner.dto.request.FilterRequest;
import com.blackrock_hackathon.self_savings_planner.dto.request.TransactionInput;
import com.blackrock_hackathon.self_savings_planner.dto.request.ValidatorRequest;
import com.blackrock_hackathon.self_savings_planner.dto.response.EnrichedTransaction;
import com.blackrock_hackathon.self_savings_planner.dto.response.InvalidTransaction;
import com.blackrock_hackathon.self_savings_planner.dto.response.ValidTransaction;
import com.blackrock_hackathon.self_savings_planner.dto.response.ValidationResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles transaction parsing, validation, and filtering.
 */
@Service
public class TransactionService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** Rounds each transaction amount up to the nearest 100 and computes the remnant. */
    public List<EnrichedTransaction> parseTransactions(List<TransactionInput> transactions) {
        return transactions.stream()
                .map(tx -> {
                    double ceiling = Math.ceil(tx.amount() / 100.0) * 100.0;
                    return new EnrichedTransaction(tx.date(), tx.amount(), ceiling, ceiling - tx.amount());
                })
                .toList();
    }

    /** Validates transactions against the wage cap. */
    public ValidationResult validateTransactionWithWage(ValidatorRequest request) {
        if (request == null) return new ValidationResult(List.of(), List.of());
        double wage = request.wage() != null ? request.wage() : 0.0;
        return validate(wage, request.transactions(), null);
    }

    /** Validates transactions and marks K-period membership. */
    public ValidationResult validateTransactionWithWageAndPeriods(FilterRequest request) {
        if (request == null) return new ValidationResult(List.of(), List.of());
        double wage = request.wage() != null ? request.wage() : 0.0;
        return validate(wage, request.transactions(), request.k());
    }

    private ValidationResult validate(double wageValue, List<? extends Record> transactions, List<K> kPeriods) {
        BigDecimal wage = BigDecimal.valueOf(wageValue);
        if (wage.compareTo(BigDecimal.ZERO) < 0) {
            return new ValidationResult(List.of(),
                    List.of(new InvalidTransaction(null, wageValue, "Wage must be >= 0")));
        }
        if (transactions == null || transactions.isEmpty()) {
            return new ValidationResult(List.of(), List.of());
        }

        List<ValidTransaction> valid = new ArrayList<>();
        List<InvalidTransaction> invalid = new ArrayList<>();
        BigDecimal runningSum = BigDecimal.ZERO;
        Set<String> seen = new HashSet<>();

        for (Record rec : transactions) {
            LocalDateTime date;
            Double amount;
            double ceiling, remnant;

            if (rec instanceof EnrichedTransaction et) {
                date = et.date(); amount = et.amount(); ceiling = et.ceiling(); remnant = et.remnant();
            } else if (rec instanceof TransactionInput ti) {
                date = ti.date(); amount = ti.amount();
                BigDecimal a = BigDecimal.valueOf(amount);
                BigDecimal c = a.divide(HUNDRED, 0, RoundingMode.CEILING).multiply(HUNDRED);
                ceiling = c.doubleValue(); remnant = c.subtract(a).doubleValue();
            } else {
                continue;
            }

            // Null checks
            if (date == null) { invalid.add(new InvalidTransaction(null, amount, "Date must not be null")); continue; }
            if (amount == null) { invalid.add(new InvalidTransaction(date, null, "Amount must not be null")); continue; }

            // Duplicate check
            String key = date + "|" + amount;
            if (!seen.add(key)) { invalid.add(new InvalidTransaction(date, amount, "Duplicate transaction")); continue; }

            BigDecimal amtBD = BigDecimal.valueOf(amount);

            // Amount range check
            if (amtBD.compareTo(BigDecimal.ZERO) < 0) { invalid.add(new InvalidTransaction(date, amount, "Amount must be >= 0")); continue; }
            if (amtBD.compareTo(wage) > 0) { invalid.add(new InvalidTransaction(date, amount, "Amount exceeds wage")); continue; }

            // Ceiling/remnant accuracy check
            BigDecimal expectedCeiling = amtBD.divide(HUNDRED, 0, RoundingMode.CEILING).multiply(HUNDRED);
            BigDecimal expectedRemnant = expectedCeiling.subtract(amtBD);
            if (Math.abs(ceiling - expectedCeiling.doubleValue()) > 1e-9) {
                invalid.add(new InvalidTransaction(date, amount, "Ceiling mismatch")); continue;
            }
            if (Math.abs(remnant - expectedRemnant.doubleValue()) > 1e-9) {
                invalid.add(new InvalidTransaction(date, amount, "Remnant mismatch")); continue;
            }

            // Wage cap check
            if (runningSum.add(amtBD).compareTo(wage) > 0) {
                invalid.add(new InvalidTransaction(date, amount, "Total exceeds wage")); continue;
            }

            runningSum = runningSum.add(amtBD);
            valid.add(new ValidTransaction(date, amount, ceiling, remnant, inKPeriod(date, kPeriods)));
        }

        return new ValidationResult(valid, invalid);
    }

    private boolean inKPeriod(LocalDateTime date, List<K> kPeriods) {
        if (kPeriods == null) return false;
        for (K k : kPeriods) {
            TemporalData td = k.temporalData();
            if (td != null && td.start() != null && td.end() != null
                    && !date.isBefore(td.start()) && !date.isAfter(td.end())) {
                return true;
            }
        }
        return false;
    }
}
