# build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 taskmanager
COPY --from=build /build/target/*.jar app.jar
USER taskmanager
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
