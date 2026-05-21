package com.food.ordering.system.order.service.domain.entity;

import com.food.ordering.system.domain.valueobject.CustomerId;
import com.food.ordering.system.domain.valueobject.Money;
import com.food.ordering.system.domain.valueobject.OrderId;
import com.food.ordering.system.domain.valueobject.OrderStatus;
import com.food.ordering.system.domain.valueobject.ProductId;
import com.food.ordering.system.domain.valueobject.RestaurantId;
import com.food.ordering.system.order.service.domain.exception.OrderDomainException;
import com.food.ordering.system.order.service.domain.valueobject.StreetAddress;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderTest {

    private Order orderWithStatus(OrderStatus status, List<String> failureMessages) {
        Money price = new Money(new BigDecimal("50.00"));
        Product product = new Product(new ProductId(UUID.randomUUID()), "Pizza", price);
        OrderItem item = OrderItem.builder()
                .product(product)
                .quantity(1)
                .price(price)
                .subTotal(price)
                .build();
        return Order.builder()
                .orderId(new OrderId(UUID.randomUUID()))
                .customerId(new CustomerId(UUID.randomUUID()))
                .restaurantId(new RestaurantId(UUID.randomUUID()))
                .deliveryAddress(new StreetAddress(UUID.randomUUID(), "1 Main St", "12345", "Lagos"))
                .price(price)
                .items(List.of(item))
                .orderStatus(status)
                .failureMessages(failureMessages)
                .build();
    }

    // --- initializeOrder ---

    @Test
    void initializeOrderSetsPendingStatusTrackingIdAndItemIds() {
        Money price = new Money(new BigDecimal("50.00"));
        Product product = new Product(new ProductId(UUID.randomUUID()), "Pizza", price);
        OrderItem item = OrderItem.builder()
                .product(product).quantity(1).price(price).subTotal(price).build();
        Order order = Order.builder()
                .customerId(new CustomerId(UUID.randomUUID()))
                .restaurantId(new RestaurantId(UUID.randomUUID()))
                .deliveryAddress(new StreetAddress(UUID.randomUUID(), "1 Main St", "12345", "Lagos"))
                .price(price)
                .items(List.of(item))
                .build();

        order.initializeOrder();

        assertEquals(OrderStatus.PENDING, order.getOrderStatus());
        assertNotNull(order.getId());
        assertNotNull(order.getTrackingId());
        assertNotNull(order.getItems().get(0).getId());
        assertEquals(order.getId(), order.getItems().get(0).getOrderId());
    }

    // --- pay() ---

    @Test
    void payMovesPendingToPaid() {
        Order order = orderWithStatus(OrderStatus.PENDING, null);
        order.pay();
        assertEquals(OrderStatus.PAID, order.getOrderStatus());
    }

    @Test
    void payFailsWhenNotPending() {
        Order order = orderWithStatus(OrderStatus.PAID, null);
        assertThrows(OrderDomainException.class, order::pay);
    }

    // --- approve() ---

    @Test
    void approveMovesPaidToApproved() {
        Order order = orderWithStatus(OrderStatus.PAID, null);
        order.approve();
        assertEquals(OrderStatus.APPROVED, order.getOrderStatus());
    }

    @Test
    void approveFailsWhenNotPaid() {
        Order order = orderWithStatus(OrderStatus.PENDING, null);
        assertThrows(OrderDomainException.class, order::approve);
    }

    // --- initCancel() ---

    @Test
    void initCancelMovesPaidToCancelling() {
        Order order = orderWithStatus(OrderStatus.PAID, new ArrayList<>());
        order.initCancel(List.of("payment failed"));
        assertEquals(OrderStatus.CANCELLING, order.getOrderStatus());
        assertTrue(order.getFailureMessages().contains("payment failed"));
    }

    @Test
    void initCancelFailsWhenNotPaid() {
        Order order = orderWithStatus(OrderStatus.PENDING, new ArrayList<>());
        assertThrows(OrderDomainException.class, () -> order.initCancel(List.of("x")));
    }

    // --- cancel() ---

    @Test
    void cancelMovesCancellingToCancelled() {
        Order order = orderWithStatus(OrderStatus.CANCELLING, new ArrayList<>());
        order.cancel(List.of("done"));
        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
    }

    @Test
    void cancelMovesPendingToCancelled() {
        Order order = orderWithStatus(OrderStatus.PENDING, new ArrayList<>());
        order.cancel(List.of("done"));
        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
    }

    @Test
    void cancelFailsWhenPaid() {
        Order order = orderWithStatus(OrderStatus.PAID, new ArrayList<>());
        assertThrows(OrderDomainException.class, () -> order.cancel(List.of("x")));
    }

    // --- updateFailureMessages ---

    @Test
    void emptyFailureMessagesAreFilteredOut() {
        Order order = orderWithStatus(OrderStatus.PAID, new ArrayList<>());
        order.initCancel(List.of("real error", ""));
        assertEquals(List.of("real error"), order.getFailureMessages());
    }

    @Test
    void cancellingWithMutableExistingListAppendsNewMessages() {
        List<String> existing = new ArrayList<>();
        existing.add("first");
        Order order = orderWithStatus(OrderStatus.CANCELLING, existing);
        order.cancel(List.of("second"));
        assertEquals(List.of("first", "second"), order.getFailureMessages());
    }

    @Test
    void validateOrderPassesForConsistentPrices() {
        Money price = new Money(new BigDecimal("50.00"));
        Product product = new Product(new ProductId(UUID.randomUUID()), "Pizza", price);
        OrderItem item = OrderItem.builder()
                .product(product).quantity(1).price(price).subTotal(price).build();
        Order order = Order.builder()
                .customerId(new CustomerId(UUID.randomUUID()))
                .restaurantId(new RestaurantId(UUID.randomUUID()))
                .deliveryAddress(new StreetAddress(UUID.randomUUID(), "1 Main St", "12345", "Lagos"))
                .price(price)
                .items(List.of(item))
                .build();
        // validateOrder must run on a fresh order (null id/status); initialization follows.
        order.validateOrder();
        order.initializeOrder();
        assertEquals(OrderStatus.PENDING, order.getOrderStatus());
    }

    @Test
    void validateOrderFailsWhenTotalPriceMismatchesItems() {
        Money itemPrice = new Money(new BigDecimal("50.00"));
        Product product = new Product(new ProductId(UUID.randomUUID()), "Pizza", itemPrice);
        OrderItem item = OrderItem.builder()
                .product(product).quantity(1).price(itemPrice).subTotal(itemPrice).build();
        Order order = Order.builder()
                .customerId(new CustomerId(UUID.randomUUID()))
                .restaurantId(new RestaurantId(UUID.randomUUID()))
                .deliveryAddress(new StreetAddress(UUID.randomUUID(), "1 Main St", "12345", "Lagos"))
                .price(new Money(new BigDecimal("99.00")))
                .items(List.of(item))
                .build();
        assertThrows(OrderDomainException.class, order::validateOrder);
    }
}
