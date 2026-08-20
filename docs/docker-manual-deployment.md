# 风险管理系统 Docker 手工部署手册

本文档用于在 Windows + Docker Desktop 环境下，手工把风险管理系统部署到 Docker。

项目目录：

```text
D:\software\workspace\user-management-distributed
```

## 1. 部署内容

Docker 部署后包含以下容器：

| 容器名 | 说明 | 访问地址 |
| --- | --- | --- |
| `risk-mysql` | MySQL 数据库 | `localhost:3307` |
| `risk-redis` | Redis 会话缓存 | `localhost:6381` |
| `risk-discovery-server` | Eureka 注册中心 | `http://localhost:8761` |
| `risk-api-gateway` | 后端统一网关 | `http://localhost:8088` |
| `risk-auth-service` | 认证服务 | 仅 Docker 内网 `auth-service:9001` |
| `risk-user-service` | 用户与权限服务 | 仅 Docker 内网 `user-service:9002` |
| `risk-system-service` | 风险业务与系统服务 | 仅 Docker 内网 `system-service:9003` |
| `risk-frontend` | 前端页面 | `http://localhost:5173` |
| `risk-log-viewer` | Dozzle 日志查看 | `http://localhost:9999` |
| `risk-prometheus` | Prometheus 监控 | `http://localhost:9090` |
| `risk-grafana` | Grafana 看板 | `http://localhost:3000` |

系统和 Grafana 均不提供默认密码；凭据必须在部署前通过环境变量设置。

## 2. 前置条件

本机需要安装并启动：

1. Docker Desktop
2. JDK 17 或更高版本
3. Maven
4. Node.js 和 npm
5. PowerShell

确认 Docker 可用：

```powershell
docker version
docker compose version
```

确认 Maven 可用：

```powershell
mvn.cmd -version
```

确认 npm 可用：

```powershell
npm.cmd -version
```

如果 PowerShell 执行 `npm run` 报 `npm.ps1` 被禁止，请使用 `npm.cmd`。

## 3. 进入项目目录

```powershell
cd D:\software\workspace\user-management-distributed
```

后续命令都在该目录执行。

## 4. 准备环境变量

建议创建本地 `.env` 文件：

```powershell
Copy-Item .env.example .env
notepad .env
```

推荐至少确认以下配置：

```text
MYSQL_ROOT_PASSWORD=<强随机密码>
REDIS_PASSWORD=<强随机密码>
JWT_RSA_PRIVATE_KEY=<新生成的PKCS8私钥Base64>
JWT_RSA_PUBLIC_KEY=<对应的X509公钥Base64>
INTERNAL_SERVICE_KEY=<至少32个随机字节>
BOOTSTRAP_ADMIN_PASSWORD_HASH='<60位BCrypt哈希>'
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=<强随机密码>
```

说明：

- `MYSQL_ROOT_PASSWORD` 是 Docker 内 MySQL root 密码。
- 如果已经存在 `risk_mysql_data` 数据卷，修改 MySQL root 密码不会自动改旧数据卷里的密码。
- 私钥只注入认证服务；网关和业务服务只注入公钥。
- BCrypt 哈希包含 `$` 时，应在 `.env` 中使用单引号包裹。

## 5. 拉取基础镜像

先拉取外部镜像：

```powershell
docker pull mysql:9.4
docker pull redis:7.4-alpine
docker pull amir20/dozzle:latest
docker pull prom/prometheus:v2.53.4
docker pull grafana/grafana:10.4.3
```

如果 Prometheus 或 Grafana 拉取超时，可以稍后重试，不影响主业务系统启动。

项目当前后端和前端 Dockerfile 使用本地基础镜像：

```text
local/rocketmq-dashboard-static:2.0.0
```

检查本机是否已有该镜像：

```powershell
docker image ls local/rocketmq-dashboard-static
```

如果没有，需要先准备该基础镜像，否则后端和前端镜像构建会失败。当前开发机已经有该镜像。

## 6. 构建后端

```powershell
mvn.cmd -f backend\pom.xml -DskipTests clean package
```

构建成功后会生成：

```text
backend\discovery-server\target\discovery-server-1.0.0.jar
backend\api-gateway\target\api-gateway-1.0.0.jar
backend\auth-service\target\auth-service-1.0.0.jar
backend\user-service\target\user-service-1.0.0.jar
backend\system-service\target\system-service-1.0.0.jar
```

## 7. 构建前端

进入前端目录：

```powershell
cd D:\software\workspace\user-management-distributed\frontend
```

安装依赖：

```powershell
npm.cmd install
```

构建前端：

```powershell
npm.cmd run build
```

编译 Docker 前端服务类：

```powershell
javac.exe --release 17 DockerFrontendServer.java
```

