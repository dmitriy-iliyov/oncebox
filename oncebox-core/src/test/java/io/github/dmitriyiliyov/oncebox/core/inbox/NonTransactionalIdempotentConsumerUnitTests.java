package io.github.dmitriyiliyov.oncebox.core.inbox;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NonTransactionalIdempotentConsumerUnitTests {

    private static final UUID ID_1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ID_2 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final String MESSAGE_1 = "message-1";
    private static final String MESSAGE_2 = "message-2";
    private static final Map<String, UUID> MESSAGE_IDS = Map.of(MESSAGE_1, ID_1, MESSAGE_2, ID_2);
    private static final EventIdExtractor<String> ID_EXTRACTOR = MESSAGE_IDS::get;
    private static final String LOSS_MESSAGE = "they will not be processed again";

    @Mock
    private InboxManager inboxManager;

    private NonTransactionalIdempotentConsumer tested;
    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void setUp() {
        tested = new NonTransactionalIdempotentConsumer(inboxManager);
        logs = new ListAppender<>();
        logs.start();
        consumerLogger().addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        consumerLogger().detachAppender(logs);
    }

    @Test
    @DisplayName("UT constructor when inboxManager is null should throw NullPointerException")
    void constructor_whenInboxManagerIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new NonTransactionalIdempotentConsumer(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("inboxManager cannot be null");
    }

    @Test
    @DisplayName("UT consume(UUID, Runnable) when event is new should run operation")
    void consumeId_whenEventIsNew_shouldRunOperation() {
        // given
        Runnable operation = mock(Runnable.class);
        when(inboxManager.tryConsume(ID_1)).thenReturn(true);

        // when
        tested.consume(ID_1, operation);

        // then
        verify(operation).run();
        assertThat(lossReports()).isEmpty();
    }

    @Test
    @DisplayName("UT consume(UUID, Runnable) when event is already consumed should skip operation")
    void consumeId_whenEventAlreadyConsumed_shouldSkipOperation() {
        // given
        Runnable operation = mock(Runnable.class);
        when(inboxManager.tryConsume(ID_1)).thenReturn(false);

        // when
        tested.consume(ID_1, operation);

        // then
        verifyNoInteractions(operation);
    }

    @Test
    @DisplayName("UT consume(UUID, Runnable) when operation throws should rethrow, keep id recorded and report it lost")
    void consumeId_whenOperationThrows_shouldRethrowKeepIdRecordedAndReportItLost() {
        // given
        RuntimeException failure = new RuntimeException("smtp is down");
        when(inboxManager.tryConsume(ID_1)).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> tested.consume(ID_1, () -> { throw failure; }))
                .isSameAs(failure);
        verify(inboxManager).tryConsume(ID_1);
        verifyNoMoreInteractions(inboxManager);
        assertThat(lossReports()).singleElement().asString().contains(ID_1.toString());
    }

    @Test
    @DisplayName("UT consume(UUID, Runnable) when operation throws an Error should rethrow and report id lost")
    void consumeId_whenOperationThrowsError_shouldRethrowAndReportIdLost() {
        // given
        Error failure = new Error("out of resources");
        when(inboxManager.tryConsume(ID_1)).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> tested.consume(ID_1, () -> { throw failure; }))
                .isSameAs(failure);
        assertThat(lossReports()).singleElement().asString().contains(ID_1.toString());
    }

    @Test
    @DisplayName("UT consume(UUID, Runnable) when recording fails should rethrow without running operation or reporting loss")
    void consumeId_whenRecordingFails_shouldRethrowWithoutRunningOperationOrReportingLoss() {
        // given
        RuntimeException failure = new RuntimeException("db is down");
        Runnable operation = mock(Runnable.class);
        when(inboxManager.tryConsume(ID_1)).thenThrow(failure);

        // when / then
        assertThatThrownBy(() -> tested.consume(ID_1, operation))
                .isSameAs(failure);
        verifyNoInteractions(operation);
        assertThat(lossReports()).isEmpty();
    }

    @Test
    @DisplayName("UT consume(T, EventIdExtractor, Consumer) when event is new should pass message to operation")
    void consumeMessage_whenEventIsNew_shouldPassMessageToOperation() {
        // given
        Consumer<String> operation = mock(Consumer.class);
        when(inboxManager.tryConsume(ID_1)).thenReturn(true);

        // when
        tested.consume(MESSAGE_1, ID_EXTRACTOR, operation);

        // then
        verify(operation).accept(MESSAGE_1);
    }

    @Test
    @DisplayName("UT consume(T, EventIdExtractor, Consumer) when event is already consumed should skip operation")
    void consumeMessage_whenEventAlreadyConsumed_shouldSkipOperation() {
        // given
        Consumer<String> operation = mock(Consumer.class);
        when(inboxManager.tryConsume(ID_1)).thenReturn(false);

        // when
        tested.consume(MESSAGE_1, ID_EXTRACTOR, operation);

        // then
        verifyNoInteractions(operation);
    }

    @Test
    @DisplayName("UT consume(T, EventIdExtractor, Consumer) when operation throws should rethrow and report id lost")
    void consumeMessage_whenOperationThrows_shouldRethrowAndReportIdLost() {
        // given
        RuntimeException failure = new RuntimeException("smtp is down");
        when(inboxManager.tryConsume(ID_1)).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> tested.consume(MESSAGE_1, ID_EXTRACTOR, m -> { throw failure; }))
                .isSameAs(failure);
        verify(inboxManager).tryConsume(ID_1);
        verifyNoMoreInteractions(inboxManager);
        assertThat(lossReports()).singleElement().asString().contains(ID_1.toString());
    }

    @Test
    @DisplayName("UT consume(Set, Consumer) when some ids are already consumed should run operation with new ids only")
    void consumeIds_whenSomeAlreadyConsumed_shouldRunOperationWithNewIdsOnly() {
        // given
        Consumer<Set<UUID>> operation = mock(Consumer.class);
        when(inboxManager.tryConsumeAndGetDuplicates(Set.of(ID_1, ID_2))).thenReturn(Set.of(ID_1));

        // when
        tested.consume(Set.of(ID_1, ID_2), operation);

        // then
        ArgumentCaptor<Set<UUID>> passed = ArgumentCaptor.forClass(Set.class);
        verify(operation).accept(passed.capture());
        assertThat(passed.getValue()).containsExactly(ID_2);
    }

    @Test
    @DisplayName("UT consume(Set, Consumer) when all ids are already consumed should skip operation")
    void consumeIds_whenAllAlreadyConsumed_shouldSkipOperation() {
        // given
        Consumer<Set<UUID>> operation = mock(Consumer.class);
        when(inboxManager.tryConsumeAndGetDuplicates(Set.of(ID_1))).thenReturn(Set.of(ID_1));

        // when
        tested.consume(Set.of(ID_1), operation);

        // then
        verifyNoInteractions(operation);
    }

    @Test
    @DisplayName("UT consume(Set, Consumer) when operation throws should report only the ids recorded by this call")
    void consumeIds_whenOperationThrows_shouldReportOnlyIdsRecordedByThisCall() {
        // given
        RuntimeException failure = new RuntimeException("smtp is down");
        when(inboxManager.tryConsumeAndGetDuplicates(Set.of(ID_1, ID_2))).thenReturn(Set.of(ID_1));

        // when / then
        assertThatThrownBy(() -> tested.consume(Set.of(ID_1, ID_2), ids -> { throw failure; }))
                .isSameAs(failure);
        assertThat(lossReports()).singleElement().asString()
                .contains(ID_2.toString())
                .doesNotContain(ID_1.toString());
    }

    @Test
    @DisplayName("UT consume(List, EventIdExtractor, Consumer) when some messages are already consumed should run operation with new messages only")
    void consumeMessages_whenSomeAlreadyConsumed_shouldRunOperationWithNewMessagesOnly() {
        // given
        Consumer<List<String>> operation = mock(Consumer.class);
        when(inboxManager.tryConsumeAndGetDuplicates(Set.of(ID_1, ID_2))).thenReturn(Set.of(ID_1));

        // when
        tested.consume(List.of(MESSAGE_1, MESSAGE_2), ID_EXTRACTOR, operation);

        // then
        verify(operation).accept(List.of(MESSAGE_2));
    }

    @Test
    @DisplayName("UT consume(List, EventIdExtractor, Consumer) when operation throws should report only the ids recorded by this call")
    void consumeMessages_whenOperationThrows_shouldReportOnlyIdsRecordedByThisCall() {
        // given
        RuntimeException failure = new RuntimeException("smtp is down");
        when(inboxManager.tryConsumeAndGetDuplicates(Set.of(ID_1, ID_2))).thenReturn(Set.of(ID_1));

        // when / then
        assertThatThrownBy(() -> tested.consume(List.of(MESSAGE_1, MESSAGE_2), ID_EXTRACTOR, m -> { throw failure; }))
                .isSameAs(failure);
        assertThat(lossReports()).singleElement().asString()
                .contains(ID_2.toString())
                .doesNotContain(ID_1.toString());
    }

    private static Logger consumerLogger() {
        return (Logger) LoggerFactory.getLogger(NonTransactionalIdempotentConsumer.class);
    }

    private List<String> lossReports() {
        return logs.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.contains(LOSS_MESSAGE))
                .toList();
    }
}
