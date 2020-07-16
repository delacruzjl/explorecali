FROM alpine:3.11
LABEL author="Jose"
LABEL maintainer="jose.delacruz@microsoft.com"
RUN apk add openjdk11
ARG JAR_FILE="target/*.jar"
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-Dserver.port=80", "-jar", "/app.jar"]
EXPOSE 80/tcp
