FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN ./gradlew --version

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN useradd --system --create-home --shell /usr/sbin/nologin dachjobs
COPY --from=build /app/build/libs/*.jar app.jar
RUN mkdir -p /export && chown dachjobs:dachjobs /export
USER dachjobs

ENV EXPORT_DIR=/export
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
