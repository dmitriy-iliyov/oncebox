package io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared;

/**
 * Which repository a consumer bean writes through. Each variant listens on queues and topics of its own: two beans
 * on one queue compete for its messages, and a JDBC test would then partly run the JPA path and the other way round.
 */
public enum ConsumerVariant {

    JDBC("jdbc"),
    JPA("jpa");

    private final String suffix;

    ConsumerVariant(String suffix) {
        this.suffix = suffix;
    }

    /**
     * The queue or topic name this variant uses for the given base name.
     */
    public String of(String baseName) {
        return baseName + "." + suffix;
    }
}
