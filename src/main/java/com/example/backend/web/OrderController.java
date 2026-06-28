package com.example.backend.web;

import com.example.backend.dto.OrderDto;
import com.example.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDto.CreateOrderResponse> createOrder(
            @RequestBody OrderDto.CreateOrderRequest request
    ) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDto.OrderResponse> getOrder(
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @GetMapping("/my-orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrderDto.OrderResponse>> getMyOrders() {
        return ResponseEntity.ok(orderService.getMyOrders());
    }

    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDto.OrderResponse> cancelOrder(
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(orderService.cancelOrder(orderId));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderDto.OrderResponse>> getAllOrdersForAdmin() {
        return ResponseEntity.ok(orderService.getAllOrdersForAdmin());
    }

    @PutMapping("/admin/{orderId}/dispatch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDto.OrderResponse> dispatchOrder(
            @PathVariable Long orderId,
            @RequestBody OrderDto.DispatchRequest request
    ) {
        return ResponseEntity.ok(orderService.dispatchOrder(orderId, request));
    }

    @PatchMapping("/{orderId}/hide")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> hideOrderFromCustomer(
            @PathVariable Long orderId
    ) {
        orderService.hideOrderFromCustomer(orderId);
        return ResponseEntity.noContent().build();
    }
}