FROM local/rocketmq-dashboard-static:2.0.0
WORKDIR /app
COPY build/LogViewerServer.class /app/LogViewerServer.class
EXPOSE 8080
CMD ["java", "-cp", "/app", "LogViewerServer"]
