package com.blackrock_hackathon.self_savings_planner.service;

import com.blackrock_hackathon.self_savings_planner.model.IncomeStatement;
import com.blackrock_hackathon.self_savings_planner.model.TransactionCandidate;
import com.blackrock_hackathon.self_savings_planner.model.validation.TransactionValidationResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionServiceTest {

    private final TransactionService transactionService = new TransactionService();

    @Test
    void validateTransactionWithWage_partitionsValidAndInvalid_andEnforcesTotalWageCap() {
        IncomeStatement incomeStatement = new IncomeStatement(
                50000,
                List.of(
                        // invalid: ceiling/remnant wrong
                        new TransactionCandidate(LocalDateTime.parse("2023-01-15T10:30:00"), 2000, 300, 50),
                        new TransactionCandidate(LocalDateTime.parse("2023-03-20T14:45:00"), 3500, 400, 70),
                        new TransactionCandidate(LocalDateTime.parse("2023-06-10T09:15:00"), 1500, 200, 30),
                        // invalid: amount < 0
                        new TransactionCandidate(LocalDateTime.parse("2023-07-10T09:15:00"), -250, 200, 30)
                )
        );

        TransactionValidationResult result = transactionService.validateTransactionWithWage(incomeStatement);

        assertNotNull(result);
        assertEquals(0, result.validTransactions().size());
        assertEquals(4, result.invalidTransactions().size());
        assertTrue(result.invalidTransactions().stream().anyMatch(t -> t.message().contains("Amount must be >= 0")));
    }

    @Test
    void validateTransactionWithWage_marksValid_whenCeilingAndRemnantMatchNearest100Rule() {
        IncomeStatement incomeStatement = new IncomeStatement(
                50000,
                List.of(
                        new TransactionCandidate(LocalDateTime.parse("2023-01-15T10:30:00"), 2000, 2000, 0),
                        new TransactionCandidate(LocalDateTime.parse("2023-03-20T14:45:00"), 3500, 3500, 0),
                        new TransactionCandidate(LocalDateTime.parse("2023-06-10T09:15:00"), 1501, 1600, 99)
                )
        );

        TransactionValidationResult result = transactionService.validateTransactionWithWage(incomeStatement);

        assertNotNull(result);
        assertEquals(3, result.validTransactions().size());
        assertEquals(0, result.invalidTransactions().size());
    }

    @Test
    void validateTransactionWithWage_invalidWhenValidSumWouldExceedWage() {
        IncomeStatement incomeStatement = new IncomeStatement(
                3000,
                List.of(
                        new TransactionCandidate(LocalDateTime.parse("2023-01-15T10:30:00"), 2000, 2000, 0),
                        // would exceed wage if accepted
                        new TransactionCandidate(LocalDateTime.parse("2023-03-20T14:45:00"), 1501, 1600, 99)
                )
        );

        TransactionValidationResult result = transactionService.validateTransactionWithWage(incomeStatement);

        assertEquals(1, result.validTransactions().size());
        assertEquals(1, result.invalidTransactions().size());
        assertTrue(result.invalidTransactions().getFirst().message().contains("Total of valid transactions"));
    }
}
