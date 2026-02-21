package com.blackrock_hackathon.self_savings_planner.service;

import com.blackrock_hackathon.self_savings_planner.dto.request.TransactionInput;
import com.blackrock_hackathon.self_savings_planner.dto.request.ValidatorRequest;
import com.blackrock_hackathon.self_savings_planner.dto.response.EnrichedTransaction;
import com.blackrock_hackathon.self_savings_planner.dto.response.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionServiceTest {

    private TransactionService service;

    @BeforeEach
    void setUp() {
        service = new TransactionService();
    }

    @Nested
    @DisplayName("parseTransactions")
    class ParseTests {

        @Test
        @DisplayName("rounds up to nearest 100 and computes remnant")
        void roundsUpCorrectly() {
            var input = List.of(
                    tx("2023-01-01 10:00:00", 375),
                    tx("2023-02-01 10:00:00", 620),
                    tx("2023-03-01 10:00:00", 100),
                    tx("2023-04-01 10:00:00", 99)
            );

            List<EnrichedTransaction> result = service.parseTransactions(input);

            assertEquals(4, result.size());
            assertEnriched(result.get(0), 375, 400, 25);
            assertEnriched(result.get(1), 620, 700, 80);
            assertEnriched(result.get(2), 100, 100, 0);
            assertEnriched(result.get(3), 99, 100, 1);
        }

        @Test
        @DisplayName("empty list returns empty")
        void emptyInput() {
            assertTrue(service.parseTransactions(List.of()).isEmpty());
        }
    }

    @Nested
    @DisplayName("validateTransactionWithWage")
    class ValidationTests {

        @Test
        @DisplayName("valid transactions pass all checks")
        void validTransactionsPass() {
            var request = new ValidatorRequest(50000.0, List.of(
                    enriched("2023-01-15 08:00:00", 375, 400, 25),
                    enriched("2023-03-20 12:00:00", 620, 700, 80)
            ));

            ValidationResult result = service.validateTransactionWithWage(request);

            assertEquals(2, result.validTransactions().size());
            assertTrue(result.invalidTransactions().isEmpty());
        }

        @Test
        @DisplayName("negative amount is rejected")
        void negativeAmountRejected() {
            var request = new ValidatorRequest(50000.0, List.of(
                    enriched("2023-01-15 08:00:00", -10, 0, 0)
            ));

            ValidationResult result = service.validateTransactionWithWage(request);

            assertTrue(result.validTransactions().isEmpty());
            assertEquals(1, result.invalidTransactions().size());
            assertTrue(result.invalidTransactions().getFirst().message().contains(">="));
        }

        @Test
        @DisplayName("duplicate transactions are rejected")
        void duplicatesRejected() {
            var request = new ValidatorRequest(50000.0, List.of(
                    enriched("2023-01-15 08:00:00", 375, 400, 25),
                    enriched("2023-01-15 08:00:00", 375, 400, 25)
            ));

            ValidationResult result = service.validateTransactionWithWage(request);

            assertEquals(1, result.validTransactions().size());
            assertEquals(1, result.invalidTransactions().size());
            assertTrue(result.invalidTransactions().getFirst().message().contains("Duplicate"));
        }

        @Test
        @DisplayName("amount exceeding wage is rejected")
        void exceedsWageRejected() {
            var request = new ValidatorRequest(100.0, List.of(
                    enriched("2023-01-15 08:00:00", 200, 200, 0)
            ));

            ValidationResult result = service.validateTransactionWithWage(request);

            assertTrue(result.validTransactions().isEmpty());
            assertEquals(1, result.invalidTransactions().size());
        }

        @Test
        @DisplayName("wrong ceiling is rejected")
        void wrongCeilingRejected() {
            var request = new ValidatorRequest(50000.0, List.of(
                    enriched("2023-01-15 08:00:00", 375, 500, 125)  // ceiling should be 400
            ));

            ValidationResult result = service.validateTransactionWithWage(request);

            assertTrue(result.validTransactions().isEmpty());
            assertEquals(1, result.invalidTransactions().size());
            assertTrue(result.invalidTransactions().getFirst().message().contains("Ceiling"));
        }

        @Test
        @DisplayName("null request returns empty result")
        void nullRequest() {
            ValidationResult result = service.validateTransactionWithWage(null);
            assertTrue(result.validTransactions().isEmpty());
            assertTrue(result.invalidTransactions().isEmpty());
        }
    }

    // helpers

    private static TransactionInput tx(String datetime, double amount) {
        return new TransactionInput(LocalDateTime.parse(datetime.replace(" ", "T")), amount);
    }

    private static EnrichedTransaction enriched(String datetime, double amount, double ceiling, double remnant) {
        return new EnrichedTransaction(LocalDateTime.parse(datetime.replace(" ", "T")), amount, ceiling, remnant);
    }

    private static void assertEnriched(EnrichedTransaction et, double expectedAmount, double expectedCeiling, double expectedRemnant) {
        assertEquals(expectedAmount, et.amount(), 1e-9);
        assertEquals(expectedCeiling, et.ceiling(), 1e-9);
        assertEquals(expectedRemnant, et.remnant(), 1e-9);
    }
}