如果系统找不到 `javac.exe`，使用本机 JDK 路径，例如：

```powershell
D:\software\maven\tools\jdk-21.0.11+10\bin\javac.exe --release 17 DockerFrontendServer.java
```

返回项目目录：

```powershell
cd D:\software\workspace\user-management-distributed
```

## 8. 构建 Docker 镜像

构建全部业务镜像：

```powershell
docker compose -f docker-compose.yml build discovery-server api-gateway auth-service user-service system-service frontend
```

查看镜像：

```powershell
docker image ls | Select-String -Pattern "risk/"
```

应看到类似：

```text
risk/discovery-server
risk/api-gateway
risk/auth-service
risk/user-service
risk/system-service
risk/frontend
```

## 9. 启动基础设施

先启动 MySQL、Redis、日志查看器：

```powershell
docker compose -f docker-compose.yml up -d mysql redis log-viewer
```

查看状态：

```powershell
docker compose -f docker-compose.yml ps mysql redis log-viewer
```

等待 `risk-mysql` 和 `risk-redis` 变为 `healthy`。

## 10. 启动后端服务

按顺序启动注册中心：

```powershell
docker compose -f docker-compose.yml up -d discovery-server
```

等待注册中心健康：

```powershell
docker compose -f docker-compose.yml ps discovery-server
```

再启动业务后端：

```powershell
docker compose -f docker-compose.yml up -d api-gateway auth-service user-service system-service
```

查看后端状态：

```powershell
docker compose -f docker-compose.yml ps discovery-server api-gateway auth-service user-service system-service
```

## 11. 启动前端

```powershell
docker compose -f docker-compose.yml up -d frontend
```

访问：

```text
http://localhost:5173
```

## 12. 启动监控插件

如果 Prometheus 和 Grafana 镜像已经拉取成功，执行：

```powershell
docker compose -f docker-compose.yml up -d prometheus grafana
```

访问：

```text
Prometheus: http://localhost:9090
Grafana:    http://localhost:3000
```

Grafana 使用 `.env` 中设置的 `GRAFANA_ADMIN_USER` 和 `GRAFANA_ADMIN_PASSWORD`。

Grafana 会自动加载：

```text
Data source: Risk Prometheus
Dashboard:   Risk Management System Overview
```

## 13. RocketMQ 邮件消息说明

当前 `docker-compose.yml` 已内置启动 RocketMQ。

`system-service` 默认配置为：

```text
MQ_ENABLED=true
ROCKETMQ_NAMESRV_ADDR=rocketmq-namesrv:9876
MAIL_MQ_CONSUME_ENABLED=true
MAIL_SEND_ENABLED=false
```

含义：

- 风险系统容器会连接同一个 Docker Compose 网络内的 RocketMQ Nameserver。
- 邮件通知会先进入 RocketMQ，再由消费者分发给 SMTP 邮件发送服务。
- 默认 `MAIL_SEND_ENABLED=false`，本地没有 SMTP 时只验证 MQ 分发，不真实发邮件；配置真实 SMTP 后再改为 `true`。
- 如暂时不使用 MQ，可在 `docker-compose.yml` 的 `system-service.environment` 中改为：

```yaml
MQ_ENABLED: "false"
```

RocketMQ 控制台：

```text
RocketMQ Dashboard: http://localhost:8082
Nameserver:         localhost:9876
```

## 14. 一键脚本方式

