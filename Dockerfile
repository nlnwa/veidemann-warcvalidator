FROM openjdk:8-jdk-alpine

LABEL maintainer="nettarkivet@nb.no"

COPY target/warc-validator-0.0.1-SNAPSHOT.jar /application.jar

ADD ./src/main/resources/jhoveconfig/jhove.conf /jhove.conf

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "/application.jar"]

CMD ["5"]

VOLUME ["/warcs", "/validwarcs"]
