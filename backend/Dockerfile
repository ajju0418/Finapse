# ---- Build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Dependencies are cached separately so source edits don't refetch the world.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

# ---- Run ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Never run the API as root.
RUN addgroup -S finapse && adduser -S finapse -G finapse

COPY --from=build /build/target/*.jar app.jar
USER finapse

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
