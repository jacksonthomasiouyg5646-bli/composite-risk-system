# Apollo 配置中心示例

## 强制启动顺序与配置边界

1. 先启动 Apollo DB、ConfigService 和 Portal，并确认 `http://127.0.0.1:8080/services/config` 返回 200。
2. 执行 `scripts/publish-apollo-config.ps1`，发布并校验 5 个 App、15 个 namespace。
3. 只有发布校验成功后才能启动 discovery-server、网关和业务微服务。
4. 微服务容器仅允许保留 `APOLLO_ENABLED`、`APOLLO_REQUIRED`、`APOLLO_META`、`APOLLO_NAMESPACES` 四个 Apollo 引导参数；数据库、Redis、JWT、内部服务密钥、路由、Eureka、风险、MQ 和邮件配置全部从 Apollo 获取。
5. MySQL/Redis 容器自身的启动密码以及 Apollo 连接参数属于配置中心启动前的基础设施引导配置，不能从 Apollo 反向获取。

推荐统一启动命令：

```powershell
.\scripts\docker-up.ps1 -EnvFile .\.env -ApolloComposeFile D:\software\apollo\compose.yaml
```

该脚本会在 Apollo 不可用、namespace 未发布、关键键缺失或存在未解析占位符时停止，不会继续启动组合风险微服务。

启动后端前建议设置：

```powershell
$env:APOLLO_ENABLED="true"
$env:APOLLO_META="http://localhost:8080"
$env:GATEWAY_PORT="8088"
```

本地 Apollo quick-start 默认占用 `8080` 作为 ConfigService，所以本项目网关默认使用 `8088`。

## AppId

- `discovery-server`
- `api-gateway`
- `auth-service`
- `user-service`
- `system-service`

## Security Namespace

JWT 已改为 RSA：

```properties
app.jwt.ttl-seconds=86400
app.jwt.rsa.public-key=<X.509 public key base64>
app.jwt.rsa.private-key=<PKCS#8 private key base64>
app.jwt.rsa.encryption-enabled=true
```

配置原则：

- `auth-service`：需要私钥和公钥，用于签发 token。
- `api-gateway`：需要私钥和公钥，用于解密浏览器提交的加密 token，并转发签名 token 给业务服务。
- `user-service`、`system-service`：只需要公钥，用于验签。

## Database Namespace

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/user_management?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
spring.datasource.username=root
spring.datasource.password=change-me
```

## Gateway Namespace

```properties
spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-origin-patterns[0]=http://localhost:*
spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-origin-patterns[1]=http://127.0.0.1:*
```

## Risk Namespace

```properties
risk.level.matrix=likelihood*impact
risk.high.threshold=12
risk.major.threshold=16
risk.treatment.overdue.days=0
```
