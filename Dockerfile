#FROM alpine/git as clone
#WORKDIR /app
#RUN git clone https://github.com/nlnwa/veidemann-warc-validator.git
#
#FROM maven:3.5.2-jdk-8-alpine as build
#WORKDIR /app
#COPY --from=clone /app/warc-validator /app
#RUN mvn install
#
#FROM openjdk:8-jre-alpine
#WORKDIR /app
#COPY --from=build /app/target/warc-validator-0.0.1-SNAPSHOT.jar /app
#ADD ./src/main/resources/jhoveconfig/jhove.conf /jhove.conf
#ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "/warc-validator-0.0.1-SNAPSHOT.jar"]
#
#CMD ["5"]
#
#VOLUME ["/warcs", "/validwarcs"]
