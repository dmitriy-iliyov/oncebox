package io.github.dmitriyiliyov.oncebox.example.producer;

import io.github.dmitriyiliyov.oncebox.aop.OutboxPublish;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxPublisher;
import io.github.dmitriyiliyov.oncebox.example.shared.OrderCreateDto;
import io.github.dmitriyiliyov.oncebox.example.shared.OrderDto;
import io.github.dmitriyiliyov.oncebox.example.shared.OrderUpdateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Example service demonstrating different approaches to publish outbox events.
 * <p>
 * This service showcases three ways to integrate with the Outbox pattern:
 * <ul>
 *   <li><code>@OutboxPublish</code> - declarative approach for simple cases where return value is the event payload</li>
 *   <li><code>@OutboxPublish with SpEL</code> - declarative approach with custom payload using Spring Expression Language</li>
 *   <li><code>OutboxPublisher</code> - programmatic approach for conditional publishing or custom payload construction</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final OutboxPublisher outboxPublisher;
    private final PriceService priceService;

    /**
     * Creates a new order using <code>@OutboxPublish</code>.
     * <p>
     * The return value (OrderDto) is automatically used as the event payload.
     * This is the simplest approach when the method's return value is exactly what you want to publish.
     * <p>
     * Event flow:
     * <ol>
     *   <li>Order is saved to database</li>
     *   <li>OrderDto is returned</li>
     *   <li>@OutboxEvent intercepts and saves event to outbox table (same transaction)</li>
     *   <li>Background job publishes event to Kafka topic</li>
     * </ol>
     *
     * @param dto order creation data
     * @return order data
     */
    @Transactional
    @OutboxPublish(eventType = "create-order")
    public OrderDto save(OrderCreateDto dto) {
        Order order = mapper.toEntity(dto);
        order.setAmount(priceService.countAmount(order.getItemIds()));
        return mapper.toDto(repository.save(order));
    }

    /**
     * Creates multiple orders using <code>@OutboxPublish</code> for batch processing.
     * <p>
     * Events saved to outbox table as 'create-order' type that will be sent to specified
     * in .properties/.yaml file topic.
     *
     * @param dtoList list of orders to create
     * @return list of order data
     */
    @Transactional
    @OutboxPublish(eventType = "create-order")
    public List<OrderDto> saveBatch(List<OrderCreateDto> dtoList) {
        List<Order> orders = mapper.toEntityList(dtoList);
        orders.forEach(order -> order.setAmount(priceService.countAmount(order.getItemIds())));
        return mapper.toDtoList(repository.saveAll(orders));
    }

    /**
     * Updates an order using <code>OutboxPublisher</code> for conditional event publishing.
     * <p>
     * This approach gives you full control over when and what to save to outbox table.
     * Use this when:
     * <ul>
     *   <li>Event should be published conditionally</li>
     *   <li>Event payload differs from return value</li>
     *   <li>Complex business logic determines what to publish</li>
     * </ul>
     * <p>
     * Event flow:
     * <ol>
     *   <li>Order is updated in database (if itemIds changed)</li>
     *   <li>OutboxPublisher.publish() explicitly saves event to outbox table (same transaction)</li>
     *   <li>Background job publishes event to Kafka topic</li>
     * </ol>
     *
     * @param dto order update data
     * @return order data
     */
    @Transactional
    public OrderDto update(OrderUpdateDto dto) {
        Order order = repository.findById(dto.getId()).orElseThrow(RuntimeException::new);
        String newIds = dto.getItemIds();
        order.setItemIds(newIds);
        order.setAmount(priceService.countAmount(order.getItemIds()));
        order = repository.save(order);
        outboxPublisher.publish("update-order", mapper.toDto(order));
        return mapper.toDto(order);
    }

    /**
     * Updates multiple orders using <code>OutboxPublisher</code> for conditional event publishing.
     * <p>
     * Demonstrates filtering updated orders and saving to outbox table only changed entities.
     * This is more efficient than publishing all orders when only some were actually updated.
     *
     * @param dtoList list of order updates
     * @return list of order data
     */
    @Transactional
    public List<OrderDto> updateBatch(List<OrderUpdateDto> dtoList) {
        Map<Long, OrderUpdateDto> dtoMap = dtoList
                .stream()
                .collect(Collectors.toMap(OrderUpdateDto::getId, Function.identity()));
        List<Order> orders = repository.findAllById(dtoMap.keySet());
        orders.forEach(order -> {
            OrderUpdateDto dto = dtoMap.get(order.getId());
            order.setItemIds(dto.getItemIds());
            order.setAmount(priceService.countAmount(dto.getItemIds()));
        });
        repository.saveAll(orders);
        outboxPublisher.publish("update-order", mapper.toDtoList(orders));
        return mapper.toDtoList(orders);
    }

    /**
     * Deletes an order using <code>@OutboxPublish with SpEL</code>.
     * <p>
     * Uses SpEL to specify custom payload.
     * The expression "#id" references the method parameter.
     * <p>
     * This approach is useful when:
     * <ul>
     *   <li>Method returns void or something not suitable as event payload</li>
     *   <li>You want to publish method parameter(s) as payload</li>
     * </ul>
     * <p>
     * Event saved to outbox table as 'delete-order' type that will be sent to specified in .properties/.yaml file topic.
     *
     * @param id order id to delete
     */
    @Transactional
    @OutboxPublish(eventType = "delete-order", payload = "#id")
    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * Deletes multiple orders using <code>@OutboxPublish with SpEL</code>.
     * <p>
     * The expression "#ids" publishes the entire list of ids as event payload.
     * Events saved to outbox table as 'delete-order' type
     * that will be sent to specified in .properties/.yaml file topic.
     *
     * @param ids list of order ids to delete
     */
    @Transactional
    @OutboxPublish(eventType = "delete-order", payload = "#ids")
    public void deleteBatch(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    public List<OrderDto> findAll(Pageable pageable) {
        return repository.findAll(pageable)
                .stream()
                .map(mapper::toDto)
                .toList();
    }
}