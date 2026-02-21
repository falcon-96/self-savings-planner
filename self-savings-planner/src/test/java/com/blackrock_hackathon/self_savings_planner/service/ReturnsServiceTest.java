package com.blackrock_hackathon.self_savings_planner.service;

import com.blackrock_hackathon.self_savings_planner.dto.common.TemporalData;
import com.blackrock_hackathon.self_savings_planner.dto.period.K;
import com.blackrock_hackathon.self_savings_planner.dto.period.P;
import com.blackrock_hackathon.self_savings_planner.dto.period.Q;
import com.blackrock_hackathon.self_savings_planner.dto.request.ReturnsRequest;
import com.blackrock_hackathon.self_savings_planner.dto.request.TransactionInput;
import com.blackrock_hackathon.self_savings_planner.dto.response.ReturnsResponse;
import com.blackrock_hackathon.self_savings_planner.dto.response.Saving;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReturnsServiceTest {

    private ReturnsService service;

    @BeforeEach
    void setUp() {
        service = new ReturnsService();
    }

    /** Builds the exact PDF example request. */
    private ReturnsRequest pdfExample() {
        var transactions = List.of(
                tx("2023-02-28 15:49:20", 375),
                tx("2023-07-01 21:59:00", 620),
                tx("2023-10-12 20:15:30", 250),
                tx("2023-12-17 08:09:45", 480),
                tx("2023-12-18 08:09:45", -10)  // negative → skipped
        );
        var q = List.of(new Q(0.0, td("2023-07-01 00:00:00", "2023-07-31 23:59:59")));
        var p = List.of(new P(25.0, td("2023-10-01 08:00:00", "2023-12-31 19:59:59")));
        var k = List.of(
                new K(td("2023-01-01 00:00:00", "2023-12-31 23:59:59")),
                new K(td("2023-03-01 00:00:00", "2023-11-30 23:59:59"))
        );
        return new ReturnsRequest(29, 50000.0, 5.5, q, p, k, transactions);
    }

    @Nested
    @DisplayName("NPS returns (7.11%)")
    class NpsTests {

        @Test
        @DisplayName("PDF example — totals and savings match")
        void pdfExampleNps() {
            ReturnsResponse res = service.calculateNpsReturns(pdfExample());

            assertEquals(1725.0, res.totalTransactionAmount(), 0.01);
            assertEquals(1900.0, res.totalCeiling(), 0.01);
            assertEquals(2, res.savingsByDates().size());

            Saving k0 = res.savingsByDates().get(0);  // Jan-Dec
            assertEquals(145.0, k0.amount(), 0.01);
            assertEquals(86.88, k0.profit(), 0.5);
            assertEquals(0.0, k0.taxBenefit(), 0.01);

            Saving k1 = res.savingsByDates().get(1);  // Mar-Nov
            assertEquals(75.0, k1.amount(), 0.01);
            assertEquals(44.94, k1.profit(), 0.5);
        }

        @Test
        @DisplayName("high salary triggers tax benefit")
        void taxBenefitTriggered() {
            var tx = List.of(tx("2023-06-15 12:00:00", 1450));
            var k = List.of(new K(td("2023-01-01 00:00:00", "2023-12-31 23:59:59")));
            var req = new ReturnsRequest(35, 100000.0, 6.0, List.of(), List.of(), k, tx);

            ReturnsResponse res = service.calculateNpsReturns(req);

            // remnant = 1500 - 1450 = 50, income = 12L → in 15% slab
            Saving s = res.savingsByDates().getFirst();
            assertEquals(50.0, s.amount(), 0.01);
            assertTrue(s.taxBenefit() > 0, "Tax benefit should be > 0 for 12L income");
        }

        @Test
        @DisplayName("income below 7L has zero tax benefit")
        void zeroTaxBenefitBelowSlab() {
            var tx = List.of(tx("2023-06-15 12:00:00", 99));
            var k = List.of(new K(td("2023-01-01 00:00:00", "2023-12-31 23:59:59")));
            var req = new ReturnsRequest(55, 30000.0, 5.0, List.of(), List.of(), k, tx);

            ReturnsResponse res = service.calculateNpsReturns(req);

            assertEquals(0.0, res.savingsByDates().getFirst().taxBenefit(), 0.01);
        }
    }

    @Nested
    @DisplayName("Index fund returns (14.49%)")
    class IndexTests {

        @Test
        @DisplayName("PDF example — higher profit, zero tax benefit")
        void pdfExampleIndex() {
            ReturnsResponse res = service.calculateIndexReturns(pdfExample());

            assertEquals(1725.0, res.totalTransactionAmount(), 0.01);
            assertEquals(1900.0, res.totalCeiling(), 0.01);

            Saving k0 = res.savingsByDates().get(0);
            assertEquals(145.0, k0.amount(), 0.01);
            assertTrue(k0.profit() > 1000, "Index profit should be much higher than NPS");
            assertEquals(0.0, k0.taxBenefit(), 0.01);
        }

        @Test
        @DisplayName("tax benefit is always zero for index")
        void alwaysZeroTaxBenefit() {
            var tx = List.of(tx("2023-06-15 12:00:00", 1450));
            var k = List.of(new K(td("2023-01-01 00:00:00", "2023-12-31 23:59:59")));
            var req = new ReturnsRequest(35, 100000.0, 6.0, List.of(), List.of(), k, tx);

            ReturnsResponse res = service.calculateIndexReturns(req);
            assertEquals(0.0, res.savingsByDates().getFirst().taxBenefit(), 0.01);
        }
    }

    @Nested
    @DisplayName("Q/P period rules")
    class PeriodRuleTests {

        @Test
        @DisplayName("Q period replaces remnant with fixed amount")
        void qPeriodOverride() {
            var tx = List.of(tx("2023-07-15 12:00:00", 250));  // remnant = 50
            var q = List.of(new Q(10.0, td("2023-07-01 00:00:00", "2023-07-31 23:59:59")));
            var k = List.of(new K(td("2023-01-01 00:00:00", "2023-12-31 23:59:59")));
            var req = new ReturnsRequest(30, 50000.0, 5.0, q, List.of(), k, tx);

            Saving s = service.calculateNpsReturns(req).savingsByDates().getFirst();
            assertEquals(10.0, s.amount(), 0.01);  // fixed=10 overrides remnant=50
        }

        @Test
        @DisplayName("overlapping Q periods — latest start wins")
        void qLatestStartWins() {
            var tx = List.of(tx("2023-07-15 12:00:00", 250));
            var q = List.of(
                    new Q(100.0, td("2023-06-01 00:00:00", "2023-08-31 23:59:59")),  // earlier start
                    new Q(5.0, td("2023-07-01 00:00:00", "2023-07-31 23:59:59"))     // later start → wins
            );
            var k = List.of(new K(td("2023-01-01 00:00:00", "2023-12-31 23:59:59")));
            var req = new ReturnsRequest(30, 50000.0, 5.0, q, List.of(), k, tx);

            assertEquals(5.0, service.calculateNpsReturns(req).savingsByDates().getFirst().amount(), 0.01);
        }

        @Test
        @DisplayName("P periods add extra to remnant")
        void pPeriodsAddExtras() {
            var tx = List.of(tx("2023-07-15 12:00:00", 250));  // remnant = 50
            var p = List.of(
                    new P(10.0, td("2023-07-01 00:00:00", "2023-07-31 23:59:59")),
                    new P(15.0, td("2023-07-01 00:00:00", "2023-08-31 23:59:59"))
            );
            var k = List.of(new K(td("2023-01-01 00:00:00", "2023-12-31 23:59:59")));
            var req = new ReturnsRequest(30, 50000.0, 5.0, List.of(), p, k, tx);

            // remnant 50 + 10 + 15 = 75
            assertEquals(75.0, service.calculateNpsReturns(req).savingsByDates().getFirst().amount(), 0.01);
        }

        @Test
        @DisplayName("Q then P — both applied in sequence")
        void qThenP() {
            var tx = List.of(tx("2023-07-15 12:00:00", 250));  // base remnant = 50
            var q = List.of(new Q(0.0, td("2023-07-01 00:00:00", "2023-07-31 23:59:59")));  // → 0
            var p = List.of(new P(25.0, td("2023-07-01 00:00:00", "2023-07-31 23:59:59"))); // → 0 + 25 = 25
            var k = List.of(new K(td("2023-01-01 00:00:00", "2023-12-31 23:59:59")));
            var req = new ReturnsRequest(30, 50000.0, 5.0, q, p, k, tx);

            assertEquals(25.0, service.calculateNpsReturns(req).savingsByDates().getFirst().amount(), 0.01);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("age >= 60 uses minimum 5-year investment period")
        void ageAboveRetirement() {
            var tx = List.of(tx("2023-06-15 12:00:00", 99));
            var k = List.of(new K(td("2023-01-01 00:00:00", "2023-12-31 23:59:59")));
            var req = new ReturnsRequest(65, 30000.0, 5.0, List.of(), List.of(), k, tx);

            ReturnsResponse res = service.calculateNpsReturns(req);
            assertTrue(res.savingsByDates().getFirst().profit() > 0);
        }

        @Test
        @DisplayName("negative transactions are silently skipped")
        void negativeTransactionsSkipped() {
            var tx = List.of(
                    tx("2023-01-01 10:00:00", 250),
                    tx("2023-02-01 10:00:00", -50)
            );
            var k = List.of(new K(td("2023-01-01 00:00:00", "2023-12-31 23:59:59")));
            var req = new ReturnsRequest(30, 50000.0, 5.0, List.of(), List.of(), k, tx);

            ReturnsResponse res = service.calculateNpsReturns(req);
            assertEquals(250.0, res.totalTransactionAmount(), 0.01);  // only the 250
        }

        @Test
        @DisplayName("transaction outside K period does not contribute")
        void outsideKPeriod() {
            var tx = List.of(tx("2023-06-15 12:00:00", 250));
            var k = List.of(new K(td("2023-01-01 00:00:00", "2023-03-31 23:59:59")));
            var req = new ReturnsRequest(30, 50000.0, 5.0, List.of(), List.of(), k, tx);

            assertEquals(0.0, service.calculateNpsReturns(req).savingsByDates().getFirst().amount(), 0.01);
        }
    }

    // helpers

    private static TransactionInput tx(String datetime, double amount) {
        return new TransactionInput(LocalDateTime.parse(datetime.replace(" ", "T")), amount);
    }

    private static TemporalData td(String start, String end) {
        return new TemporalData(
                LocalDateTime.parse(start.replace(" ", "T")),
                LocalDateTime.parse(end.replace(" ", "T"))
        );
    }
}
