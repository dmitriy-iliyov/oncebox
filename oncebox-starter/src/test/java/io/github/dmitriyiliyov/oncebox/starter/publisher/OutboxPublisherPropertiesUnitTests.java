package io.github.dmitriyiliyov.oncebox.starter.publisher;

import io.github.dmitriyiliyov.oncebox.starter.OutboxProperties;
import io.github.dmitriyiliyov.oncebox.starter.PollingType;
import io.github.dmitriyiliyov.oncebox.starter.TransportType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

public class OutboxPublisherPropertiesUnitTests {

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should throw when sender is null")
    public void applyDefaults_whenSenderNull_thenThrow() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        properties.setEvents(new HashMap<>());

        assertThrows(IllegalArgumentException.class, properties::applyDefaults);
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should throw when events is null")
    public void applyDefaults_whenEventsNull_thenThrow() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("beanName");
        properties.setSender(sender);

        assertThrows(IllegalArgumentException.class, properties::applyDefaults);
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should accept empty events map")
    public void applyDefaults_whenEventsEmpty_thenAccept() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("beanName");
        properties.setSender(sender);
        properties.setEvents(new HashMap<>());

        properties.applyDefaults();

        assertTrue(properties.getEvents().isEmpty());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should throw when event type is null")
    public void applyDefaults_whenEventTypeNull_thenThrow() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("beanName");
        properties.setSender(sender);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("topic");
        events.put(null, event);
        properties.setEvents(events);

        assertThrows(IllegalArgumentException.class, properties::applyDefaults);
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should throw when event type is blank")
    public void applyDefaults_whenEventTypeBlank_thenThrow() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("beanName");
        properties.setSender(sender);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("topic");
        events.put("  ", event);
        properties.setEvents(events);

        assertThrows(IllegalArgumentException.class, properties::applyDefaults);
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should applyDefaults stuckRecovery when null")
    public void applyDefaults_whenStuckRecoveryNull_thenUseDefault() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("beanName");
        properties.setSender(sender);
        properties.setEvents(new HashMap<>());

        properties.applyDefaults();

