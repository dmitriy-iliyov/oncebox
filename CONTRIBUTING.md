# Contributing to oncebox

Thanks for taking the time to contribute. Bug reports, documentation fixes and code changes are all welcome.

## Before you start

- **Bugs** - open an issue using the *Bug report* template. A failing test or a minimal reproduction makes a fix much faster.
- **Features and larger changes** - open an issue first and describe the use case. Agreeing on the approach before writing the code saves both sides a rewrite.
- **Security issues** - do not open a public issue, follow [SECURITY.md](SECURITY.md) instead.
- **Questions** - use [GitHub Discussions](https://github.com/dmitriy-iliyov/oncebox/discussions) rather than issues.

## Prerequisites

- **JDK 21** - the build uses `<release>21</release>` and fails on older JDKs.
- **Docker** - integration and end-to-end tests start PostgreSQL, MySQL, Oracle, Kafka, RabbitMQ and Redis with Testcontainers.
- Maven is not required, use the bundled wrapper `./mvnw`.

## Building and testing

Build all library modules and run their unit and integration tests:

```shell
./mvnw clean install -Dgpg.skip=true
```

The modules under `oncebox-tests/` that are not part of the reactor are run separately, after the
reactor has been installed into the local Maven repository.

Classpath isolation tests (PostgreSQL only on the classpath):

```shell
cd oncebox-tests/oncebox-classpath-isolation-tests
../../mvnw verify -Dgpg.skip=true
```

Infrastructure integration tests (`db` is one of `postgres`, `mysql`, `oracle`):

```shell
cd oncebox-tests/oncebox-integration-tests
../../mvnw verify -Dgpg.skip=true -Dgroups=outbox       -Dspring.profiles.active=outbox       -Ddb=postgres
../../mvnw verify -Dgpg.skip=true -Dgroups=kafka-inbox  -Dspring.profiles.active=inbox,kafka  -Ddb=postgres
../../mvnw verify -Dgpg.skip=true -Dgroups=rabbit-inbox -Dspring.profiles.active=inbox,rabbit -Ddb=postgres
```

End-to-end tests (`broker` is `kafka` or `rabbit`):

```shell
cd oncebox-tests/oncebox-e2e-tests
../../mvnw verify -Ddb=postgres -Dbroker=kafka
```

CI runs all of the above for every pull request, the full database and broker matrix included.
Running the combination your change touches locally is usually enough.

## Code guidelines

- Keep a change focused on one thing. Unrelated refactoring belongs in a separate pull request.
- Follow the style of the surrounding code. Code comments and javadoc are written in English.
- Database-specific code lives in the dialect modules (`oncebox-postgresql`, `oncebox-mysql`, `oncebox-oracle`).
  A change to the SQL of one dialect usually needs the same change in the other two.
- A schema change is a new migration, `V<n+1>__*.sql` in `oncebox-starter/src/main/resources/db/oncebox/<vendor>/<feature>/`,
  for all three dialects; a released migration is never edited. One script runs under Flyway, Liquibase and the `DEV`
  mode at once, so follow the rules in
  [Writing a migration](https://dmitriy-iliyov.github.io/oncebox-docs/docs/reference/schema/#writing-a-migration),
  and sync the copy in `oncebox-tests/oncebox-test-fixtures`.
- Configuration properties live under the `oncebox.*` prefix. A new property needs a default and a line in the README.

## Tests

- Every bug fix comes with a test that fails without the fix.
- Every new behaviour comes with tests for it.
- Test classes are named after their level: `*UnitTests`, `*IntegrationTests`, `*ComponentTests`, `*EndToEndTests`.
- Repository tests run against all three databases. Shared verification logic lives in `oncebox-tests/oncebox-testkit`.

## Commit messages

Commits use a short `type(description)` header, for example:

```
fix(read and write the inbox cache under one key type)
add(qualifier annotations for the idempotent consumers)
docs(agent skill section in the README)
chore(align spring boot to 3.5.16 in modules outside the reactor)
```

Common types are `add`, `fix`, `docs`, `test` and `chore`.

## Pull requests

1. Fork the repository and create a branch from `main`.
2. Make your change, with tests.
3. Make sure `./mvnw clean install -Dgpg.skip=true` passes.
4. Update the README if the change is visible to users.
5. Open a pull request against `main` and fill in the template.

By contributing you agree that your contribution is licensed under the [Apache License 2.0](LICENSE)
and that you follow the [Code of Conduct](CODE_OF_CONDUCT.md).
