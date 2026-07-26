package io.github.dmitriyiliyov.oncebox.example.traficgenerator;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class OrderSimulation extends Simulation {

    private static final String PROTOCOL = prop("targetProtocol", "http");
    private static final String HOST = prop("targetHost", "oncebox-producer-app");
    private static final int PORT = intProp("targetPort", 8080);
    private static final String ROOT = prop("targetRoot", "/api/orders");

    private static final int USERS = intProp("users", 100);
    private static final int DURATION = intProp("durationSec", 60);

    private static final int MIN_WAIT_MS = intProp("minWaitMs", 1);
    private static final int MAX_WAIT_MS = intProp("maxWaitMs", 5);

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(PROTOCOL + "://" + HOST + ":" + PORT)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .shareConnections();

    private static String createSingleBody() {
        return """
                {
                  "userId": %d,
                  "itemIds": "%s"
                }
                """.formatted(
                randomUserId(),
                generateItemIds()
        );
    }

    private static String updateBody() {
        return """
                {
                  "itemIds": "%s"
                }
                """.formatted(generateItemIds());
    }

    private final ScenarioBuilder orderCrudLoop = scenario("Order CRUD Loop")
            .exec(http("POST single order")
                            .post(ROOT)
                            .body(StringBody(s -> createSingleBody())).asJson()
                            .check(status().is(201))
                            .check(jsonPath("$.id").ofLong().saveAs("singleId"))
            )
            .pause(MIN_WAIT_MS, MAX_WAIT_MS)
            .exec(http("PATCH single order")
                            .patch(ROOT + "/#{singleId}")
                            .body(StringBody(s -> updateBody())).asJson()
                            .check(status().is(200))
            )
            .pause(MIN_WAIT_MS, MAX_WAIT_MS)
            .exec(http("DELETE single order")
                            .delete(ROOT + "/#{singleId}")
                            .check(status().is(204))
            );
    {
        setUp(
                orderCrudLoop.injectOpen(
                        rampUsersPerSec(0).to(USERS).during(Duration.ofSeconds(15)),
                        constantUsersPerSec(USERS).during(Duration.ofSeconds(DURATION))
                )
        ).protocols(httpProtocol);
    }

    private static String generateItemIds() {
        int count = ThreadLocalRandom.current().nextInt(1, 5);

        return IntStream.range(0, count)
                .mapToObj(i -> String.valueOf(ThreadLocalRandom.current().nextInt(1, 100)))
                .collect(Collectors.joining(","));
    }

    private static long randomUserId() {
        return ThreadLocalRandom.current().nextLong(1, 1000);
    }

    private static String prop(String key, String def) {
        return System.getProperty(key, def);
    }

    private static int intProp(String key, int def) {
        String v = System.getProperty(key);
        return v != null ? Integer.parseInt(v) : def;
    }
}