package io.github.dmitriyiliyov.oncebox.tests.load;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;
import java.util.UUID;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class OutboxLoadSimulation extends Simulation {

    private static final int USERS = 100;

    HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:8080")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    ScenarioBuilder createOrderScenario = scenario("Create Single Order")
            .exec(http("create_order")
                    .post("/api/orders")
                    .body(StringBody(session -> {
                        long userId = (long) (Math.random() * 1000);
                        String itemIds = UUID.randomUUID().toString();
                        return "{\"userId\":" + userId + ",\"itemIds\":\"" + itemIds + "\"}";
                    }))
                    .check(status().is(201))
            );

    ScenarioBuilder createBatchOrderScenario = scenario("Create Batch Orders")
            .exec(http("create_batch_orders")
                    .post("/api/orders/batch")
                    .body(StringBody(session -> {
                        StringBuilder sb = new StringBuilder("[");
                        for (int i = 0; i < 10; i++) {
                            long userId = (long) (Math.random() * 1000);
                            String itemIds = UUID.randomUUID().toString();
                            sb.append("{\"userId\":").append(userId).append(",\"itemIds\":\"").append(itemIds).append("\"}");
                            if (i < 9) {
                                sb.append(",");
                            }
                        }
                        sb.append("]");
                        return sb.toString();
                    }))
                    .check(status().is(201))
            );

    {
        setUp(
                createOrderScenario.injectOpen(
                        rampUsersPerSec(1).to(USERS).during(Duration.ofSeconds(30)),
                        constantUsersPerSec(USERS).during(Duration.ofSeconds(60))
                ),
                createBatchOrderScenario.injectOpen(
                        rampUsersPerSec(1).to(USERS).during(Duration.ofSeconds(30)),
                        constantUsersPerSec(USERS).during(Duration.ofSeconds(60))
                )
        ).protocols(httpProtocol)
                // A KO request alone does not fail the Gatling run - only a global assertion does.
                .assertions(global().failedRequests().count().is(0L));
    }
}
