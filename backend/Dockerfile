FROM local/rocketmq-dashboard-static:2.0.0

ARG SERVICE_NAME
ENV SERVICE_NAME=${SERVICE_NAME}
ENV JAVA_OPTS=""

WORKDIR /app
COPY ${SERVICE_NAME}/target/${SERVICE_NAME}-1.0.0.jar /app/app.jar

EXPOSE 8761 8088 9001 9002 9003

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
