FROM gradle:7.6-jdk16 AS build
COPY --chown=gradle:gradle . /pidor
WORKDIR /pidor
RUN gradle shadowJar --no-daemon

FROM openjdk:16-alpine
RUN mkdir /config/
RUN touch /config/config.json
COPY --from=build /pidor/build/libs/*.jar /

ENTRYPOINT ["java", "-jar", "/Bot.jar"]