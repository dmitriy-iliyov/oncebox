package io.github.dmitriyiliyov.oncebox.dlq.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DlqProblemUnitTests {

    private static final Instant NOW = Instant.parse("2026-09-30T10:15:30Z");
    private static final String PATH = "/api/outbox-dlq/events/batch";

    private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();

    @Test
    @DisplayName("UT DlqProblem when body has errors should list every key the real body has")
    void dlqProblem_whenBodyHasErrors_shouldListEveryKeyOfRealBody() throws Exception {
        // given
        ProblemDetail validation = ProblemDetailFactory.validationFailed(
                List.of(Map.of("field", "batchSize", "message", "too small")), PATH, NOW);

        // when
        JsonNode body = serialize(validation);

        // then
        assertThat(keysOf(body)).containsExactlyInAnyOrderElementsOf(componentsOf(DlqProblem.class));
    }

    @Test
    @DisplayName("UT DlqProblem when body has no errors should list every key the real body has except errors")
    void dlqProblem_whenBodyHasNoErrors_shouldListEveryKeyOfRealBodyExceptErrors() throws Exception {
        // given
        ProblemDetail notFound = ProblemDetailFactory.dlqEventNotFound("not found", PATH, NOW);

        // when
        JsonNode body = serialize(notFound);

        // then
        List<String> expected = componentsOf(DlqProblem.class);
        expected.remove("errors");
        assertThat(keysOf(body)).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("UT DlqFieldError when error is serialized should list every key the real error has")
    void dlqFieldError_whenErrorIsSerialized_shouldListEveryKeyOfRealError() throws Exception {
        // given
        ProblemDetail validation = ProblemDetailFactory.validationFailed(
                List.of(Map.of("field", "batchSize", "message", "too small")), PATH, NOW);

        // when
        JsonNode error = serialize(validation).get("errors").get(0);

        // then
        assertThat(keysOf(error)).containsExactlyInAnyOrderElementsOf(componentsOf(DlqProblem.DlqFieldError.class));
    }

    private JsonNode serialize(ProblemDetail problem) throws Exception {
        return mapper.readTree(mapper.writeValueAsString(problem));
    }

    private static List<String> keysOf(JsonNode node) {
        List<String> keys = new ArrayList<>();
        node.fieldNames().forEachRemaining(keys::add);
        return keys;
    }

    private static List<String> componentsOf(Class<? extends Record> type) {
        return new ArrayList<>(Arrays.stream(type.getRecordComponents()).map(c -> c.getName()).toList());
    }
}
