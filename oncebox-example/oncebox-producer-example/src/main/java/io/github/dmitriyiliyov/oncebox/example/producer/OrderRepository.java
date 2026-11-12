package io.github.dmitriyiliyov.oncebox.example.producer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository of orders, saved in the same transaction as the outbox events they publish.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> { }
