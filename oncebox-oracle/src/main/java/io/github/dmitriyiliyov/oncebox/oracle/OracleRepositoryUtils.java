package io.github.dmitriyiliyov.oncebox.oracle;

public final class OracleRepositoryUtils {

    // Oracle rejects an IN list of more than 1000 expressions (ORA-01795)
    public static final int MAX_IN_LIST_SIZE = 1000;

    private OracleRepositoryUtils() {}
}
