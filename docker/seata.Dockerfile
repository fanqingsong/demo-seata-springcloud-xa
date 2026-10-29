FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/maven:3.6.3-jdk-8 AS build
WORKDIR /build
COPY docker/maven/settings.xml /root/.m2/settings.xml
COPY docker/seata/pom.xml /build/pom.xml
RUN mvn -DskipTests package

FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/eclipse-temurin:8-jre
WORKDIR /seata
COPY --from=build /build/target/lib /seata/lib
COPY docker/seata/conf /seata/conf
RUN mkdir -p /seata/logs /seata/sessionStore
EXPOSE 8091
ENTRYPOINT ["sh", "-c", "exec java -Xms256m -Xmx768m -XX:MaxDirectMemorySize=1024m -cp '/seata/conf:/seata/lib/*' io.seata.server.Server 8091 db"]
