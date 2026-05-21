package com.food.ordering.system.restaurant.service.domain.entity;

import com.food.ordering.system.domain.valueobject.Money;
import com.food.ordering.system.domain.valueobject.OrderApprovalStatus;
import com.food.ordering.system.domain.valueobject.OrderId;
import com.food.ordering.system.domain.valueobject.OrderStatus;
import com.food.ordering.system.domain.valueobject.ProductId;
import com.food.ordering.system.domain.valueobject.RestaurantId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestaurantTest {

    private Product product(String price, int quantity, boolean available) {
        return Product.builder()
                .productId(new ProductId(UUID.randomUUID()))
                .name("Item")
                .price(new Money(new BigDecimal(price)))
                .quantity(quantity)
                .available(available)
                .build();
    }

    private Restaurant restaurant(OrderStatus status, String total, boolean active, List<Product> products) {
        OrderDetail detail = OrderDetail.builder()
                .orderId(new OrderId(UUID.randomUUID()))
                .orderStatus(status)
                .totalAmount(new Money(new BigDecimal(total)))
                .products(products)
                .build();
        return Restaurant.builder()
                .restaurantId(new RestaurantId(UUID.randomUUID()))
                .active(active)
                .orderDetail(detail)
                .build();
    }

    @Test
    void validateOrderProducesNoFailuresForPaidOrderWithMatchingTotal() {
        Restaurant restaurant = restaurant(OrderStatus.PAID, "20.00", true,
                List.of(product("10.00", 2, true)));
        List<String> failureMessages = new ArrayList<>();

        restaurant.validateOrder(failureMessages);

        assertTrue(failureMessages.isEmpty(), "Expected no failures but got: " + failureMessages);
    }

    @Test
    void validateOrderFlagsUnpaidOrder() {
        Restaurant restaurant = restaurant(OrderStatus.PENDING, "20.00", true,
                List.of(product("10.00", 2, true)));
        List<String> failureMessages = new ArrayList<>();

        restaurant.validateOrder(failureMessages);

        assertTrue(failureMessages.stream().anyMatch(m -> m.contains("Payment is not completed")));
    }

    @Test
    void validateOrderFlagsUnavailableProduct() {
        Restaurant restaurant = restaurant(OrderStatus.PAID, "10.00", true,
                List.of(product("10.00", 1, false)));
        List<String> failureMessages = new ArrayList<>();

        restaurant.validateOrder(failureMessages);

        assertTrue(failureMessages.stream().anyMatch(m -> m.contains("is not available")));
    }

    @Test
    void validateOrderFlagsIncorrectTotal() {
        Restaurant restaurant = restaurant(OrderStatus.PAID, "99.00", true,
                List.of(product("10.00", 2, true)));
        List<String> failureMessages = new ArrayList<>();

        restaurant.validateOrder(failureMessages);

        assertTrue(failureMessages.stream().anyMatch(m -> m.contains("Price total is not correct")));
    }

    @Test
    void validateOrderSumsMultipleProducts() {
        Restaurant restaurant = restaurant(OrderStatus.PAID, "35.00", true,
                List.of(product("10.00", 2, true), product("15.00", 1, true)));
        List<String> failureMessages = new ArrayList<>();

        restaurant.validateOrder(failureMessages);

        assertTrue(failureMessages.isEmpty(), "Expected no failures but got: " + failureMessages);
    }

    @Test
    void constructOrderApprovalBuildsApprovalWithGivenStatus() {
        Restaurant restaurant = restaurant(OrderStatus.PAID, "20.00", true,
                List.of(product("10.00", 2, true)));

        restaurant.constructOrderApproval(OrderApprovalStatus.APPROVED);

        assertNotNull(restaurant.getOrderApproval());
        assertEquals(OrderApprovalStatus.APPROVED, restaurant.getOrderApproval().getApprovalStatus());
        assertEquals(restaurant.getId(), restaurant.getOrderApproval().getRestaurantId());
    }
}
