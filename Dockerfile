FROM openjdk:17-slim

WORKDIR /usr/app/

COPY build/libs/*.jar application.jar

ENTRYPOINT ["java", "-jar", "application.jar"]
