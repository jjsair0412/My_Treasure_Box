FROM openjdk:17-jre-slim
ARG JAR_FILE=build/libs/signup-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} /app.jar

EXPOSE 7000

ENTRYPOINT ["java","-jar","/app.jar"]