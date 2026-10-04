package io.github.dmitriyiliyov.oncebox.starter.outbox;

import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxSender;
import io.github.dmitriyiliyov.oncebox.rabbit.RabbitOutboxSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationContext;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OutboxRabbitAutoConfigurationUnitTests {

    private OutboxRabbitAutoConfiguration config;

    @Mock
    private OutboxProperties outboxProperties;

    @Mock
    private OutboxProperties.SenderProperties senderProperties;

    @Mock
    private ApplicationContext context;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        config = new OutboxRabbitAutoConfiguration(outboxProperties);
        lenient().when(outboxProperties.getSender()).thenReturn(senderProperties);
        lenient().when(senderProperties.getEmergencyTimeout()).thenReturn(Duration.ofSeconds(10));
    }

    @Test
    @DisplayName("UT rabbitOutboxSender creates sender with explicit bean name and mandatory flag true")
    void rabbitOutboxSender_withExplicitBeanName_createsSuccessfully() {
        when(senderProperties.getBeanName()).thenReturn("explicitRabbitBean");
        when(context.containsBean("explicitRabbitBean")).thenReturn(true);
        when(context.getBean("explicitRabbitBean", RabbitTemplate.class)).thenReturn(rabbitTemplate);
        when(rabbitTemplate.isMandatoryFor(any())).thenReturn(true);

        OutboxSender result = config.rabbitOutboxSender(context);

        assertThat(result).isInstanceOf(RabbitOutboxSender.class);
        verify(senderProperties).setBeanName("explicitRabbitBean");
    }

    @Test
    @DisplayName("UT rabbitOutboxSender creates sender with implicit type resolution and mandatory flag false")
    void rabbitOutboxSender_withImplicitTypeResolution_createsSuccessfullyWithWarning() {
        when(senderProperties.getBeanName()).thenReturn("");
        when(context.getBeanNamesForType(RabbitTemplate.class)).thenReturn(new String[]{"resolvedRabbitBean"});
        when(context.containsBean("resolvedRabbitBean")).thenReturn(true);
        when(context.getBean("resolvedRabbitBean", RabbitTemplate.class)).thenReturn(rabbitTemplate);
        when(rabbitTemplate.isMandatoryFor(any())).thenReturn(false);

        OutboxSender result = config.rabbitOutboxSender(context);

        assertThat(result).isInstanceOf(RabbitOutboxSender.class);
        verify(senderProperties).setBeanName("resolvedRabbitBean");
    }

    @Test
    @DisplayName("UT rabbitOutboxSender throws IllegalStateException when implicit resolution finds zero beans")
    void rabbitOutboxSender_implicitResolutionZeroBeans_throwsException() {
        when(senderProperties.getBeanName()).thenReturn(null);
        when(context.getBeanNamesForType(RabbitTemplate.class)).thenReturn(new String[0]);

        assertThatThrownBy(() -> config.rabbitOutboxSender(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot create OutboxSender: no RabbitTemplate bean found");
    }

    @Test
    @DisplayName("UT rabbitOutboxSender throws IllegalStateException when implicit resolution finds multiple beans")
    void rabbitOutboxSender_implicitResolutionMultipleBeans_throwsException() {
        when(senderProperties.getBeanName()).thenReturn("");
        when(context.getBeanNamesForType(RabbitTemplate.class)).thenReturn(new String[]{"rabbit1", "rabbit2"});

        assertThatThrownBy(() -> config.rabbitOutboxSender(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot create OutboxSender: found more then one RabbitTemplate bean");
    }

    @Test
    @DisplayName("UT rabbitOutboxSender throws IllegalArgumentException when context does not contain resolved bean")
    void rabbitOutboxSender_beanNotFoundInContext_throwsException() {
        when(senderProperties.getBeanName()).thenReturn("missingRabbitBean");
        when(context.containsBean("missingRabbitBean")).thenReturn(false);

        assertThatThrownBy(() -> config.rabbitOutboxSender(context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot create OutboxSender: RabbitTemplate bean 'missingRabbitBean' not found");
    }
}
