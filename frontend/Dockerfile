FROM local/rocketmq-dashboard-static:2.0.0
WORKDIR /app
COPY dist /app
COPY DockerFrontendServer.class /app/DockerFrontendServer.class
EXPOSE 80
CMD ["java", "-cp", "/app", "DockerFrontendServer"]
