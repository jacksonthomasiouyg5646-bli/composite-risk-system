# MQ Mail Integration

风险管理系统已将 RocketMQ 纳入 Docker Compose。邮件通知由 `system-service` 负责：通知保存成功后，如果通知渠道为 `EMAIL` 且状态为 `PUBLISHED`，系统会先把邮件消息投递到 RocketMQ，再由 MQ 消费者读取消息并调用 SMTP 发邮件。

## Docker Services

当前 Compose 内置以下 MQ 容器：

- `risk-rocketmq-namesrv`: RocketMQ Nameserver, `localhost:9876`
- `risk-rocketmq-broker`: RocketMQ Broker, `localhost:10909`, `localhost:10911`
- `risk-rocketmq-dashboard`: RocketMQ 控制台, `http://127.0.0.1:8082/`

`system-service` 在 Docker 网络内连接：

```text
ROCKETMQ_NAMESRV_ADDR=rocketmq-namesrv:9876
```

不要在容器内使用 `host.docker.internal:9876` 连接本系统内置 MQ，否则 Broker 地址和控制台可能不是同一套 RocketMQ。

## Configuration

`docker-compose.yml` 中 `system-service` 默认开启 MQ：

```properties
MQ_ENABLED=true
ROCKETMQ_NAMESRV_ADDR=rocketmq-namesrv:9876
MAIL_MQ_TOPIC=risk-mail-topic
MAIL_MQ_TAG=mail-send
MAIL_MQ_PRODUCER_GROUP=risk-mail-producer-group
MAIL_MQ_CONSUMER_GROUP=risk-mail-consumer-group
MAIL_MQ_CONSUME_ENABLED=true
MAIL_SEND_ENABLED=false
```

SMTP 可按实际邮箱服务器配置：

```properties
MAIL_HOST=localhost
MAIL_PORT=25
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_SMTP_AUTH=false
MAIL_SMTP_STARTTLS=false
MAIL_FROM=no-reply@risk.local
```

默认 `MAIL_SEND_ENABLED=false`，表示消费者会从 RocketMQ 取出邮件消息并记录日志，但不会连接真实 SMTP。这样本地没有 SMTP 服务时不会造成消息反复重试。配置好真实 SMTP 后，改成：

```properties
MAIL_SEND_ENABLED=true
```

## Trigger

在前端进入“风险通知”，新增或编辑通知时填写：

- `channel`: `EMAIL`
- `status`: `PUBLISHED`
- `recipients`: 收件人邮箱，多个邮箱用英文逗号或分号分隔

如果不填写 `recipients`，且 `target_type` 为 `ALL`，系统会读取所有启用用户的邮箱作为收件人。

## RocketMQ Console

- 控制台地址：`http://127.0.0.1:8082/`
- Topic：`risk-mail-topic`
- Tag：`mail-send`
- Producer group：`risk-mail-producer-group`
- Consumer group：`risk-mail-consumer-group`

如果消费者正常运行，消息会很快被消费。控制台中优先查看 Topic、Message Trace 和 Consumer 进度。

## Logs

查看系统服务日志：

```powershell
docker logs --tail=120 risk-system-service
```

正常发送时应出现：

```text
RocketMQ mail producer started
Published mail notification to RocketMQ
Consume RocketMQ mail notification
Mail send is disabled, consume MQ mail notification only
```

## Fault Tolerance

通知数据会先落库，再投递 MQ。RocketMQ 临时不可用时，系统会记录错误日志，但不会回滚通知保存。
