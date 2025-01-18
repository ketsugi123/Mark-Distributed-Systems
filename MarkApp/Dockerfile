FROM openjdk:11
RUN mkdir /usr/executables
COPY target/MarkApp-1.0.jar /usr/executables
RUN mkdir /usr/datafiles
WORKDIR /usr/datafiles
ENTRYPOINT ["java", "-jar", "/usr/executables/MarkApp-1.0.jar"]
