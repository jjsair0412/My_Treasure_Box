FROM openjdk:17-jdk-slim AS build

WORKDIR /app

COPY ./gradlew /app/
COPY ./gradle /app/gradle

COPY . /app/

RUN ls -al

RUN chmod +x ./gradlew

RUN ./gradlew clean build

FROM openjdk:17-jdk-slim
WORKDIR /app

ARG ENVIRONMENT

COPY --from=build /app/build/libs/spring-logging-proj-0.0.1-SNAPSHOT-plain.jar /app/

ENV SPRING_PROFILES_ACTIVE=${ENVIRONMENT}

EXPOSE 8080
CMD ["java", "-jar", "spring-logging-proj-0.0.1-SNAPSHOT-plain.jar"]