        assertNotNull(properties.getStuckRecovery());
        assertEquals(500, properties.getStuckRecovery().getBatchSize());
        assertEquals(Duration.ofMinutes(5), properties.getStuckRecovery().getMaxBatchProcessingTime());
        assertEquals(PollingType.ADAPTIVE, properties.getDefaults().getPolling().getType());
        assertEquals(Duration.ofMinutes(5), properties.getDefaults().getPolling().getInitialDelay());
        assertEquals(Duration.ofMillis(250), properties.getDefaults().getPolling().getMinFixedDelay());
        assertEquals(Duration.ofMinutes(1), properties.getDefaults().getPolling().getMaxFixedDelay());
        assertEquals(1.5, properties.getDefaults().getPolling().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should applyDefaults defaults when null")
    public void applyDefaults_whenDefaultsNull_thenUseDefault() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("beanName");
        properties.setSender(sender);
        properties.setEvents(new HashMap<>());

        properties.applyDefaults();

        assertNotNull(properties.getDefaults());
        assertEquals(200, properties.getDefaults().getBatchSize());
        assertEquals(PollingType.ADAPTIVE, properties.getDefaults().getPolling().getType());
        assertEquals(Duration.ofMinutes(5), properties.getDefaults().getPolling().getInitialDelay());
        assertEquals(Duration.ofMillis(250), properties.getDefaults().getPolling().getMinFixedDelay());
        assertEquals(Duration.ofMinutes(1), properties.getDefaults().getPolling().getMaxFixedDelay());
        assertEquals(1.5, properties.getDefaults().getPolling().getMultiplier());
        assertEquals(3, properties.getDefaults().getMaxRetries());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() with full configuration should applyDefaults all properties correctly")
    public void applyDefaults_withFullConfiguration_shouldApplyDefaultsAll() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("kafkaOutboxTemplate");
        properties.setSender(sender);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();

        OutboxPublisherProperties.EventProperties accountDelete = new OutboxPublisherProperties.EventProperties();
        accountDelete.setTopic("account-delete");
        OutboxProperties.PollingProperties polling1 = new OutboxProperties.PollingProperties();
        polling1.setType(PollingType.FIXED);
        polling1.setFixedDelay(Duration.ofSeconds(10));
        accountDelete.setPolling(polling1);
        accountDelete.setMaxRetries(1);
        events.put("account-delete", accountDelete);

        OutboxPublisherProperties.EventProperties userRegistered = new OutboxPublisherProperties.EventProperties();
        userRegistered.setTopic("user-registered");
        userRegistered.setMaxRetries(4);
        OutboxPublisherProperties.BackoffProperties backoff = new OutboxPublisherProperties.BackoffProperties();
        userRegistered.setBackoff(backoff);
        events.put("user-registered", userRegistered);

        properties.setEvents(events);

        OutboxPublisherProperties.StuckRecoveryProperties stuckRecovery = new OutboxPublisherProperties.StuckRecoveryProperties();
        stuckRecovery.setMaxBatchProcessingTime(Duration.ofMinutes(10));
        properties.setStuckRecovery(stuckRecovery);

        OutboxProperties.CleanUpProperties cleanUp = new OutboxProperties.CleanUpProperties();
        properties.setCleanUp(cleanUp);

        OutboxPublisherProperties.DlqProperties dlq = new OutboxPublisherProperties.DlqProperties();
        dlq.setEnabled(true);
        properties.setDlq(dlq);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        properties.setDefaults(defaults);

        properties.applyDefaults();

        assertEquals(TransportType.KAFKA, properties.getSender().getType());
        assertEquals("kafkaOutboxTemplate", properties.getSender().getBeanName());

        assertEquals(200, properties.getEvents().get("account-delete").getBatchSize());
        assertEquals(PollingType.FIXED, properties.getEvents().get("account-delete").getPolling().getType());
        assertEquals(Duration.ofMinutes(5), properties.getEvents().get("account-delete").getPolling().getInitialDelay());
        assertEquals(Duration.ofSeconds(10), properties.getEvents().get("account-delete").getPolling().getFixedDelay());
        assertEquals(Duration.ZERO, properties.getEvents().get("account-delete").getPolling().getMinFixedDelay());
        assertEquals(Duration.ZERO, properties.getEvents().get("account-delete").getPolling().getMaxFixedDelay());
        assertEquals(Double.NaN, properties.getEvents().get("account-delete").getPolling().getMultiplier());
        assertEquals(1, properties.getEvents().get("account-delete").getMaxRetries());

        assertEquals(200, properties.getEvents().get("user-registered").getBatchSize());
        assertEquals(PollingType.ADAPTIVE, properties.getEvents().get("user-registered").getPolling().getType());
        assertEquals(Duration.ofMinutes(5), properties.getEvents().get("user-registered").getInitialDelay());
        assertEquals(Duration.ZERO, properties.getEvents().get("user-registered").getFixedDelay());
        assertEquals(Duration.ofMillis(250), properties.getEvents().get("user-registered").getPolling().getMinFixedDelay());
        assertEquals(Duration.ofMinutes(1), properties.getEvents().get("user-registered").getPolling().getMaxFixedDelay());
        assertEquals(1.5, properties.getEvents().get("user-registered").getPolling().getMultiplier());
        assertEquals(4, properties.getEvents().get("user-registered").getMaxRetries());

        assertEquals(500, properties.getStuckRecovery().getBatchSize());
        assertEquals(Duration.ofMinutes(10), properties.getStuckRecovery().getMaxBatchProcessingTime());
        assertEquals(PollingType.ADAPTIVE, properties.getStuckRecovery().getPolling().getType());
        assertEquals(Duration.ofMinutes(5), properties.getStuckRecovery().getInitialDelay());
        assertEquals(Duration.ZERO, properties.getStuckRecovery().getFixedDelay());
        assertEquals(Duration.ofSeconds(1), properties.getStuckRecovery().getPolling().getMinFixedDelay());
        assertEquals(Duration.ofMinutes(1), properties.getStuckRecovery().getPolling().getMaxFixedDelay());
        assertEquals(4, properties.getStuckRecovery().getPolling().getMultiplier());

        assertTrue(properties.isCleanUpEnabled());
        assertEquals(500, properties.getCleanUp().getBatchSize());
        assertEquals(Duration.ofHours(24), properties.getCleanUp().getTtl());
        assertEquals(PollingType.ADAPTIVE, properties.getCleanUp().getPolling().getType());
        assertEquals(Duration.ofMinutes(5), properties.getCleanUp().getInitialDelay());
        assertEquals(Duration.ZERO, properties.getCleanUp().getFixedDelay());
        assertEquals(Duration.ofSeconds(5), properties.getCleanUp().getPolling().getMinFixedDelay());
        assertEquals(Duration.ofMinutes(1), properties.getCleanUp().getPolling().getMaxFixedDelay());
        assertEquals(2, properties.getCleanUp().getPolling().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should handle disabled backoff in defaults correctly")
    public void applyDefaults_whenBackoffDisabledInDefaults_thenApplyCorrectly() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("kafkaOutboxTemplate");
        properties.setSender(sender);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(false);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();

        OutboxPublisherProperties.EventProperties accountDelete = new OutboxPublisherProperties.EventProperties();
        accountDelete.setTopic("account-delete");
        OutboxProperties.PollingProperties polling1 = new OutboxProperties.PollingProperties();
        polling1.setFixedDelay(Duration.ofSeconds(10));
        accountDelete.setPolling(polling1);
        accountDelete.setMaxRetries(1);
        events.put("account-delete", accountDelete);

        OutboxPublisherProperties.EventProperties userRegistered = new OutboxPublisherProperties.EventProperties();
        userRegistered.setTopic("user-registered");
        userRegistered.setMaxRetries(4);
        OutboxPublisherProperties.BackoffProperties backoff = new OutboxPublisherProperties.BackoffProperties();
        userRegistered.setBackoff(backoff);
        events.put("user-registered", userRegistered);

        properties.setEvents(events);

        OutboxProperties.CleanUpProperties cleanUp = new OutboxProperties.CleanUpProperties();
        cleanUp.setEnabled(true);
        properties.setCleanUp(cleanUp);

        OutboxPublisherProperties.DlqProperties dlq = new OutboxPublisherProperties.DlqProperties();
        dlq.setEnabled(true);
        properties.setDlq(dlq);

        properties.applyDefaults();

        assertFalse(properties.getDefaults().getBackoff().isEnabled());
        assertFalse(properties.getEvents().get("account-delete").getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(0), properties.getEvents().get("account-delete").getBackoff().getDelay());
        assertEquals(1.0, properties.getEvents().get("account-delete").getBackoff().getMultiplier());

        assertTrue(properties.getEvents().get("user-registered").getBackoff().isEnabled());
        assertTrue(properties.isCleanUpEnabled());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should apply defaults to event properties with null values")
    public void applyDefaults_whenEventPropertiesNull_thenApplyDefaults() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        defaults.setBatchSize(50);
        OutboxProperties.PollingProperties defaultPolling = new OutboxProperties.PollingProperties();
        defaultPolling.setType(PollingType.FIXED);
        defaultPolling.setInitialDelay(Duration.ofSeconds(10));
        defaultPolling.setFixedDelay(Duration.ofSeconds(60));
        defaults.setPolling(defaultPolling);
        defaults.setMaxRetries(5);
        OutboxPublisherProperties.BackoffProperties backoff = new OutboxPublisherProperties.BackoffProperties();
        backoff.setEnabled(true);
        backoff.setDelay(Duration.ofSeconds(5));
        backoff.setMultiplier(3.9);
        defaults.setBackoff(backoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        events.put("test-event", event);
        properties.setEvents(events);

        properties.applyDefaults();

        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertEquals(50, resultEvent.getBatchSize());
        assertEquals(Duration.ofSeconds(10), resultEvent.getInitialDelay());
        assertEquals(Duration.ofSeconds(60), resultEvent.getFixedDelay());
        assertEquals(5, resultEvent.getMaxRetries());
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(5), resultEvent.getBackoff().getDelay());
        assertEquals(3.9, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should override defaults with event specific values")
    public void applyDefaults_whenEventPropertiesProvided_thenOverrideDefaults() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        defaults.setBatchSize(50);
        OutboxProperties.PollingProperties defaultPolling = new OutboxProperties.PollingProperties();
        defaultPolling.setType(PollingType.FIXED);
        defaultPolling.setInitialDelay(Duration.ofSeconds(10));
        defaultPolling.setFixedDelay(Duration.ofSeconds(60));
        defaults.setPolling(defaultPolling);
        defaults.setMaxRetries(5);
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(5));
        defaultBackoff.setMultiplier(3.9);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        event.setBatchSize(100);
        OutboxProperties.PollingProperties eventPolling = new OutboxProperties.PollingProperties();
        eventPolling.setInitialDelay(Duration.ofSeconds(20));
        eventPolling.setFixedDelay(Duration.ofSeconds(120));
        event.setPolling(eventPolling);
        event.setMaxRetries(10);
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setDelay(Duration.ofSeconds(15));
        eventBackoff.setMultiplier(5.4);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        properties.applyDefaults();

        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertEquals(100, resultEvent.getBatchSize());
        assertEquals(Duration.ofSeconds(20), resultEvent.getInitialDelay());
        assertEquals(Duration.ofSeconds(120), resultEvent.getFixedDelay());
        assertEquals(10, resultEvent.getMaxRetries());
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(15), resultEvent.getBackoff().getDelay());
        assertEquals(5.4, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should handle disabled backoff in event properties")
    public void applyDefaults_whenEventBackoffDisabled_thenApplyCorrectly() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        defaults.setBatchSize(50);
        OutboxProperties.PollingProperties defaultPolling = new OutboxProperties.PollingProperties();
        defaultPolling.setInitialDelay(Duration.ofSeconds(10));
        defaultPolling.setFixedDelay(Duration.ofSeconds(60));
        defaults.setPolling(defaultPolling);
        defaults.setMaxRetries(5);
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(5));
        defaultBackoff.setMultiplier(3.9);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(false);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        properties.applyDefaults();

        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertFalse(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(0), resultEvent.getBackoff().getDelay());
        assertEquals(1.0, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should use default backoff delay when event backoff delay is null")
    public void applyDefaults_whenEventBackoffDelayNull_thenUseDefaultDelay() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        OutboxProperties.PollingProperties defaultPolling = new OutboxProperties.PollingProperties();
        defaultPolling.setType(PollingType.FIXED);
        defaultPolling.setInitialDelay(Duration.ofSeconds(10));
        defaults.setPolling(defaultPolling);
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(3.9);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setMultiplier(5.4);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        properties.applyDefaults();

        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(15), resultEvent.getBackoff().getDelay());
        assertEquals(5.4, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should use event backoff delay when provided")
    public void applyDefaults_whenEventBackoffDelayProvided_thenUseEventDelay() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(3.9);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setDelay(Duration.ofSeconds(25));
        eventBackoff.setMultiplier(5.4);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        properties.applyDefaults();

        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(25), resultEvent.getBackoff().getDelay());
        assertEquals(5.4, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should use default backoff multiplier when event multiplier is zero")
    public void applyDefaults_whenEventBackoffMultiplierZero_thenUseDefaultMultiplier() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(3.9);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setDelay(Duration.ofSeconds(20));
        eventBackoff.setMultiplier(0.0);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        properties.applyDefaults();

        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(20), resultEvent.getBackoff().getDelay());
        assertEquals(3.9, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should use default backoff multiplier when event multiplier is negative")
    public void applyDefaults_whenEventBackoffMultiplierNegative_thenUseDefaultMultiplier() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(3.9);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setDelay(Duration.ofSeconds(20));
        eventBackoff.setMultiplier(-2.8);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        properties.applyDefaults();

        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(20), resultEvent.getBackoff().getDelay());
        assertEquals(3.9, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should use event backoff multiplier when it is 1")
    public void applyDefaults_whenEventBackoffMultiplierOne_thenUseEventMultiplier() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(3.9);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setDelay(Duration.ofSeconds(20));
        eventBackoff.setMultiplier(1.0);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        properties.applyDefaults();

        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(20), resultEvent.getBackoff().getDelay());
        assertEquals(1.0, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should use event backoff multiplier when greater than 1")
    public void applyDefaults_whenEventBackoffMultiplierGreaterThanOne_thenUseEventMultiplier() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(3.9);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setDelay(Duration.ofSeconds(20));
        eventBackoff.setMultiplier(7.3);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        properties.applyDefaults();

        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(20), resultEvent.getBackoff().getDelay());
        assertEquals(7.3, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should use both defaults when event backoff delay and multiplier are null")
    public void applyDefaults_whenEventBackoffDelayAndMultiplierNull_thenUseBothDefaults() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        OutboxProperties.PollingProperties defaultPolling = new OutboxProperties.PollingProperties();
        defaultPolling.setType(PollingType.FIXED);
        defaultPolling.setInitialDelay(Duration.ofSeconds(10));
        defaults.setPolling(defaultPolling);

        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(2.6);
        defaults.setBackoff(defaultBackoff);

        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        events.put("test-event", event);
        properties.setEvents(events);

        properties.applyDefaults();

        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(15), resultEvent.getBackoff().getDelay());
        assertEquals(2.6, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should use both event values when backoff delay and multiplier are provided")
    public void applyDefaults_whenEventBackoffDelayAndMultiplierProvided_thenUseBothEventValues() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(3.9);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setDelay(Duration.ofSeconds(25));
        eventBackoff.setMultiplier(6.4);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        properties.applyDefaults();

        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(25), resultEvent.getBackoff().getDelay());
        assertEquals(6.4, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should mix defaults and event values correctly")
    public void applyDefaults_whenMixingDefaultsAndEventValues_thenApplyCorrectly() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(4.0);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setDelay(Duration.ofSeconds(25));
        eventBackoff.setMultiplier(0.0);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        properties.applyDefaults();

        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(25), resultEvent.getBackoff().getDelay());
        assertEquals(4.0, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT isCleanUpEnabled() should return true when cleanUp is null")
    public void isCleanUpEnabled_whenCleanUpNull_thenReturnTrue() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);
        properties.setEvents(new HashMap<>());

        properties.applyDefaults();

        assertTrue(properties.isCleanUpEnabled());
    }

