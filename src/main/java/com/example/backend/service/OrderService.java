package com.example.backend.service;

import com.example.backend.domain.*;
import com.example.backend.dto.OrderDto;
import com.example.backend.repository.CustomerOrderRepository;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CustomerOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private static final BigDecimal DELIVERY_CHARGE = BigDecimal.valueOf(1000);

    @Transactional
    public OrderDto.CreateOrderResponse createOrder(OrderDto.CreateOrderRequest request) {
        User user = getCurrentUser();

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        if (request.getDeliveryAddress() == null) {
            throw new RuntimeException("Delivery address is required");
        }

        CustomerOrder order = new CustomerOrder();
        order.setUser(user);
        order.setOrderNumber(generateOrderNumber());

        var address = request.getDeliveryAddress();

        order.setDeliveryFullName(address.getFullName());
        order.setDeliveryPhone(address.getPhone());
        order.setDeliveryAddressLine1(address.getAddressLine1());
        order.setDeliveryAddressLine2(address.getAddressLine2());
        order.setDeliveryCity(address.getCity());
        order.setDeliveryDistrict(address.getDistrict());
        order.setDeliveryPostalCode(address.getPostalCode());

        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderDto.CartItemRequest cartItem : request.getItems()) {
            if (cartItem.getQuantity() == null || cartItem.getQuantity() <= 0) {
                throw new RuntimeException("Invalid quantity");
            }

            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            if (product.getType() != ProductType.SALE) {
                throw new RuntimeException("Only sale products can be purchased");
            }

            if (product.getAvailableUnits() < cartItem.getQuantity()) {
                throw new RuntimeException(product.getProductName() + " does not have enough stock");
            }

            BigDecimal unitPrice = BigDecimal.valueOf(product.getPrice());
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            CustomerOrderItem item = new CustomerOrderItem();
            item.setOrder(order);
            item.setProductDbId(product.getId());
            item.setProductId(product.getProductId());
            item.setProductName(product.getProductName());
            item.setImageUrl(product.getImageUrl());
            item.setUnitPrice(unitPrice);
            item.setQuantity(cartItem.getQuantity());
            item.setLineTotal(lineTotal);

            order.getItems().add(item);
            subtotal = subtotal.add(lineTotal);
        }

        order.setSubtotal(subtotal);
        order.setDeliveryCharge(DELIVERY_CHARGE);
        order.setTotalAmount(subtotal.add(DELIVERY_CHARGE));

        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setPaymentGateway("PAYHERE_SANDBOX_LATER");
        order.setPaymentMethod("CARD");

        CustomerOrder savedOrder = orderRepository.save(order);

        OrderDto.CreateOrderResponse response = new OrderDto.CreateOrderResponse();
        response.setOrderId(savedOrder.getId());
        response.setOrderNumber(savedOrder.getOrderNumber());
        response.setTotalAmount(savedOrder.getTotalAmount());
        response.setOrderStatus(savedOrder.getOrderStatus().name());
        response.setPaymentStatus(savedOrder.getPaymentStatus().name());

        return response;
    }

    @Transactional(readOnly = true)
    public OrderDto.OrderResponse getOrderById(Long orderId) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDto.OrderResponse> getMyOrders() {
        User user = getCurrentUser();

        return orderRepository.findByUserAndHiddenByCustomerFalseOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderDto.OrderResponse> getAllOrdersForAdmin() {
        return orderRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrderDto.OrderResponse cancelOrder(Long orderId) {
        User user = getCurrentUser();

        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You are not allowed to cancel this order");
        }

        if (order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new RuntimeException("Only pending payment orders can be cancelled");
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.CANCELLED);

        CustomerOrder savedOrder = orderRepository.save(order);

        return toResponse(savedOrder);
    }

    @Transactional
    public void hideOrderFromCustomer(Long orderId) {
        User user = getCurrentUser();

        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You are not allowed to remove this order");
        }

        if (order.getOrderStatus() != OrderStatus.CANCELLED) {
            throw new RuntimeException("Only cancelled orders can be removed from history");
        }

        order.setHiddenByCustomer(true);
        orderRepository.save(order);
    }

    @Transactional
    public OrderDto.OrderResponse dispatchOrder(Long orderId, OrderDto.DispatchRequest request) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Cancelled orders cannot be dispatched");
        }

        if (order.getOrderStatus() == OrderStatus.DISPATCHED) {
            throw new RuntimeException("Order has already been dispatched");
        }

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new RuntimeException("Order cannot be dispatched before payment is completed");
        }

        if (order.getOrderStatus() != OrderStatus.PROCESSING) {
            throw new RuntimeException("Only processing orders can be dispatched");
        }

        if (request.getCourierName() == null || request.getCourierName().isBlank()) {
            throw new RuntimeException("Courier service is required");
        }

        if (request.getTrackingNumber() == null || request.getTrackingNumber().isBlank()) {
            throw new RuntimeException("Tracking number is required");
        }

        order.setCourierName(request.getCourierName().trim());
        order.setTrackingNumber(request.getTrackingNumber().trim());
        order.setOrderStatus(OrderStatus.DISPATCHED);
        order.setDispatchedAt(Instant.now());

        CustomerOrder savedOrder = orderRepository.save(order);

        User user = savedOrder.getUser();

        emailService.sendDispatchEmail(
                user.getEmail(),
                user.getFirstName() + " " + user.getLastName(),
                savedOrder.getOrderNumber(),
                savedOrder.getCourierName(),
                savedOrder.getTrackingNumber()
        );

        return toResponse(savedOrder);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Logged user not found"));
    }

    private String generateOrderNumber() {
        String date = DateTimeFormatter.ofPattern("yyyyMMdd")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());

        long count = orderRepository.count() + 1;

        return "RKG-ORD-" + date + "-" + String.format("%04d", count);
    }

    private OrderDto.OrderResponse toResponse(CustomerOrder order) {
        OrderDto.OrderResponse response = new OrderDto.OrderResponse();

        response.setOrderId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setCustomerName(order.getUser().getFirstName() + " " + order.getUser().getLastName());
        response.setCustomerEmail(order.getUser().getEmail());

        response.setSubtotal(order.getSubtotal());
        response.setDeliveryCharge(order.getDeliveryCharge());
        response.setTotalAmount(order.getTotalAmount());

        response.setOrderStatus(order.getOrderStatus().name());
        response.setPaymentStatus(order.getPaymentStatus().name());

        response.setCourierName(order.getCourierName());
        response.setTrackingNumber(order.getTrackingNumber());

        response.setDeliveryFullName(order.getDeliveryFullName());
        response.setDeliveryPhone(order.getDeliveryPhone());
        response.setDeliveryAddressLine1(order.getDeliveryAddressLine1());
        response.setDeliveryAddressLine2(order.getDeliveryAddressLine2());
        response.setDeliveryCity(order.getDeliveryCity());
        response.setDeliveryDistrict(order.getDeliveryDistrict());
        response.setDeliveryPostalCode(order.getDeliveryPostalCode());

        response.setCreatedAt(order.getCreatedAt());
        response.setPaidAt(order.getPaidAt());
        response.setDispatchedAt(order.getDispatchedAt());

        response.setItems(
                order.getItems()
                        .stream()
                        .map(item -> {
                            OrderDto.OrderItemResponse itemResponse = new OrderDto.OrderItemResponse();
                            itemResponse.setProductDbId(item.getProductDbId());
                            itemResponse.setProductId(item.getProductId());
                            itemResponse.setProductName(item.getProductName());
                            itemResponse.setImageUrl(item.getImageUrl());
                            itemResponse.setUnitPrice(item.getUnitPrice());
                            itemResponse.setQuantity(item.getQuantity());
                            itemResponse.setLineTotal(item.getLineTotal());
                            return itemResponse;
                        })
                        .toList()
        );

        return response;
    }
}