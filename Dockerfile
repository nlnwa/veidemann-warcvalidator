FROM openjdk:8

LABEL maintainer="netarkivet@nb.no"

COPY target/warc-validator-0.0.1-SNAPSHOT.jar /application.jar

ENTRYPOINT ["java", "-jar", "/application.jar"]

CMD ["5"]

VOLUME ["/warcs", "/validwarcs"]
