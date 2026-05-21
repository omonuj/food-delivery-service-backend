package com.food.ordering.system.payment.service.domain;

import com.food.ordering.system.domain.valueobject.CustomerId;
import com.food.ordering.system.domain.valueobject.Money;
import com.food.ordering.system.domain.valueobject.OrderId;
import com.food.ordering.system.domain.valueobject.PaymentStatus;
import com.food.ordering.system.payment.service.domain.entity.CreditEntry;
import com.food.ordering.system.payment.service.domain.entity.CreditHistory;
import com.food.ordering.system.payment.service.domain.entity.Payment;
import com.food.ordering.system.payment.service.domain.event.PaymentCancelledEvent;
import com.food.ordering.system.payment.service.domain.event.PaymentCompletedEvent;
import com.food.ordering.system.payment.service.domain.event.PaymentEvent;
import com.food.ordering.system.payment.service.domain.event.PaymentFailedEvent;
import com.food.ordering.system.payment.service.domain.valueobject.CreditEntryId;
import com.food.ordering.system.payment.service.domain.valueobject.CreditHistoryId;
import com.food.ordering.system.payment.service.domain.valueobject.PaymentId;
import com.food.ordering.system.payment.service.domain.valueobject.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentDomainServiceImplTest {

    private PaymentDomainService service;
    private CustomerId customerId;

    @BeforeEach
    void setUp() {
        service = new PaymentDomainServiceImpl();
        customerId = new CustomerId(UUID.randomUUID());
    }

    private Payment payment(String price) {
        return Payment.builder()
                .paymentId(new PaymentId(UUID.randomUUID()))
                .orderId(new OrderId(UUID.randomUUID()))
                .customerId(customerId)
                .price(new Money(new BigDecimal(price)))
                .build();
    }

    private CreditEntry creditEntry(String amount) {
        return CreditEntry.builder()
                .creditEntryId(new CreditEntryId(UUID.randomUUID()))
                .customerId(customerId)
                .totalCreditAmount(new Money(new BigDecimal(amount)))
                .build();
    }

    private CreditHistory creditHistory(String amount, TransactionType type) {
        return CreditHistory.builder()
                .creditHistoryId(new CreditHistoryId(UUID.randomUUID()))
                .customerId(customerId)
                .amount(new Money(new BigDecimal(amount)))
                .transactionType(type)
                .build();
    }

    @Test
    void initiatePaymentSucceedsWithSufficientCredit() {
        Payment payment = payment("50.00");
        CreditEntry entry = creditEntry("100.00");
        List<CreditHistory> histories = new ArrayList<>(List.of(creditHistory("100.00", TransactionType.CREDIT)));
        List<String> failures = new ArrayList<>();

        PaymentEvent event = service.validateAndInitiatePayment(payment, entry, histories, failures);

        assertInstanceOf(PaymentCompletedEvent.class, event);
        assertEquals(PaymentStatus.COMPLETED, payment.getPaymentStatus());
        assertTrue(failures.isEmpty());
        // credit debited: 100 - 50 = 50
        assertEquals(new BigDecimal("50.00"), entry.getTotalCreditAmount().getAmount());
    }

    @Test
    void initiatePaymentFailsWhenCreditInsufficient() {
        Payment payment = payment("150.00");
        CreditEntry entry = creditEntry("100.00");
        List<CreditHistory> histories = new ArrayList<>(List.of(creditHistory("100.00", TransactionType.CREDIT)));
        List<String> failures = new ArrayList<>();

        PaymentEvent event = service.validateAndInitiatePayment(payment, entry, histories, failures);

        assertInstanceOf(PaymentFailedEvent.class, event);
        assertEquals(PaymentStatus.FAILED, payment.getPaymentStatus());
        assertTrue(failures.stream().anyMatch(m -> m.contains("enough credit")));
    }

    @Test
    void initiatePaymentFailsWhenHistoryLedgerInconsistent() {
        Payment payment = payment("50.00");
        // credit entry claims 500 but history only supports 100 -> ledger invariant broken
        CreditEntry entry = creditEntry("500.00");
        List<CreditHistory> histories = new ArrayList<>(List.of(creditHistory("100.00", TransactionType.CREDIT)));
        List<String> failures = new ArrayList<>();

        PaymentEvent event = service.validateAndInitiatePayment(payment, entry, histories, failures);

        assertInstanceOf(PaymentFailedEvent.class, event);
        assertTrue(failures.stream().anyMatch(m -> m.contains("Credit history total")));
    }

    @Test
    void cancelPaymentRefundsCreditAndCompletes() {
        Payment payment = payment("50.00");
        CreditEntry entry = creditEntry("50.00");
        List<CreditHistory> histories = new ArrayList<>(List.of(
                creditHistory("100.00", TransactionType.CREDIT),
                creditHistory("50.00", TransactionType.DEBIT)));
        List<String> failures = new ArrayList<>();

        PaymentEvent event = service.validateAndCancelPayment(payment, entry, histories, failures);

        assertInstanceOf(PaymentCancelledEvent.class, event);
        assertEquals(PaymentStatus.CANCELLED, payment.getPaymentStatus());
        // refunded: 50 + 50 = 100
        assertEquals(new BigDecimal("100.00"), entry.getTotalCreditAmount().getAmount());
        assertTrue(failures.isEmpty());
    }

    @Test
    void newCustomerWithZeroCreditAndNoHistoryPassesLedgerCheck() {
        // Regression guard for Money.ZERO scale: an empty debit history sums to a scale-normalized zero.
        Payment payment = payment("0.00");
        CreditEntry entry = creditEntry("0.00");
        List<CreditHistory> histories = new ArrayList<>(List.of(creditHistory("0.00", TransactionType.CREDIT)));
        List<String> failures = new ArrayList<>();

        service.validateAndInitiatePayment(payment, entry, histories, failures);

        assertTrue(failures.stream().noneMatch(m -> m.contains("Credit history total")),
                "Ledger equality check should not fail for a zero-credit customer");
    }
}
