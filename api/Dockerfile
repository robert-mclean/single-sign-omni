FROM amazoncorretto:21-alpine3.21-jdk
EXPOSE 8080

VOLUME /tmp
COPY target/api-0.0.1-SNAPSHOT.jar app.jar
COPY src/main/resources/saml-idp-keystore.jks src/main/resources/saml-idp-keystore.jks
ENTRYPOINT ["java","-jar","/app.jar"]