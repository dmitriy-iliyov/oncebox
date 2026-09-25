package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.github.dmitriyiliyov.oncebox.dlq.api.exception.OutboxDlqEventNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Clock;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * An application's own catch-all advice sits next to the library's one; the library's handlers must still win.
 * The registration order is the production one - the application's advice first, from its component scan, the
 * library's after it, from the auto-configuration - since two unordered advices are tried in that order.
 */
@WebMvcTest(
        controllers = OutboxDlqController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = OutboxDlqControllerAdvice.class)
)
@Import(OutboxDlqControllerAdviceOrderIntegrationTests.AdviceRegistrationOrder.class)
class OutboxDlqControllerAdviceOrderIntegrationTests {

    private static final UUID EVENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OutboxDlqApiService service;

    @Test
    @DisplayName("IT GET /{id} when application has its own catch-all advice should still return 404")
    void get_whenApplicationHasCatchAllAdvice_shouldStillReturn404() throws Exception {
        // given
        when(service.findById(EVENT_ID)).thenThrow(new OutboxDlqEventNotFoundException(EVENT_ID));

        // when / then
        mockMvc.perform(get("/api/outbox-dlq/events/{id}", EVENT_ID))
                .andExpect(status().isNotFound());
    }

    @TestConfiguration
    static class AdviceRegistrationOrder {

        @Bean
        ApplicationCatchAllAdvice applicationCatchAllAdvice() {
            return new ApplicationCatchAllAdvice();
        }

        @Bean
        OutboxDlqControllerAdvice outboxDlqControllerAdvice(Clock clock) {
            return new OutboxDlqControllerAdvice(clock);
        }
    }

    @RestControllerAdvice
    static class ApplicationCatchAllAdvice {

        @ExceptionHandler(Exception.class)
        ProblemDetail handleException(Exception exception) {
            return ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
