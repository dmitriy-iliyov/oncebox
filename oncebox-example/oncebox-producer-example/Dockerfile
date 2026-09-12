FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# build lib
COPY . .
RUN mvn clean install -DskipTests -Dgpg.skip=true

# build producer
RUN mvn -f oncebox-example/oncebox-shared-contract/pom.xml clean install -DskipTests
RUN mvn -B -e -f oncebox-example/oncebox-producer-example/pom.xml clean install -DskipTests=true

FROM eclipse-temurin:21-jre AS final
WORKDIR /app
COPY --from=builder /app/oncebox-example/oncebox-producer-example/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]