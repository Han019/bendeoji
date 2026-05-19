FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

RUN chmod +x ./gradlew
RUN ./gradlew --no-daemon dependencies

COPY src ./src

RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

ENV PORT=8080
ENV APP_DATABASE_PATH=/app/data/bendeoji.sqlite

RUN mkdir -p /app/data

COPY --from=build /workspace/build/libs/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
