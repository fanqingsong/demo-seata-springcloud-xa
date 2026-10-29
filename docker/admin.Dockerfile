FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/maven:3.6.3-jdk-8 AS build
WORKDIR /src
COPY docker/maven/settings.xml /root/.m2/settings.xml
COPY seata-admin /src
RUN mvn -DskipTests package

FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/eclipse-temurin:8-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /src/target/seata-admin-0.0.1-SNAPSHOT.jar /app/seata-admin.jar
EXPOSE 7091
ENTRYPOINT ["java", "-Xms128m", "-Xmx256m", "-jar", "/app/seata-admin.jar"]
