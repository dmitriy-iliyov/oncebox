package io.github.dmitriyiliyov.oncebox.dlq.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Checks the document springdoc generates from the annotations, since nothing else compiles or tests them.
 */
@SpringBootTest(classes = WebTestApplication.class)
@AutoConfigureMockMvc
class OutboxDlqOpenApiIntegrationTests {

    private static final String BASE = "/api/outbox-dlq/events";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OutboxDlqApiService service;

    @Test
    @DisplayName("IT api-docs every error response should carry no content instead of the success body")
    void apiDocs_everyErrorResponse_shouldHaveNoContent() throws Exception {
        // given
        JsonNode paths = apiDocs().path("paths");

        // when
        List<String> withContent = new ArrayList<>();
        for (Map.Entry<String, JsonNode> path : paths.properties()) {
            for (Map.Entry<String, JsonNode> operation : path.getValue().properties()) {
                for (Map.Entry<String, JsonNode> response : operation.getValue().path("responses").properties()) {
                    if (response.getKey().charAt(0) >= '4' && response.getValue().has("content")) {
                        withContent.add("%s %s %s".formatted(operation.getKey(), path.getKey(), response.getKey()));
                    }
                }
            }
        }

        // then
        assertThat(withContent).isEmpty();
    }

    @Test
    @DisplayName("IT api-docs GET /batch should expose each BatchRequest component as a query parameter")
    void apiDocs_getBatch_shouldExposeFlatQueryParameters() throws Exception {
        // when
        JsonNode parameters = apiDocs().path("paths").path(BASE + "/batch").path("get").path("parameters");

        // then
        assertThat(parameters.findValuesAsText("name"))
                .containsExactlyInAnyOrder("eventType", "status", "batchNumber", "batchSize");
    }

    @Test
    @DisplayName("IT api-docs GET /{id} should not document a 400 for a malformed UUID")
    void apiDocs_getById_shouldNotDocument400() throws Exception {
        // when
        JsonNode responses = apiDocs().path("paths").path(BASE + "/{id}").path("get").path("responses");

        // then
        assertThat(fieldNames(responses)).containsExactlyInAnyOrder("200", "404");
    }

    @Test
    @DisplayName("IT api-docs DELETE /{id} should document IN_PROCESS as 409 and nothing as 400")
    void apiDocs_deleteById_shouldDocumentInProcessAs409() throws Exception {
        // when
        JsonNode responses = apiDocs().path("paths").path(BASE + "/{id}").path("delete").path("responses");

        // then
        assertThat(fieldNames(responses)).containsExactlyInAnyOrder("204", "404", "409");
    }

    @Test
    @DisplayName("IT api-docs PATCH /{id} should document validation as 400 and IN_PROCESS as 409")
    void apiDocs_updateById_shouldDocumentInProcessAs409() throws Exception {
        // when
        JsonNode responses = apiDocs().path("paths").path(BASE + "/{id}").path("patch").path("responses");

        // then
        assertThat(fieldNames(responses)).containsExactlyInAnyOrder("204", "400", "404", "409");
    }

    @Test
    @DisplayName("IT api-docs OperationStatus should be a schema of its own with its constants described")
    void apiDocs_operationStatus_shouldBeReferencedSchema() throws Exception {
        // when
        JsonNode schema = apiDocs().path("components").path("schemas").path("OperationStatus");

        // then
        assertThat(schema.path("description").asText()).contains("SUCCESS", "PARTIAL_SUCCESS", "POSSIBLE_PARTIAL_SUCCESS");
    }

    @Test
    @DisplayName("IT api-docs batch requests should not expose helper methods as properties")
    void apiDocs_batchRequests_shouldHaveOnlyRecordComponents() throws Exception {
        // when
        JsonNode schemas = apiDocs().path("components").path("schemas");

        // then
        assertThat(fieldNames(schemas.path("BatchUpdateRequest").path("properties")))
                .containsExactlyInAnyOrder("ids", "eventType", "status");
        assertThat(fieldNames(schemas.path("BatchDeleteRequest").path("properties")))
                .containsExactlyInAnyOrder("ids", "eventType");
    }

    private JsonNode apiDocs() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }
}
