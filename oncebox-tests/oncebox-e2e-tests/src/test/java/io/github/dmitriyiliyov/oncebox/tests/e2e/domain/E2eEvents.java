package io.github.dmitriyiliyov.oncebox.tests.e2e.domain;

/**
 * Event types and destination declared in application.yaml.
 * TOPIC is the Kafka topic and, for Rabbit, the exchange the events are routed through.
 */
public final class E2eEvents {

    public static final String TOPIC = "e2e.events";

    // Rabbit-only: the queue bound to the TOPIC exchange that the consumer listens on
    public static final String QUEUE = "e2e.events.queue";

    // Events of AT_MOST_ONCE_EVENT travel apart from the rest, to a listener on the non-transactional consumer
    public static final String AT_MOST_ONCE_TOPIC = "e2e.at-most-once.events";
    public static final String AT_MOST_ONCE_QUEUE = "e2e.at-most-once.events.queue";

    public static final String DEFAULT_EVENT = "e2e-event";
    public static final String AOP_EVENT = "e2e-aop-event";
    public static final String RETRY_EVENT = "e2e-retry-event";
    public static final String DLQ_EVENT = "e2e-dlq-event";
    public static final String AT_MOST_ONCE_EVENT = "e2e-at-most-once-event";

    private E2eEvents() {}
}