如果不想逐步手工执行，也可以使用项目脚本：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\docker-up.ps1
```

该脚本会执行：

1. 停止本机占用端口的进程
2. Maven 构建后端
3. npm 构建前端
4. 编译前端 Docker 服务类
5. 执行 Docker Compose 构建并启动

注意：这是自动化部署方式，不属于纯手工部署。

## 15. 部署验证

查看全部容器：

```powershell
docker compose -f docker-compose.yml ps
```

验证前端：

```text
http://localhost:5173
```

验证网关健康：

```powershell
curl.exe -i http://127.0.0.1:8088/actuator/health
```

期望返回：

```text
HTTP/1.1 200 OK
{"status":"UP"}
```

验证用户服务健康：

```powershell
curl.exe -i http://127.0.0.1:9002/actuator/health
```

验证 Prometheus 指标：

```powershell
curl.exe -s http://127.0.0.1:8088/actuator/prometheus | Select-Object -First 20
curl.exe -s http://127.0.0.1:9002/actuator/prometheus | Select-Object -First 20
```

验证 Eureka：

```text
http://localhost:8761
```

验证 Dozzle 日志：

```text
http://localhost:9999
```

## 16. 登录验证

打开：

```text
http://localhost:5173
```

输入部署人员安全初始化的管理员账号、密码，以及页面当前显示的一次性验证码。系统没有默认密码或固定验证码。

```text
用户名：admin
密码：<部署时设置的密码>
验证码：<页面当前验证码>
```

登录成功后应进入风险管理系统首页。

## 17. 查看日志

查看网关日志：

```powershell
docker logs --tail=100 risk-api-gateway
```

查看用户服务日志：

```powershell
docker logs --tail=100 risk-user-service
```

查看系统服务日志：

```powershell
docker logs --tail=100 risk-system-service
```

实时跟踪日志：

```powershell
docker logs -f risk-api-gateway
```

也可以打开 Dozzle：

```text
http://localhost:9999
```

日志中应包含：

```text
时间
请求接口
线程 ID
txId
SQL_EXEC
TX_START
TX_END
```

## 18. 停止系统

停止并保留数据卷：

```powershell
docker compose -f docker-compose.yml down
```

再次启动时数据仍保留。

## 19. 清理系统

仅在需要重置数据库和监控数据时执行。

停止并删除容器：

```powershell
docker compose -f docker-compose.yml down
```

删除数据卷：

```powershell
docker volume rm user-management-distributed_risk_mysql_data
docker volume rm user-management-distributed_risk_redis_data
docker volume rm user-management-distributed_risk_app_logs
docker volume rm user-management-distributed_risk_prometheus_data
docker volume rm user-management-distributed_risk_grafana_data
```

注意：删除 `risk_mysql_data` 会清空数据库。

## 20. 常见问题

### 20.1 npm.ps1 被禁止执行

报错：

```text
npm : 无法加载文件 npm.ps1
```

处理：

```powershell
npm.cmd install
npm.cmd run build
```

### 20.2 vite 不是内部或外部命令

原因：前端依赖未安装。

处理：

```powershell
cd D:\software\workspace\user-management-distributed\frontend
npm.cmd install
```

### 20.3 Prometheus 或 Grafana 镜像拉取超时

处理：

```powershell
docker pull prom/prometheus:v2.53.4
docker pull grafana/grafana:10.4.3
docker compose -f docker-compose.yml up -d prometheus grafana
```

如果仍超时，需要检查 Docker Desktop 网络或配置镜像加速。

### 20.4 Dozzle 显示容器尚无日志

先确认 Docker 原生日志是否存在：

```powershell
docker logs --tail=50 risk-api-gateway
```

如果 Docker 能看到日志，刷新 Dozzle 页面：

```text
http://localhost:9999
```

如果 Docker 命令也卡住，通常是 Docker Desktop 日志流异常，需要重启 Docker Desktop。

### 20.5 后端服务启动很慢

当前 Spring Boot 服务启动可能需要 1 到 3 分钟。

查看日志：

```powershell
docker logs --tail=100 risk-api-gateway
docker logs --tail=100 risk-user-service
```

看到类似内容表示启动完成：

```text
Started ApiGatewayApplication
Started UserServiceApplication
```

### 20.6 登录后跳回登录页

检查网关和 Redis：

```powershell
docker compose -f docker-compose.yml ps api-gateway redis
docker logs --tail=100 risk-api-gateway
docker logs --tail=100 risk-redis
```

确认浏览器访问的是：

```text
http://localhost:5173
```

不要混用 `localhost` 和其它 IP，避免前端 token 存储和跨域状态混乱。

### 20.7 数据库连接失败

检查 MySQL：

```powershell
docker compose -f docker-compose.yml ps mysql
docker logs --tail=100 risk-mysql
```

如果首次初始化失败，可以清理 MySQL 数据卷后重建：

```powershell
docker compose -f docker-compose.yml down
docker volume rm user-management-distributed_risk_mysql_data
docker compose -f docker-compose.yml up -d mysql
```

注意：该操作会删除数据库数据。

## 21. 推荐部署顺序汇总

```powershell
cd D:\software\workspace\user-management-distributed

docker pull mysql:9.4
docker pull redis:7.4-alpine
docker pull amir20/dozzle:latest

mvn.cmd -f backend\pom.xml -DskipTests clean package

cd frontend
npm.cmd install
npm.cmd run build
javac.exe --release 17 DockerFrontendServer.java
cd ..

docker compose -f docker-compose.yml build discovery-server api-gateway auth-service user-service system-service frontend
docker compose -f docker-compose.yml up -d mysql redis rocketmq-namesrv rocketmq-broker rocketmq-dashboard log-viewer
docker compose -f docker-compose.yml up -d discovery-server
docker compose -f docker-compose.yml up -d api-gateway auth-service user-service system-service
docker compose -f docker-compose.yml up -d frontend

docker compose -f docker-compose.yml ps
```

主系统启动后访问：

```text
http://localhost:5173
```
