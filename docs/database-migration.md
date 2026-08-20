# 数据库迁移版本化

组合风险系统从 P2 产品化阶段开始使用 Flyway 管理数据库结构和种子数据版本。

## 目录约定

- `database/migration/V1__baseline_schema_and_seed.sql`：当前全量基线，来源于 `database/init.sql`。
- 后续变更新增 `V2__xxx.sql`、`V3__xxx.sql`，不要修改已发布版本。
- 文件名必须符合 `V数字__英文或下划线描述.sql`。

## Docker 启动顺序

`docker-compose.yml` 中 MySQL 只负责创建空数据库和持久化数据卷：

1. `mysql` 启动并健康检查通过。
2. `flyway` 执行迁移。
3. `bootstrap-admin` 使用 `BOOTSTRAP_ADMIN_PASSWORD_HASH` 启用管理员。
4. `auth-service`、`user-service`、`system-service` 启动。

已有非空库首次接入 Flyway 时使用 `baselineOnMigrate=true` 和 `baselineVersion=1`，只登记基线，不重跑 V1，避免破坏现有数据。

## 变更规则

- 新增表、列、索引：新增版本脚本。
- 修复数据：新增版本脚本并写清楚 WHERE 条件。
- 禁止在 V2 及以后脚本中直接使用裸 `DROP TABLE`。
- 生产迁移前必须备份数据库。
- 应用代码依赖新字段时，先合并并执行数据库迁移，再发布应用镜像。

## 自动校验

GitHub Actions 会执行：

- `scripts/verify-migrations.sh`
- `mvn -B -f backend/pom.xml test -DskipTests`
- `npm ci && npm run build`

本地也可以运行：

```bash
bash scripts/verify-migrations.sh
```
