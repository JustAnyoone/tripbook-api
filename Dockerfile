# Stage 1: build the jar with the Maven wrapper (tests run in CI, so they are skipped here)
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

# Stage 2: a small runtime image with only the JRE and the jar, running as a non-root user
FROM eclipse-temurin:17-jre
RUN useradd --system --uid 10001 tripbook
USER tripbook
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
