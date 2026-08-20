# 分布式风险管理系统

本项目是一个本地可运行的 Java + Vue 分布式风险管理系统，包含风险台账、风险评估、控制措施、整改任务、风险事件、风险指标以及基础账号权限能力。

## 技术栈

- 后端：Spring Boot 3、Spring Cloud、Eureka、Gateway、JDBC、JWT
- 前端：Vue 3、Vite、Element Plus、Pinia、Vue Router、Axios
- 数据库：MySQL 9，本机 `localhost:3306`

## 默认地址

- 前端：http://localhost:5173
- 网关：http://localhost:8088
- Eureka：http://localhost:8761

## 安全启动要求

系统不再提供可直接登录的默认账号。复制 `.env.example` 到部署环境的安全配置位置，并注入数据库、Redis、JWT、公钥、内部服务密钥和 Grafana 凭据。新建数据库时还必须提供 `BOOTSTRAP_ADMIN_PASSWORD_HASH`；初始化脚本只会用该 BCrypt 哈希启用管理员。

JWT 私钥只允许注入 `auth-service`，网关及业务服务只使用公钥。缺少必需密钥或 Redis 不可用时，认证链路会拒绝启动或拒绝会话，不再降级放行。

## Apollo 配置中心

后端已集成 Apollo Java Client。当前本地 Apollo quick-start 已启动时，启动脚本默认启用 Apollo；如需脱离 Apollo 运行，可手动设置 `APOLLO_ENABLED=false`。

启用 Apollo：

```powershell
$env:APOLLO_ENABLED="true"
$env:APOLLO_META="http://localhost:8080"
$env:GATEWAY_PORT="8088"
scripts\start-backend.cmd
```

本地 Apollo quick-start 默认占用 `8080` 作为 ConfigService，所以本项目网关默认改为 `8088`。Apollo 配置项示例见 `docs\apollo-config-example.md`。

## 初始化数据库

> `database/init.sql` 会删除并重建全部业务表，只能用于全新环境。

先确保 MySQL 已启动：

```powershell
D:\software\mysql\start-mysql.bat
```

然后执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-database.ps1
```

## 启动系统

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-all.ps1
```

也可以分开启动：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-backend.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\start-frontend.ps1
```

如果在 PowerShell 中执行 `npm run` 出现 `npm.ps1` 被禁止加载，使用下面任一方式：

```powershell
npm.cmd run dev
```

或直接运行前端目录里的免策略脚本：

```powershell
cd D:\software\workspace\user-management-distributed\frontend
.\dev.cmd
```

原因是 PowerShell 优先匹配 `npm.ps1`，而当前系统禁止执行 `.ps1` 脚本；`npm.cmd` 不受这个限制。

在 VS Code 中启动前端：

```text
Terminal -> Run Task -> Frontend: dev
```

或者打开“运行和调试”，选择 `Open Vue Frontend`。这些配置会调用 `frontend\dev.cmd`，不会触发 `npm.ps1`。

如果提示 `'vite' 不是内部或外部命令`，说明前端依赖尚未安装。执行：

```powershell
cd D:\software\workspace\user-management-distributed\frontend
npm.cmd install --cache "$env:TEMP\user-management-npm-cache"
npm.cmd run dev
```

## 构建和烟测

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build-backend.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\build-frontend.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
```

## API 路由

- `POST /api/auth/login`
- `GET /api/auth/profile`
- `/api/users`
- `/api/users/{id}/roles`
- `/api/roles`
- `/api/roles/{id}/permissions`
- `/api/permissions`
- `/api/departments`
- `/api/posts`
- `/api/menus`
- `/api/risks/registers`
- `/api/risks/assessments`
- `/api/risks/controls`
- `/api/risks/treatments`
- `/api/risks/events`
- `/api/risks/indicators`
- `/api/logs/login`
- `/api/logs/operation`
- `/api/logs/error`
- `/api/notifications`
- `/api/configs`
- `/api/security/policies`
- `/api/tenants`
- `/api/import/users`
- `/api/export/users`

所有受保护接口都需要请求头：

```text
Authorization: Bearer <token>
```

## JDK Native Access 警告

项目已在 Maven Spring Boot 启动配置和启动脚本中加入：

```text
--enable-native-access=ALL-UNNAMED
```

用于消除 Tomcat JNI 在新版 JDK 下的 `java.lang.System::load` native access 警告。
