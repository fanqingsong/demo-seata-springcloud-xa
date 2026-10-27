FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/maven:3.6.3-jdk-8 AS build
WORKDIR /src
COPY docker/maven/settings.xml /root/.m2/settings.xml
COPY . .
RUN mvn -f seata-springcloud/pom.xml -DskipTests package \
    && mkdir -p /out \
    && cp seata-springcloud-eureka/target/seata-springcloud-eureka-0.0.1-SNAPSHOT.jar /out/eureka.jar \
    && cp seata-springcloud-account/target/seata-springcloud-account-0.0.1-SNAPSHOT.jar /out/account.jar \
    && cp seata-springcloud-order/target/seata-springcloud-order-0.0.1-SNAPSHOT.jar /out/order.jar \
    && cp seata-springcloud-storage/target/seata-springcloud-storage-0.0.1-SNAPSHOT.jar /out/storage.jar \
    && cp seata-springcloud-business/target/seata-springcloud-business-0.0.1-SNAPSHOT.jar /out/business.jar \
    && cp seata-springcloud-web/target/seata-springcloud-web-0.0.1-SNAPSHOT.jar /out/web.jar

FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/eclipse-temurin:8-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /out/ /app/
ENV APP_JAR=web.jar
ENV JAVA_OPTS="-Xms128m -Xmx384m"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/$APP_JAR"]
