# Risk Management Monitoring

The Docker deployment includes Prometheus and Grafana as monitoring plugins for the risk management system.

## Access

- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000
- Grafana default account: `admin / Admin@123456`

You can override the Grafana password with `.env`:

```text
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=change-me
```

## Metrics

Prometheus scrapes these Spring Boot actuator endpoints:

- `discovery-server:8761/actuator/prometheus`
- `api-gateway:8088/actuator/prometheus`
- `auth-service:9001/actuator/prometheus`
- `user-service:9002/actuator/prometheus`
- `system-service:9003/actuator/prometheus`

Each backend service adds an `application` metric tag, so Grafana panels can group data by service name.

## Dashboard

Grafana automatically loads:

- Data source: `Risk Prometheus`
- Dashboard: `Risk Management System Overview`

The dashboard shows service availability, CPU usage, JVM heap usage, HTTP request rate, and recent 5xx errors.

## Start

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\docker-up.ps1
```

Or start only the monitoring plugins:

```powershell
docker compose up -d prometheus grafana
```
