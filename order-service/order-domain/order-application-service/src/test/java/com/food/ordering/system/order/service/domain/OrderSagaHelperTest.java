package com.food.ordering.system.order.service.domain;

import com.food.ordering.system.domain.valueobject.OrderStatus;
import com.food.ordering.system.order.service.domain.ports.output.repository.OrderRepository;
import com.food.ordering.system.saga.SagaStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderSagaHelperTest {

    private OrderSagaHelper orderSagaHelper;

    @BeforeEach
    void setUp() {
        orderSagaHelper = new OrderSagaHelper(Mockito.mock(OrderRepository.class));
    }

    @Test
    void paidMapsToProcessing() {
        assertEquals(SagaStatus.PROCESSING, orderSagaHelper.orderStatusToSagaStatus(OrderStatus.PAID));
    }

    @Test
    void approvedMapsToSucceeded() {
        assertEquals(SagaStatus.SUCCEEDED, orderSagaHelper.orderStatusToSagaStatus(OrderStatus.APPROVED));
    }

    @Test
    void cancellingMapsToCompensating() {
        assertEquals(SagaStatus.COMPENSATING, orderSagaHelper.orderStatusToSagaStatus(OrderStatus.CANCELLING));
    }

    @Test
    void cancelledMapsToCompensated() {
        assertEquals(SagaStatus.COMPENSATED, orderSagaHelper.orderStatusToSagaStatus(OrderStatus.CANCELLED));
    }

    @Test
    void pendingFallsBackToStarted() {
        assertEquals(SagaStatus.STARTED, orderSagaHelper.orderStatusToSagaStatus(OrderStatus.PENDING));
    }

    @Test
    void everyOrderStatusMapsWithoutThrowing() {
        // Guards the default branch: a newly added OrderStatus constant silently maps to STARTED.
        for (OrderStatus status : OrderStatus.values()) {
            SagaStatus mapped = orderSagaHelper.orderStatusToSagaStatus(status);
            assertEquals(true, mapped != null);
        }
    }
}
