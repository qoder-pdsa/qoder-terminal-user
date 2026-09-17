FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /src/target/qoder-terminal-user-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8084
# 迁移：docker run ... qoder-terminal-user --spring.profiles.active=migrate
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
