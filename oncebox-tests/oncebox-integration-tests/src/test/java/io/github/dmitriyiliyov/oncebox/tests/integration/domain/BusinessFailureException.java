package io.github.dmitriyiliyov.oncebox.tests.integration.domain;

/**
 * Thrown by a business method after it has written its own row, so that a test can tell a rolled-back business
 * transaction from any other failure on the way.
 */
public class BusinessFailureException extends RuntimeException {

    public BusinessFailureException() {
        super("Business transaction exception");
    }
}
