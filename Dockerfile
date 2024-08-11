FROM openjdk:17-slim

WORKDIR /usr/app/

COPY build/libs/*.jar application.jar

ENTRYPOINT ["java", "-jar", "application.jar"]

# Google Chrome 설치를 위한 추가
RUN apt-get update && \
    apt-get install -y wget && \
    wget https://dl.google.com/linux/chrome/deb/pool/main/g/google-chrome-stable/google-chrome-stable_125.0.6422.76-1_amd64.deb && \
    dpkg -i google-chrome-stable_125.0.6422.76-1_amd64.deb || apt --fix-broken install -y && \
    apt-get clean && \
    rm google-chrome-stable_125.0.6422.76-1_amd64.deb
