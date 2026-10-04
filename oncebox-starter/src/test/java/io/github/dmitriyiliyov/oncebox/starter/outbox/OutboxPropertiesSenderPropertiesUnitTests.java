package io.github.dmitriyiliyov.oncebox.starter.outbox;

import io.github.dmitriyiliyov.oncebox.starter.TransportType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OutboxPropertiesSenderPropertiesUnitTests {

    @Test
    @DisplayName("UT initialize() should throw when type is null")
    public void applyDefaults_typeNull_shouldThrow() {
        // given
        OutboxProperties.SenderProperties sender = new OutboxProperties.SenderProperties();
        sender.setType(null);
        sender.setBeanName("myBean");

        // when + then
        assertThrows(IllegalArgumentException.class, sender::applyDefaults);
    }

    @Test
    @DisplayName("UT initialize() with valid type and beanName should assign values")
    public void applyDefaults_withValidTypeAndBeanName_shouldAssignValues() {
        // given
        TransportType type = TransportType.KAFKA;
        String beanName = "myKafkaBean";

        // when
        OutboxProperties.SenderProperties sender = new OutboxProperties.SenderProperties();
        sender.setType(type);
        sender.setBeanName(beanName);
        sender.applyDefaults();

        // then
        assertEquals(type, sender.getType());
        assertEquals(beanName, sender.getBeanName());
        assertEquals(Duration.ofSeconds(120), sender.getEmergencyTimeout());
    }

    @Test
    @DisplayName("UT initialize() with valid type, beanName and emergencyTimeout should assign values")
    public void applyDefaults_validParameters_shouldAssignValues() {
        // given
        TransportType type = TransportType.KAFKA;
        String beanName = "myKafkaBean";
        Duration emergencyTimeout = Duration.ofSeconds(120);

        // when
        OutboxProperties.SenderProperties sender = new OutboxProperties.SenderProperties();
        sender.setType(type);
        sender.setBeanName(beanName);
        sender.setEmergencyTimeout(emergencyTimeout);
        sender.applyDefaults();

        // then
        assertEquals(type, sender.getType());
        assertEquals(beanName, sender.getBeanName());
        assertEquals(emergencyTimeout, sender.getEmergencyTimeout());
    }
}
