package com.blackrock_hackathon.self_savings_planner.service;

import com.blackrock_hackathon.self_savings_planner.model.*;
import com.blackrock_hackathon.self_savings_planner.model.specialPeriod.K;
import com.blackrock_hackathon.self_savings_planner.model.validation.InvalidTransaction;
import com.blackrock_hackathon.self_savings_planner.model.validation.TransactionValidationResult;
import com.blackrock_hackathon.self_savings_planner.model.validation.ValidTransaction;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TransactionService {
    public List<EnrichedTransaction> parseTransactions(List<TransactionCandidate> transactions) {
        return transactions.stream()
                .map(transaction -> {
                    double roundedCeiling = Math.ceil(transaction.amount() / 100.0) * 100.0;
                    double remnant = roundedCeiling - transaction.amount();
                    return new EnrichedTransaction(transaction.date(), transaction.amount(), roundedCeiling, remnant);
                })
                .toList();
    }

    public TransactionValidationResult validateTransactionWithWage(IncomeStatement incomeStatement) {
        if (incomeStatement == null) {
            return new TransactionValidationResult(List.of(), List.of());
        }
        return validateTransactionsInternal(incomeStatement.wage(), incomeStatement.transactions(), null);
    }

    public TransactionValidationResult validateTransactionWithWageAndPeriods(
            IncomeStatementWithSpecialPeriodData incomeStatementWithSpecialPeriodData) {
        if (incomeStatementWithSpecialPeriodData == null) {
            return new TransactionValidationResult(List.of(), List.of());
        }
        return validateTransactionsInternal(
                incomeStatementWithSpecialPeriodData.wage(),
                incomeStatementWithSpecialPeriodData.transactions(),
                incomeStatementWithSpecialPeriodData.k()
        );
    }

    private TransactionValidationResult validateTransactionsInternal(
            double wageValue,
            List<TransactionCandidate> transactions,
            List<K> kPeriods) {

        BigDecimal wage = BigDecimal.valueOf(wageValue);
        if (wage.compareTo(BigDecimal.ZERO) < 0) {
            return new TransactionValidationResult(List.of(), List.of(new InvalidTransaction(null, wageValue, "Wage must be >= 0")));
        }

        if (transactions == null || transactions.isEmpty()) {
            return new TransactionValidationResult(List.of(), List.of());
        }

        List<ValidTransaction> validTransactions = new ArrayList<>();
        List<InvalidTransaction> invalidTransactions = new ArrayList<>();
        BigDecimal validSum = BigDecimal.ZERO;
        Set<String> seenTransactions = new HashSet<>();

        for (TransactionCandidate tx : transactions) {
            if (!isTransactionValid(tx, wage, validSum, seenTransactions,
                    validTransactions, invalidTransactions, kPeriods)) {
                continue;
            }
            validSum = validSum.add(BigDecimal.valueOf(tx.amount()));
        }

        return new TransactionValidationResult(validTransactions, invalidTransactions);
    }

    private boolean isTransactionValid(TransactionCandidate tx,
                                       BigDecimal wage,
                                       BigDecimal validSum,
                                       Set<String> seenTransactions,
                                       List<ValidTransaction> validTransactions,
                                       List<InvalidTransaction> invalidTransactions,
                                       List<K> kPeriods) {
        if (tx == null) {
            invalidTransactions.add(new InvalidTransaction(null, 0.0, "Transaction must not be null"));
            return false;
        }

        if (tx.date() == null) {
            invalidTransactions.add(new InvalidTransaction(null, tx.amount(), "Transaction date must not be null"));
            return false;
        }

        String key = tx.date() + "|" + tx.amount();
        if (!seenTransactions.add(key)) {
            invalidTransactions.add(new InvalidTransaction(tx.date(), tx.amount(), "Duplicate transaction detected"));
            return false;
        }

        BigDecimal amount = BigDecimal.valueOf(tx.amount());
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            invalidTransactions.add(new InvalidTransaction(tx.date(), tx.amount(), "Amount must be >= 0"));
            return false;
        }

        if (amount.compareTo(wage) > 0) {
            invalidTransactions.add(new InvalidTransaction(tx.date(), tx.amount(), "Amount must be <= wage"));
            return false;
        }

        if (!isCeilingAndRemnantValid(tx, amount, invalidTransactions)) {
            return false;
        }

        if (validSum.add(amount).compareTo(wage) > 0) {
            invalidTransactions.add(new InvalidTransaction(tx.date(), tx.amount(), "Total of valid transactions must not exceed wage"));
            return false;
        }

        boolean inKPeriod = isInKPeriod(tx.date(), kPeriods);

        validTransactions.add(new ValidTransaction(
                tx.date(),
                tx.amount(),
                tx.ceiling(),
                tx.remnant(),
                inKPeriod
        ));
        return true;
    }

    private boolean isCeilingAndRemnantValid(TransactionCandidate tx,
                                             BigDecimal amount,
                                             List<InvalidTransaction> invalidTransactions) {
        BigDecimal expectedCeiling = amount
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.CEILING)
                .multiply(BigDecimal.valueOf(100));
        BigDecimal expectedRemnant = expectedCeiling.subtract(amount);

        if (Math.abs(tx.ceiling() - expectedCeiling.doubleValue()) > 1e-9) {
            invalidTransactions.add(new InvalidTransaction(tx.date(), tx.amount(), "Ceiling is not accurate according to amount"));
            return false;
        }

        if (Math.abs(tx.remnant() - expectedRemnant.doubleValue()) > 1e-9) {
            invalidTransactions.add(new InvalidTransaction(tx.date(), tx.amount(), "Remnant is not accurate according to ceiling and amount"));
            return false;
        }

        return true;
    }


    private boolean isInKPeriod(LocalDateTime date, List<K> kPeriods) {
        if (kPeriods == null || kPeriods.isEmpty()) {
            return false;
        }
        for (K k : kPeriods) {
            if (k.start() == null || k.end() == null) {
                continue;
            }
            if ((date.isEqual(k.start()) || date.isEqual(k.end()))
                    || (date.isAfter(k.start()) && date.isBefore(k.end()))) {
                return true;
            }
        }
        return false;
    }
}
