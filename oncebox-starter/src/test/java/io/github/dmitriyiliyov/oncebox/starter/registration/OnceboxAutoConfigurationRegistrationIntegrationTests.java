package io.github.dmitriyiliyov.oncebox.starter.registration;

import io.github.dmitriyiliyov.oncebox.core.inbox.InboxManager;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxPublisher;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxSender;
import io.github.dmitriyiliyov.oncebox.starter.registration.componentsender.ComponentSenderApplication;
import io.github.dmitriyiliyov.oncebox.starter.registration.importedsender.ImportedSenderApplication;
import io.github.dmitriyiliyov.oncebox.starter.registration.mainjdbctemplate.MainClassJdbcTemplateApplication;
import io.github.dmitriyiliyov.oncebox.starter.registration.mainsender.MainClassSenderApplication;
import io.github.dmitriyiliyov.oncebox.starter.registration.plain.PlainApplication;
import io.github.dmitriyiliyov.oncebox.starter.registration.scannedsender.ScannedSenderApplication;
import io.github.dmitriyiliyov.oncebox.starter.registration.scheduling.SchedulingApplication;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// oncebox in a real Spring Boot application, registered through AutoConfiguration.imports; one application per package
class OnceboxAutoConfigurationRegistrationIntegrationTests {

    @Test
    @DisplayName("IT context() when starter is on the classpath should register the outbox")
    void context_whenStarterIsOnClasspath_shouldRegisterOutbox() {
        // when
        RegisteredBeans tested = RegisteredBeans.of(PlainApplication.class);

        // then
        assertThat(tested.namesOf(OutboxPublisher.class)).containsExactly("outboxPublisher");
        assertThat(tested.namesOf(OutboxSender.class)).containsExactly("kafkaOutboxSender");
        assertThat(tested.contains("outboxPublishAspect")).isTrue();
    }

    @Test
    @DisplayName("IT context() when AspectJ is not on the classpath should register the outbox without the publish aspect")
    void context_whenAspectJIsNotOnClasspath_shouldRegisterOutboxWithoutPublishAspect() {
        // when
        RegisteredBeans tested = RegisteredBeans.withoutClasses(PlainApplication.class, Aspect.class);

        // then
        assertThat(tested.startupFailure()).isNull();
        assertThat(tested.namesOf(OutboxPublisher.class)).containsExactly("outboxPublisher");
        assertThat(tested.contains("outboxPublishAspect")).isFalse();
    }

    @Test
    @DisplayName("IT context() when inbox is enabled should register the inbox next to the outbox")
    void context_whenInboxIsEnabled_shouldRegisterInboxNextToOutbox() {
        // when
        RegisteredBeans tested = RegisteredBeans.of(
                PlainApplication.class,
                "oncebox.inbox.enabled=true",
                "oncebox.inbox.source.type=kafka"
        );

        // then
        assertThat(tested.namesOf(InboxManager.class)).containsExactly("inboxManager");
        assertThat(tested.namesOf(OutboxPublisher.class)).containsExactly("outboxPublisher");
    }

    @Test
    @DisplayName("IT context() when outbox is disabled should not register the outbox")
    void context_whenOutboxIsDisabled_shouldNotRegisterOutbox() {
        // when
        RegisteredBeans tested = RegisteredBeans.of(PlainApplication.class, "oncebox.outbox.enabled=false");

        // then
        assertThat(tested.namesOf(OutboxPublisher.class)).isEmpty();
        assertThat(tested.namesOf(OutboxSender.class)).isEmpty();
    }

    @Test
    @DisplayName("IT context() when sender is a @Bean of the main class should replace the built-in sender")
    void context_whenSenderIsBeanOfMainClass_shouldReplaceBuiltInSender() {
        // when
        RegisteredBeans tested = RegisteredBeans.of(MainClassSenderApplication.class);

        // then
        assertThat(tested.namesOf(OutboxSender.class)).containsExactly("userOutboxSender");
    }

    @Test
    @DisplayName("IT context() when sender comes from a configuration imported by the main class should replace the built-in sender")
    void context_whenSenderIsImportedByMainClass_shouldReplaceBuiltInSender() {
        // when
        RegisteredBeans tested = RegisteredBeans.of(ImportedSenderApplication.class);

        // then
        assertThat(tested.namesOf(OutboxSender.class)).containsExactly("userOutboxSender");
    }

    @Test
    @DisplayName("IT context() when sender is a @Bean of a scanned configuration should replace the built-in sender")
    void context_whenSenderIsBeanOfScannedConfiguration_shouldReplaceBuiltInSender() {
        // when
        RegisteredBeans tested = RegisteredBeans.of(ScannedSenderApplication.class);

        // then
        assertThat(tested.namesOf(OutboxSender.class)).containsExactly("userOutboxSender");
    }

    @Test
    @DisplayName("IT context() when sender is a scanned @Component should replace the built-in sender")
    void context_whenSenderIsScannedComponent_shouldReplaceBuiltInSender() {
        // when
        RegisteredBeans tested = RegisteredBeans.of(ComponentSenderApplication.class);

        // then
        assertThat(tested.namesOf(OutboxSender.class)).containsExactly("userOutboxSender");
    }

    @Test
    @DisplayName("IT context() when onceboxJdbcTemplate is a @Bean of the main class should start with it instead of the library one")
    void context_whenOnceboxJdbcTemplateIsBeanOfMainClass_shouldStartWithItInsteadOfLibraryOne() {
        // when
        RegisteredBeans tested = RegisteredBeans.of(MainClassJdbcTemplateApplication.class);

        // then
        assertThat(tested.startupFailure()).isNull();
        assertThat(tested.definition("onceboxJdbcTemplate").getFactoryBeanName())
                .isEqualTo("mainClassJdbcTemplateApplication");
        assertThat(tested.contains("jdbcTemplate")).as("Spring Boot jdbcTemplate").isTrue();
    }

    @Test
    @DisplayName("IT context() when application enables scheduling should keep the Spring Boot task scheduler")
    void context_whenApplicationEnablesScheduling_shouldKeepSpringBootTaskScheduler() {
        // when
        RegisteredBeans tested = RegisteredBeans.of(SchedulingApplication.class);

        // then
        assertThat(tested.contains("taskScheduler")).isTrue();
        assertThat(tested.contains("onceboxScheduledExecutorService")).isTrue();
    }
}
