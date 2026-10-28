FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY lombok.config build.gradle.kts settings.gradle.kts gradle.properties ./
RUN ./gradlew --no-daemon resolveDependencies
COPY src src
RUN ./gradlew --no-daemon bootJar

FROM build AS extract
WORKDIR /app/build/libs
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination ./extracted

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

ARG UID=10001
RUN adduser \
    --disabled-password \
    --gecos "" \
    --home "/nonexistent" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid "${UID}" \
    appuser
USER appuser

COPY --from=extract /app/build/libs/extracted/dependencies/ ./
COPY --from=extract /app/build/libs/extracted/spring-boot-loader/ ./
COPY --from=extract /app/build/libs/extracted/snapshot-dependencies/ ./
COPY --from=extract /app/build/libs/extracted/application/ ./

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