    @Test
    @DisplayName("UT isCleanUpEnabled() should return true when cleanUp enabled")
    public void isCleanUpEnabled_whenCleanUpEnabled_thenReturnTrue() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);
        properties.setEvents(new HashMap<>());

        OutboxProperties.CleanUpProperties cleanUp = new OutboxProperties.CleanUpProperties();
        cleanUp.setEnabled(true);
        properties.setCleanUp(cleanUp);

        properties.applyDefaults();

        assertTrue(properties.isCleanUpEnabled());
    }

    @Test
    @DisplayName("UT isCleanUpEnabled() should return false when cleanUp disabled")
    public void isCleanUpEnabled_whenCleanUpDisabled_thenReturnFalse() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);
        properties.setEvents(new HashMap<>());

        OutboxProperties.CleanUpProperties cleanUp = new OutboxProperties.CleanUpProperties();
        cleanUp.setEnabled(false);
        properties.setCleanUp(cleanUp);

        properties.applyDefaults();

        assertFalse(properties.isCleanUpEnabled());
    }

    @Test
    @DisplayName("UT getCleanUp() should return cleanUp properties when provided")
    public void getCleanUp_whenCleanUpProvided_thenReturnProperties() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);
        properties.setEvents(new HashMap<>());

        OutboxProperties.PollingProperties polling = new OutboxProperties.PollingProperties();
        polling.setType(PollingType.FIXED);
        polling.setInitialDelay(Duration.ofMinutes(5));
        polling.setFixedDelay(Duration.ofMillis(30));
        OutboxProperties.CleanUpProperties cleanUp = new OutboxProperties.CleanUpProperties();
        cleanUp.setEnabled(true);
        cleanUp.setBatchSize(50);
        cleanUp.setTtl(Duration.ofHours(2));
        cleanUp.setPolling(polling);
        properties.setCleanUp(cleanUp);

        properties.applyDefaults();

        assertNotNull(properties.getCleanUp());
        assertEquals(50, properties.getCleanUp().getBatchSize());
        assertEquals(Duration.ofHours(2), properties.getCleanUp().getTtl());
        assertEquals(Duration.ofMinutes(5), properties.getCleanUp().getInitialDelay());
        assertEquals(Duration.ofMillis(30), properties.getCleanUp().getFixedDelay());
    }

    @Test
    @DisplayName("UT existEventType() should return true when event exists")
    public void existEventType_whenEventExists_thenReturnTrue() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        event.setBatchSize(10);
        OutboxProperties.PollingProperties polling = new OutboxProperties.PollingProperties();
        polling.setInitialDelay(Duration.ofSeconds(5));
        polling.setFixedDelay(Duration.ofSeconds(30));
        event.setPolling(polling);
        event.setMaxRetries(3);
        events.put("test-event", event);
        properties.setEvents(events);

        properties.applyDefaults();

        assertTrue(properties.existEventType("test-event"));
    }

    @Test
    @DisplayName("UT existEventType() should return false when event does not exist")
    public void existEventType_whenEventDoesNotExist_thenReturnFalse() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        events.put("test-event", event);
        properties.setEvents(events);

        properties.applyDefaults();

        assertFalse(properties.existEventType("non-existing-event"));
    }

    @Test
    @DisplayName("UT getEvents() should return unmodifiable map")
    public void getEvents_shouldReturnUnmodifiableMap() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        events.put("test-event", event);
        properties.setEvents(events);

        properties.applyDefaults();

        OutboxPublisherProperties.EventProperties newEvent = new OutboxPublisherProperties.EventProperties();
        newEvent.setTopic("topic");
        assertThrows(UnsupportedOperationException.class,
                () -> properties.getEvents().put("new-event", newEvent)
        );
    }

    @Test
    @DisplayName("UT getSender() should return sender properties")
    public void getSender_shouldReturnSenderProperties() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("testBean");
        properties.setSender(sender);
        properties.setEvents(new HashMap<>());

        properties.applyDefaults();

        assertNotNull(properties.getSender());
        assertEquals(TransportType.KAFKA, properties.getSender().getType());
        assertEquals("testBean", properties.getSender().getBeanName());
    }

    @Test
    @DisplayName("UT setEnabled() should update enabled flag")
    public void setEnabled_shouldUpdateEnabledFlag() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        properties.setEnabled(true);

        assertTrue(properties.isEnabled());

        properties.setEnabled(false);

        assertFalse(properties.isEnabled());
    }

    @Test
    @DisplayName("UT setMetrics() should update metrics properties")
    public void setMetrics_shouldUpdateMetricsProperties() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();

        properties.setMetrics(metrics);

        assertEquals(metrics, properties.getMetrics());
    }

    @Test
    @DisplayName("UT applyDefaults() when metrics is null should create enabled metrics")
    public void applyDefaults_whenMetricsNull_shouldCreateDefaultMetrics() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);
        properties.setEvents(new HashMap<>());

        properties.applyDefaults();

        assertNotNull(properties.getMetrics());
        assertFalse(properties.getMetrics().isEnabled());
        assertNotNull(properties.getMetrics().getGauge());
        assertFalse(properties.getMetrics().getGauge().isEnabled());
    }

    @Test
    @DisplayName("UT applyDefaults() when enabled is false should set enabled to false and apply default fallbacks")
    public void applyDefaultsEnabledFalse() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        properties.setEnabled(false);

        properties.applyDefaults();

        assertFalse(properties.isEnabled());
        assertFalse(properties.getCleanUp().isEnabled());
        assertFalse(properties.getDlq().isEnabled());
        assertThat(properties.getEvents()).isEmpty();
        assertFalse(properties.getMetrics().isEnabled());
        assertNull(properties.getSender());
    }

    @Test
    @DisplayName("UT applyDefaults() when emergency timeout exceeds max batch processing time should throw naming both")
    public void applyDefaults_whenEmergencyTimeoutExceedsMaxBatchProcessingTime_shouldThrow() {
        // given
        OutboxPublisherProperties properties = propertiesWithTimeouts(Duration.ofMinutes(10), Duration.ofMinutes(5));

        // when / then
        assertThatThrownBy(properties::applyDefaults)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("'sender.emergency-timeout' (PT10M)")
                .hasMessageContaining("'stuck-recovery.max-batch-processing-time' (PT5M)");
    }

    @Test
    @DisplayName("UT applyDefaults() when emergency timeout equals max batch processing time should throw")
    public void applyDefaults_whenEmergencyTimeoutEqualsMaxBatchProcessingTime_shouldThrow() {
        // given
        OutboxPublisherProperties properties = propertiesWithTimeouts(Duration.ofMinutes(5), Duration.ofMinutes(5));

        // when / then
        assertThatThrownBy(properties::applyDefaults)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sender.emergency-timeout");
    }

    @Test
    @DisplayName("UT applyDefaults() when emergency timeout is below max batch processing time should accept")
    public void applyDefaults_whenEmergencyTimeoutBelowMaxBatchProcessingTime_shouldAccept() {
        // given
        OutboxPublisherProperties properties = propertiesWithTimeouts(Duration.ofMinutes(4), Duration.ofMinutes(5));

        // when / then
        assertThatCode(properties::applyDefaults).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("UT DlqProperties.applyDefaults() when enabled is null should disable and init empty properties")
    public void applyDefaults_dlqWhenEnabledNull_shouldDisableAndInitEmptyProperties() {
        OutboxPublisherProperties.DlqProperties dlq = new OutboxPublisherProperties.DlqProperties();
        dlq.setEnabled(null);

        dlq.applyDefaults();

        assertFalse(dlq.isEnabled());
        assertNotNull(dlq.getTransferTo());
        assertNotNull(dlq.getTransferFrom());
        assertNotNull(dlq.getCleanUp());
        assertFalse(dlq.getCleanUp().isEnabled());
    }

    @Test
    @DisplayName("UT DlqProperties.applyDefaults() when enabled is false should disable and init empty properties")
    public void applyDefaults_dlqWhenEnabledFalse_shouldDisableAndInitEmptyProperties() {
        OutboxPublisherProperties.DlqProperties dlq = new OutboxPublisherProperties.DlqProperties();
        dlq.setEnabled(false);

        dlq.applyDefaults();

        assertFalse(dlq.isEnabled());
        assertNotNull(dlq.getTransferTo());
        assertNotNull(dlq.getTransferFrom());
        assertNotNull(dlq.getCleanUp());
        assertFalse(dlq.getCleanUp().isEnabled());
    }

    @Test
    @DisplayName("UT DlqProperties.applyDefaults() when enabled is true and fields null should apply defaults")
    public void applyDefaults_dlqWhenEnabledTrueAndFieldsNull_shouldApplyDefaults() {
        OutboxPublisherProperties.DlqProperties dlq = new OutboxPublisherProperties.DlqProperties();
        dlq.setEnabled(true);

        dlq.applyDefaults();

        assertTrue(dlq.isEnabled());
        assertEquals(500, dlq.getBatchSize());
        assertNotNull(dlq.getPolling());
        assertEquals(PollingType.ADAPTIVE, dlq.getPolling().getType());

        assertNotNull(dlq.getTransferTo());
        assertEquals(500, dlq.getTransferTo().getBatchSize());

        assertNotNull(dlq.getTransferFrom());
        assertEquals(500, dlq.getTransferFrom().getBatchSize());

        assertNotNull(dlq.getCleanUp());
        assertTrue(dlq.getCleanUp().isEnabled());
    }

    @Test
    @DisplayName("UT DlqProperties.applyDefaults() when batch size is zero or negative should use default")
    public void applyDefaults_dlqWhenBatchSizeInvalid_shouldUseDefault() {
        OutboxPublisherProperties.DlqProperties dlq1 = new OutboxPublisherProperties.DlqProperties();
        dlq1.setEnabled(true);
        dlq1.setBatchSize(0);
        dlq1.applyDefaults();
        assertEquals(500, dlq1.getBatchSize());

        OutboxPublisherProperties.DlqProperties dlq2 = new OutboxPublisherProperties.DlqProperties();
        dlq2.setEnabled(true);
        dlq2.setBatchSize(-10);
        dlq2.applyDefaults();
        assertEquals(500, dlq2.getBatchSize());
    }

    @Test
    @DisplayName("UT DlqProperties.applyDefaults() when custom values provided should retain them")
    public void applyDefaults_dlqWhenCustomValuesProvided_shouldRetainThem() {
        OutboxPublisherProperties.DlqProperties dlq = new OutboxPublisherProperties.DlqProperties();
        dlq.setEnabled(true);
        dlq.setBatchSize(100);

        OutboxProperties.CleanUpProperties cleanUp = new OutboxProperties.CleanUpProperties();
        cleanUp.setEnabled(false);
        dlq.setCleanUp(cleanUp);

        OutboxPublisherProperties.DlqProperties.TransferProperties transferTo = new OutboxPublisherProperties.DlqProperties.TransferProperties();
        transferTo.setBatchSize(50);
        dlq.setTransferTo(transferTo);

        dlq.applyDefaults();

        assertTrue(dlq.isEnabled());
        assertEquals(100, dlq.getBatchSize());
        assertNotNull(dlq.getCleanUp());
        assertFalse(dlq.getCleanUp().isEnabled());
        assertEquals(50, dlq.getTransferTo().getBatchSize());
        assertEquals(100, dlq.getTransferFrom().getBatchSize());
    }

    @Test
    @DisplayName("UT TransferProperties getters should delegate to polling properties")
    public void transferProperties_shouldDelegateToPolling() {
        OutboxPublisherProperties.DlqProperties.TransferProperties transfer = new OutboxPublisherProperties.DlqProperties.TransferProperties();
        OutboxProperties.PollingProperties polling = new OutboxProperties.PollingProperties();
        polling.setMinFixedDelay(Duration.ofSeconds(1));
        polling.setMaxFixedDelay(Duration.ofSeconds(10));
        polling.setMultiplier(2.0);
        polling.setInitialDelay(Duration.ofSeconds(5));
        polling.setFixedDelay(Duration.ofSeconds(2));
        transfer.setPolling(polling);

        assertEquals(Duration.ofSeconds(1), transfer.getMinFixedDelay());
        assertEquals(Duration.ofSeconds(10), transfer.getMaxFixedDelay());
        assertEquals(2.0, transfer.getMultiplier());
        assertEquals(Duration.ofSeconds(5), transfer.getInitialDelay());
        assertEquals(Duration.ofSeconds(2), transfer.getFixedDelay());
    }

    @Test
    @DisplayName("UT toString methods should return formatted strings")
    public void dlqAndTransferProperties_toString_shouldNotBeNull() {
        OutboxPublisherProperties.DlqProperties dlq = new OutboxPublisherProperties.DlqProperties();
        OutboxPublisherProperties.DlqProperties.TransferProperties transfer = new OutboxPublisherProperties.DlqProperties.TransferProperties();

        assertNotNull(dlq.toString());
        assertNotNull(transfer.toString());
    }

    private OutboxPublisherProperties propertiesWithTimeouts(Duration emergencyTimeout, Duration maxBatchProcessingTime) {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        sender.setBeanName("bean");
        sender.setEmergencyTimeout(emergencyTimeout);
        properties.setSender(sender);
        properties.setEvents(new HashMap<>());
        OutboxPublisherProperties.StuckRecoveryProperties stuckRecovery = new OutboxPublisherProperties.StuckRecoveryProperties();
        stuckRecovery.setMaxBatchProcessingTime(maxBatchProcessingTime);
        properties.setStuckRecovery(stuckRecovery);
        return properties;
    }
}
