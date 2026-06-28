package com.example.backend.repository;

import com.example.backend.domain.CustomerOrder;
import com.example.backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    Optional<CustomerOrder> findByOrderNumber(String orderNumber);

    List<CustomerOrder> findByUserAndHiddenByCustomerFalseOrderByCreatedAtDesc(User user);
}