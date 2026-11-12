package io.github.dmitriyiliyov.oncebox.starter;

/**
 * Parts of oncebox that own tables. The value names the feature's directory of migrations and its schema
 * history table.
 */
public enum OnceboxFeature {
    JOBS("jobs"),
    OUTBOX("outbox"),
    DLQ("dlq"),
    INBOX("inbox");

    private final String value;

    OnceboxFeature(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
