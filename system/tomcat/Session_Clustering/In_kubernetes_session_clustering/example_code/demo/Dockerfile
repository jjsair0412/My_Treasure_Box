#FROM tomcat:8.5.69-jdk8-openjdk
#FROM tomcat:9.0
# version issue
FROM tomcat:10.0

#FROM openjdk:17-jdk-alpine


ARG WAR_FILE=./build/libs/*.war
ARG SERVER_XML=./tomcat/server.xml
ARG TIR_JAR_PATH=./tomcat/tomcat-tribes-11.0.0.jar

COPY ${WAR_FILE} /usr/local/tomcat/webapps/app.war
COPY ${SERVER_XML} /usr/local/tomcat/conf/server.xml
COPY ${TIR_JAR_PATH} /usr/local/tomcat/lib/tomcat-tribes-11.0.0.jar


EXPOSE 8080
ENTRYPOINT ["catalina.sh", "run"]