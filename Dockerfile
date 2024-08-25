FROM maven:3.9.6-amazoncorretto-21
WORKDIR /boot/target
COPY /boot/target/document-service-0.0.1-SNAPSHOT.jar document-service-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java", "-jar", "document-service-0.0.1-SNAPSHOT.jar"]