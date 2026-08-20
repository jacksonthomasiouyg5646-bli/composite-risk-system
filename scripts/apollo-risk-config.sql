SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
SET SESSION group_concat_max_len = 1048576;

USE ApolloConfigDB;

DROP PROCEDURE IF EXISTS ensure_risk_app;
DROP PROCEDURE IF EXISTS ensure_risk_namespace;
DROP PROCEDURE IF EXISTS ensure_risk_item;
DROP PROCEDURE IF EXISTS publish_risk_namespace;

DELIMITER //

CREATE PROCEDURE ensure_risk_app(
  IN p_app_id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  IN p_name VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci
)
BEGIN
  IF EXISTS (SELECT 1 FROM ApolloConfigDB.App WHERE AppId = p_app_id) THEN
    UPDATE ApolloConfigDB.App
       SET Name = p_name,
           OrgId = 'risk',
           OrgName = 'Risk Management',
           OwnerName = 'apollo',
           OwnerEmail = 'apollo@risk.local',
           IsDeleted = 0,
           DeletedAt = NULL,
           DataChange_LastModifiedBy = 'apollo',
           DataChange_LastTime = NOW()
     WHERE AppId = p_app_id;
  ELSE
    INSERT INTO ApolloConfigDB.App
      (AppId, Name, OrgId, OrgName, OwnerName, OwnerEmail, IsDeleted, DataChange_CreatedBy, DataChange_CreatedTime, DataChange_LastModifiedBy, DataChange_LastTime)
    VALUES
      (p_app_id, p_name, 'risk', 'Risk Management', 'apollo', 'apollo@risk.local', 0, 'apollo', NOW(), 'apollo', NOW());
  END IF;

  IF EXISTS (SELECT 1 FROM ApolloPortalDB.App WHERE AppId = p_app_id) THEN
    UPDATE ApolloPortalDB.App
       SET Name = p_name,
           OrgId = 'risk',
           OrgName = 'Risk Management',
           OwnerName = 'apollo',
           OwnerEmail = 'apollo@risk.local',
           IsDeleted = 0,
           DeletedAt = NULL,
           DataChange_LastModifiedBy = 'apollo',
           DataChange_LastTime = NOW()
     WHERE AppId = p_app_id;
  ELSE
    INSERT INTO ApolloPortalDB.App
      (AppId, Name, OrgId, OrgName, OwnerName, OwnerEmail, IsDeleted, DataChange_CreatedBy, DataChange_CreatedTime, DataChange_LastModifiedBy, DataChange_LastTime)
    VALUES
      (p_app_id, p_name, 'risk', 'Risk Management', 'apollo', 'apollo@risk.local', 0, 'apollo', NOW(), 'apollo', NOW());
  END IF;

  IF EXISTS (SELECT 1 FROM ApolloConfigDB.Cluster WHERE AppId = p_app_id AND Name = 'default') THEN
    UPDATE ApolloConfigDB.Cluster
       SET ParentClusterId = 0,
           Comment = 'Default cluster',
           IsDeleted = 0,
           DeletedAt = NULL,
           DataChange_LastModifiedBy = 'apollo',
           DataChange_LastTime = NOW()
     WHERE AppId = p_app_id AND Name = 'default';
  ELSE
    INSERT INTO ApolloConfigDB.Cluster
      (Name, AppId, ParentClusterId, Comment, IsDeleted, DataChange_CreatedBy, DataChange_CreatedTime, DataChange_LastModifiedBy, DataChange_LastTime)
    VALUES
      ('default', p_app_id, 0, 'Default cluster', 0, 'apollo', NOW(), 'apollo', NOW());
  END IF;
END//

CREATE PROCEDURE ensure_risk_namespace(
  IN p_app_id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  IN p_namespace VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  IN p_comment VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci
)
BEGIN
  IF EXISTS (SELECT 1 FROM ApolloConfigDB.AppNamespace WHERE AppId = p_app_id AND Name = p_namespace) THEN
    UPDATE ApolloConfigDB.AppNamespace
       SET Format = 'properties',
           IsPublic = 0,
           Comment = p_comment,
           IsDeleted = 0,
           DeletedAt = NULL,
           DataChange_LastModifiedBy = 'apollo',
           DataChange_LastTime = NOW()
     WHERE AppId = p_app_id AND Name = p_namespace;
  ELSE
    INSERT INTO ApolloConfigDB.AppNamespace
      (Name, AppId, Format, IsPublic, Comment, IsDeleted, DataChange_CreatedBy, DataChange_CreatedTime, DataChange_LastModifiedBy, DataChange_LastTime)
    VALUES
      (p_namespace, p_app_id, 'properties', 0, p_comment, 0, 'apollo', NOW(), 'apollo', NOW());
  END IF;

  IF EXISTS (SELECT 1 FROM ApolloPortalDB.AppNamespace WHERE AppId = p_app_id AND Name = p_namespace) THEN
    UPDATE ApolloPortalDB.AppNamespace
       SET Format = 'properties',
           IsPublic = 0,
           Comment = p_comment,
           IsDeleted = 0,
           DeletedAt = NULL,
           DataChange_LastModifiedBy = 'apollo',
           DataChange_LastTime = NOW()
     WHERE AppId = p_app_id AND Name = p_namespace;
  ELSE
    INSERT INTO ApolloPortalDB.AppNamespace
      (Name, AppId, Format, IsPublic, Comment, IsDeleted, DataChange_CreatedBy, DataChange_CreatedTime, DataChange_LastModifiedBy, DataChange_LastTime)
    VALUES
      (p_namespace, p_app_id, 'properties', 0, p_comment, 0, 'apollo', NOW(), 'apollo', NOW());
  END IF;

  IF EXISTS (SELECT 1 FROM ApolloConfigDB.`Namespace` WHERE AppId = p_app_id AND ClusterName = 'default' AND NamespaceName = p_namespace) THEN
    UPDATE ApolloConfigDB.`Namespace`
       SET IsDeleted = 0,
           DeletedAt = NULL,
           DataChange_LastModifiedBy = 'apollo',
           DataChange_LastTime = NOW()
     WHERE AppId = p_app_id AND ClusterName = 'default' AND NamespaceName = p_namespace;
  ELSE
    INSERT INTO ApolloConfigDB.`Namespace`
      (AppId, ClusterName, NamespaceName, IsDeleted, DataChange_CreatedBy, DataChange_CreatedTime, DataChange_LastModifiedBy, DataChange_LastTime)
    VALUES
      (p_app_id, 'default', p_namespace, 0, 'apollo', NOW(), 'apollo', NOW());
  END IF;
END//

CREATE PROCEDURE ensure_risk_item(
  IN p_app_id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  IN p_namespace VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  IN p_key VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  IN p_value TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  IN p_line_num INT
)
BEGIN
  DECLARE v_namespace_id INT DEFAULT 0;

  SELECT Id
    INTO v_namespace_id
    FROM ApolloConfigDB.`Namespace`
   WHERE AppId = p_app_id
     AND ClusterName = 'default'
     AND NamespaceName = p_namespace
     AND IsDeleted = 0
   LIMIT 1;

  IF EXISTS (SELECT 1 FROM ApolloConfigDB.Item WHERE NamespaceId = v_namespace_id AND `Key` = p_key) THEN
    UPDATE ApolloConfigDB.Item
       SET Type = 0,
           Value = p_value,
           Comment = 'Managed by scripts/apollo-risk-config.sql',
           LineNum = p_line_num,
           IsDeleted = 0,
           DeletedAt = NULL,
           DataChange_LastModifiedBy = 'apollo',
           DataChange_LastTime = NOW()
     WHERE NamespaceId = v_namespace_id AND `Key` = p_key;
  ELSE
    INSERT INTO ApolloConfigDB.Item
      (NamespaceId, `Key`, Type, Value, Comment, LineNum, IsDeleted, DataChange_CreatedBy, DataChange_CreatedTime, DataChange_LastModifiedBy, DataChange_LastTime)
    VALUES
      (v_namespace_id, p_key, 0, p_value, 'Managed by scripts/apollo-risk-config.sql', p_line_num, 0, 'apollo', NOW(), 'apollo', NOW());
  END IF;
END//

CREATE PROCEDURE publish_risk_namespace(
  IN p_app_id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  IN p_namespace VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci
)
BEGIN
  DECLARE v_namespace_id INT DEFAULT 0;
  DECLARE v_previous_release_id INT DEFAULT 0;
  DECLARE v_release_id INT DEFAULT 0;
  DECLARE v_release_key VARCHAR(64);
  DECLARE v_configurations MEDIUMTEXT DEFAULT '{}';

  SELECT Id
    INTO v_namespace_id
    FROM ApolloConfigDB.`Namespace`
   WHERE AppId = p_app_id
     AND ClusterName = 'default'
     AND NamespaceName = p_namespace
     AND IsDeleted = 0
   LIMIT 1;

  SELECT CONCAT('{', IFNULL(GROUP_CONCAT(CONCAT(JSON_QUOTE(`Key`), ':', JSON_QUOTE(Value)) ORDER BY LineNum SEPARATOR ','), ''), '}')
    INTO v_configurations
    FROM ApolloConfigDB.Item
   WHERE NamespaceId = v_namespace_id
     AND IsDeleted = 0;

  SELECT IFNULL(MAX(Id), 0)
    INTO v_previous_release_id
    FROM ApolloConfigDB.`Release`
   WHERE AppId = p_app_id
     AND ClusterName = 'default'
     AND NamespaceName = p_namespace
     AND IsDeleted = 0
     AND IsAbandoned = 0;

  UPDATE ApolloConfigDB.`Release`
     SET IsAbandoned = 1,
         DataChange_LastModifiedBy = 'apollo',
         DataChange_LastTime = NOW()
   WHERE AppId = p_app_id
     AND ClusterName = 'default'
     AND NamespaceName = p_namespace
     AND IsDeleted = 0
     AND IsAbandoned = 0;

  SET v_release_key = REPLACE(UUID(), '-', '');

  INSERT INTO ApolloConfigDB.`Release`
    (ReleaseKey, Name, Comment, AppId, ClusterName, NamespaceName, Configurations, IsAbandoned, IsDeleted, DataChange_CreatedBy, DataChange_CreatedTime, DataChange_LastModifiedBy, DataChange_LastTime)
  VALUES
    (v_release_key, CONCAT(p_namespace, '-', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s')), 'Risk management local release', p_app_id, 'default', p_namespace, v_configurations, 0, 0, 'apollo', NOW(), 'apollo', NOW());

  SET v_release_id = LAST_INSERT_ID();

  INSERT INTO ApolloConfigDB.`Commit`
    (ChangeSets, AppId, ClusterName, NamespaceName, Comment, IsDeleted, DataChange_CreatedBy, DataChange_CreatedTime, DataChange_LastModifiedBy, DataChange_LastTime)
  VALUES
    ('{}', p_app_id, 'default', p_namespace, 'Risk management local commit', 0, 'apollo', NOW(), 'apollo', NOW());

  INSERT INTO ApolloConfigDB.ReleaseHistory
    (AppId, ClusterName, NamespaceName, BranchName, ReleaseId, PreviousReleaseId, Operation, OperationContext, IsDeleted, DataChange_CreatedBy, DataChange_CreatedTime, DataChange_LastModifiedBy, DataChange_LastTime)
  VALUES
    (p_app_id, 'default', p_namespace, 'default', v_release_id, v_previous_release_id, 0, CONCAT('{"releaseKey":"', v_release_key, '"}'), 0, 'apollo', NOW(), 'apollo', NOW());

  INSERT INTO ApolloConfigDB.ReleaseMessage
    (Message, DataChange_LastTime)
  VALUES
    (CONCAT(p_app_id, '+default+', p_namespace), NOW());
END//

DELIMITER ;

SET @jwt_private_key = '${JWT_RSA_PRIVATE_KEY}';
SET @jwt_public_key = '${JWT_RSA_PUBLIC_KEY}';

CALL ensure_risk_app('discovery-server', 'Risk Discovery Server');
CALL ensure_risk_app('api-gateway', 'Risk API Gateway');
CALL ensure_risk_app('auth-service', 'Risk Auth Service');
CALL ensure_risk_app('user-service', 'Risk User Service');
CALL ensure_risk_app('system-service', 'Risk System Service');

CALL ensure_risk_namespace('discovery-server', 'application', 'Spring application settings');
CALL ensure_risk_namespace('api-gateway', 'application', 'Spring application settings');
CALL ensure_risk_namespace('api-gateway', 'gateway', 'Gateway routes and CORS');
CALL ensure_risk_namespace('api-gateway', 'security', 'Security settings');
CALL ensure_risk_namespace('auth-service', 'application', 'Spring application settings');
CALL ensure_risk_namespace('auth-service', 'database', 'Database settings');
CALL ensure_risk_namespace('auth-service', 'security', 'Security settings');
CALL ensure_risk_namespace('user-service', 'application', 'Spring application settings');
CALL ensure_risk_namespace('user-service', 'database', 'Database settings');
CALL ensure_risk_namespace('user-service', 'security', 'Security settings');
CALL ensure_risk_namespace('system-service', 'application', 'Spring application settings');
CALL ensure_risk_namespace('system-service', 'database', 'Database settings');
CALL ensure_risk_namespace('system-service', 'security', 'Security settings');
CALL ensure_risk_namespace('system-service', 'risk', 'Risk management settings');
CALL ensure_risk_namespace('system-service', 'mq', 'Message queue and mail settings');

CALL ensure_risk_item('discovery-server', 'application', 'server.port', '8761', 1);
CALL ensure_risk_item('discovery-server', 'application', 'spring.application.name', 'discovery-server', 2);
CALL ensure_risk_item('discovery-server', 'application', 'eureka.client.register-with-eureka', 'false', 3);
CALL ensure_risk_item('discovery-server', 'application', 'eureka.client.fetch-registry', 'false', 4);
CALL ensure_risk_item('discovery-server', 'application', 'eureka.server.enable-self-preservation', 'false', 5);
CALL ensure_risk_item('discovery-server', 'application', 'management.endpoints.web.exposure.include', 'health,info,prometheus', 6);
CALL ensure_risk_item('discovery-server', 'application', 'management.metrics.tags.application', 'discovery-server', 7);
CALL ensure_risk_item('discovery-server', 'application', 'logging.level.root', '${LOG_LEVEL:INFO}', 8);

CALL ensure_risk_item('api-gateway', 'application', 'server.port', '${GATEWAY_PORT:8088}', 1);
CALL ensure_risk_item('api-gateway', 'application', 'spring.application.name', 'api-gateway', 2);
CALL ensure_risk_item('api-gateway', 'application', 'eureka.client.service-url.defaultZone', 'http://discovery-server:8761/eureka/', 3);
CALL ensure_risk_item('api-gateway', 'application', 'management.endpoints.web.exposure.include', 'health,info,gateway,prometheus', 4);
CALL ensure_risk_item('api-gateway', 'application', 'management.metrics.tags.application', 'api-gateway', 5);
CALL ensure_risk_item('api-gateway', 'application', 'logging.level.root', '${LOG_LEVEL:INFO}', 6);

CALL ensure_risk_item('api-gateway', 'gateway', 'spring.cloud.gateway.globalcors.add-to-simple-url-handler-mapping', 'true', 1);
CALL ensure_risk_item('api-gateway', 'gateway', 'spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-origin-patterns[0]', 'http://localhost:*', 2);
CALL ensure_risk_item('api-gateway', 'gateway', 'spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-origin-patterns[1]', 'http://127.0.0.1:*', 3);
CALL ensure_risk_item('api-gateway', 'gateway', 'spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-methods[0]', 'GET', 4);
CALL ensure_risk_item('api-gateway', 'gateway', 'spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-methods[1]', 'POST', 5);
CALL ensure_risk_item('api-gateway', 'gateway', 'spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-methods[2]', 'PUT', 6);
CALL ensure_risk_item('api-gateway', 'gateway', 'spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-methods[3]', 'DELETE', 7);
CALL ensure_risk_item('api-gateway', 'gateway', 'spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-methods[4]', 'OPTIONS', 8);
CALL ensure_risk_item('api-gateway', 'gateway', 'spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-headers', '*', 9);
CALL ensure_risk_item('api-gateway', 'gateway', 'spring.cloud.gateway.globalcors.cors-configurations.[/**].exposed-headers', '*', 10);
CALL ensure_risk_item('api-gateway', 'gateway', 'spring.cloud.gateway.globalcors.cors-configurations.[/**].allow-credentials', 'true', 11);
CALL ensure_risk_item('api-gateway', 'gateway', 'spring.cloud.gateway.routes[0].id', 'user-service-users', 12);
CALL ensure_risk_item('api-gateway', 'gateway', 'spring.cloud.gateway.routes[0].uri', 'http://user-service:9002', 13);
CALL ensure_risk_item('api-gateway', 'gateway', 'spring.cloud.gateway.routes[0].predicates[0]', 'Path=/api/users/**,/api/roles/**,/api/permissions/**,/api/departments/**,/api/posts/**,/api/menus/**', 14);
CALL ensure_risk_item('api-gateway', 'gateway', 'spring.cloud.gateway.routes[0].filters[0]', 'StripPrefix=1', 15);
CALL ensure_risk_item('api-gateway', 'gateway', 'spring.cloud.gateway.routes[1].id', 'system-service', 16);
CALL ensure_risk_item('api-gateway', 'gateway', 'spring.cloud.gateway.routes[1].uri', 'http://system-service:9003', 17);
CALL ensure_risk_item('api-gateway', 'gateway', 'spring.cloud.gateway.routes[1].predicates[0]', 'Path=/api/risks/**,/api/logs/**,/api/notifications/**,/api/configs/**,/api/security/**,/api/tenants/**,/api/import/**,/api/export/**', 18);
CALL ensure_risk_item('api-gateway', 'gateway', 'spring.cloud.gateway.routes[1].filters[0]', 'StripPrefix=1', 19);
CALL ensure_risk_item('api-gateway', 'gateway', 'spring.cloud.gateway.routes[2].id', 'auth-service', 20);
CALL ensure_risk_item('api-gateway', 'gateway', 'spring.cloud.gateway.routes[2].uri', 'http://auth-service:9001', 21);
CALL ensure_risk_item('api-gateway', 'gateway', 'spring.cloud.gateway.routes[2].predicates[0]', 'Path=/api/auth/**', 22);
CALL ensure_risk_item('api-gateway', 'gateway', 'spring.cloud.gateway.routes[2].filters[0]', 'StripPrefix=1', 23);

CALL ensure_risk_item('auth-service', 'application', 'server.port', '9001', 1);
CALL ensure_risk_item('auth-service', 'application', 'spring.application.name', 'auth-service', 2);
CALL ensure_risk_item('auth-service', 'application', 'eureka.client.service-url.defaultZone', 'http://discovery-server:8761/eureka/', 3);
CALL ensure_risk_item('auth-service', 'application', 'management.endpoints.web.exposure.include', 'health,info,prometheus', 4);
CALL ensure_risk_item('auth-service', 'application', 'management.metrics.tags.application', 'auth-service', 5);
CALL ensure_risk_item('auth-service', 'application', 'logging.level.root', '${LOG_LEVEL:INFO}', 6);

CALL ensure_risk_item('user-service', 'application', 'server.port', '9002', 1);
CALL ensure_risk_item('user-service', 'application', 'spring.application.name', 'user-service', 2);
CALL ensure_risk_item('user-service', 'application', 'eureka.client.service-url.defaultZone', 'http://discovery-server:8761/eureka/', 3);
CALL ensure_risk_item('user-service', 'application', 'management.endpoints.web.exposure.include', 'health,info,prometheus', 4);
CALL ensure_risk_item('user-service', 'application', 'management.metrics.tags.application', 'user-service', 5);
CALL ensure_risk_item('user-service', 'application', 'logging.level.root', '${LOG_LEVEL:INFO}', 6);

CALL ensure_risk_item('system-service', 'application', 'server.port', '9003', 1);
CALL ensure_risk_item('system-service', 'application', 'spring.application.name', 'system-service', 2);
CALL ensure_risk_item('system-service', 'application', 'eureka.client.service-url.defaultZone', 'http://discovery-server:8761/eureka/', 3);
CALL ensure_risk_item('system-service', 'application', 'management.endpoints.web.exposure.include', 'health,info,prometheus', 4);
CALL ensure_risk_item('system-service', 'application', 'management.metrics.tags.application', 'system-service', 5);
CALL ensure_risk_item('system-service', 'application', 'logging.level.root', '${LOG_LEVEL:INFO}', 6);
CALL ensure_risk_item('system-service', 'application', 'spring.cloud.openfeign.circuitbreaker.enabled', 'true', 7);
CALL ensure_risk_item('system-service', 'application', 'spring.cloud.openfeign.client.refresh-enabled', 'true', 8);

CALL ensure_risk_item('auth-service', 'database', 'spring.datasource.url', '${MYSQL_URL:jdbc:mysql://mysql:3306/user_management?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false}', 1);
CALL ensure_risk_item('auth-service', 'database', 'spring.datasource.username', '${MYSQL_USER:root}', 2);
CALL ensure_risk_item('auth-service', 'database', 'spring.datasource.password', '${MYSQL_PASSWORD}', 3);
CALL ensure_risk_item('auth-service', 'database', 'spring.datasource.driver-class-name', 'com.mysql.cj.jdbc.Driver', 4);

CALL ensure_risk_item('user-service', 'database', 'spring.datasource.url', '${MYSQL_URL:jdbc:mysql://mysql:3306/user_management?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false}', 1);
CALL ensure_risk_item('user-service', 'database', 'spring.datasource.username', '${MYSQL_USER:root}', 2);
CALL ensure_risk_item('user-service', 'database', 'spring.datasource.password', '${MYSQL_PASSWORD}', 3);
CALL ensure_risk_item('user-service', 'database', 'spring.datasource.driver-class-name', 'com.mysql.cj.jdbc.Driver', 4);

CALL ensure_risk_item('system-service', 'database', 'spring.datasource.url', '${MYSQL_URL:jdbc:mysql://mysql:3306/user_management?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false}', 1);
CALL ensure_risk_item('system-service', 'database', 'spring.datasource.username', '${MYSQL_USER:root}', 2);
CALL ensure_risk_item('system-service', 'database', 'spring.datasource.password', '${MYSQL_PASSWORD}', 3);
CALL ensure_risk_item('system-service', 'database', 'spring.datasource.driver-class-name', 'com.mysql.cj.jdbc.Driver', 4);

CALL ensure_risk_item('api-gateway', 'security', 'app.jwt.ttl-seconds', '${JWT_TTL_SECONDS:86400}', 1);
CALL ensure_risk_item('api-gateway', 'security', 'app.jwt.rsa.public-key', @jwt_public_key, 2);
CALL ensure_risk_item('api-gateway', 'security', 'app.jwt.rsa.encryption-enabled', 'false', 3);
CALL ensure_risk_item('api-gateway', 'security', 'app.session.ttl-seconds', '${SESSION_TTL_SECONDS:900}', 5);
CALL ensure_risk_item('api-gateway', 'security', 'spring.data.redis.host', '${REDIS_HOST:redis}', 6);
CALL ensure_risk_item('api-gateway', 'security', 'spring.data.redis.port', '${REDIS_PORT:6379}', 7);
CALL ensure_risk_item('api-gateway', 'security', 'spring.data.redis.password', '${REDIS_PASSWORD}', 8);
CALL ensure_risk_item('api-gateway', 'security', 'spring.data.redis.database', '${REDIS_DATABASE:0}', 9);
CALL ensure_risk_item('api-gateway', 'security', 'app.session.redis-required', 'true', 10);

CALL ensure_risk_item('auth-service', 'security', 'app.jwt.ttl-seconds', '${JWT_TTL_SECONDS:86400}', 1);
CALL ensure_risk_item('auth-service', 'security', 'app.jwt.rsa.private-key', @jwt_private_key, 2);
CALL ensure_risk_item('auth-service', 'security', 'app.jwt.rsa.public-key', @jwt_public_key, 3);
CALL ensure_risk_item('auth-service', 'security', 'app.jwt.rsa.encryption-enabled', 'false', 4);
CALL ensure_risk_item('auth-service', 'security', 'app.session.ttl-seconds', '${SESSION_TTL_SECONDS:900}', 5);
CALL ensure_risk_item('auth-service', 'security', 'spring.data.redis.host', '${REDIS_HOST:redis}', 6);
CALL ensure_risk_item('auth-service', 'security', 'spring.data.redis.port', '${REDIS_PORT:6379}', 7);
CALL ensure_risk_item('auth-service', 'security', 'spring.data.redis.password', '${REDIS_PASSWORD}', 8);
CALL ensure_risk_item('auth-service', 'security', 'spring.data.redis.database', '${REDIS_DATABASE:0}', 9);
CALL ensure_risk_item('auth-service', 'security', 'app.session.redis-required', 'true', 10);

CALL ensure_risk_item('user-service', 'security', 'app.jwt.ttl-seconds', '${JWT_TTL_SECONDS:86400}', 1);
CALL ensure_risk_item('user-service', 'security', 'app.jwt.rsa.public-key', @jwt_public_key, 2);
CALL ensure_risk_item('user-service', 'security', 'app.jwt.rsa.encryption-enabled', 'false', 3);
CALL ensure_risk_item('user-service', 'security', 'app.session.ttl-seconds', '${SESSION_TTL_SECONDS:900}', 4);
CALL ensure_risk_item('user-service', 'security', 'spring.data.redis.host', '${REDIS_HOST:redis}', 5);
CALL ensure_risk_item('user-service', 'security', 'spring.data.redis.port', '${REDIS_PORT:6379}', 6);
CALL ensure_risk_item('user-service', 'security', 'spring.data.redis.password', '${REDIS_PASSWORD}', 7);
CALL ensure_risk_item('user-service', 'security', 'spring.data.redis.database', '${REDIS_DATABASE:0}', 8);
CALL ensure_risk_item('user-service', 'security', 'app.session.redis-required', 'true', 9);
CALL ensure_risk_item('user-service', 'security', 'app.security.internal-service-key', '${INTERNAL_SERVICE_KEY}', 10);

CALL ensure_risk_item('system-service', 'security', 'app.jwt.ttl-seconds', '${JWT_TTL_SECONDS:86400}', 1);
CALL ensure_risk_item('system-service', 'security', 'app.jwt.rsa.public-key', @jwt_public_key, 2);
CALL ensure_risk_item('system-service', 'security', 'app.jwt.rsa.encryption-enabled', 'false', 3);
CALL ensure_risk_item('system-service', 'security', 'app.session.ttl-seconds', '${SESSION_TTL_SECONDS:900}', 4);
CALL ensure_risk_item('system-service', 'security', 'spring.data.redis.host', '${REDIS_HOST:redis}', 5);
CALL ensure_risk_item('system-service', 'security', 'spring.data.redis.port', '${REDIS_PORT:6379}', 6);
CALL ensure_risk_item('system-service', 'security', 'spring.data.redis.password', '${REDIS_PASSWORD}', 7);
CALL ensure_risk_item('system-service', 'security', 'spring.data.redis.database', '${REDIS_DATABASE:0}', 8);
CALL ensure_risk_item('system-service', 'security', 'app.session.redis-required', 'true', 9);
CALL ensure_risk_item('system-service', 'security', 'app.security.internal-service-key', '${INTERNAL_SERVICE_KEY}', 10);

UPDATE ApolloConfigDB.Item i
  JOIN ApolloConfigDB.`Namespace` n ON n.Id = i.NamespaceId
   SET i.IsDeleted = 1,
       i.DeletedAt = UNIX_TIMESTAMP(NOW()) * 1000,
       i.DataChange_LastModifiedBy = 'apollo',
       i.DataChange_LastTime = NOW()
 WHERE n.AppId = 'api-gateway'
   AND n.NamespaceName = 'security'
   AND i.`Key` = 'app.jwt.rsa.private-key';

UPDATE ApolloConfigDB.Item i
  JOIN ApolloConfigDB.`Namespace` n ON n.Id = i.NamespaceId
   SET i.IsDeleted = 1,
       i.DeletedAt = UNIX_TIMESTAMP(NOW()) * 1000,
       i.DataChange_LastModifiedBy = 'apollo',
       i.DataChange_LastTime = NOW()
 WHERE n.AppId IN ('api-gateway', 'auth-service', 'user-service', 'system-service')
   AND n.NamespaceName IN ('application', 'security')
   AND i.`Key` = 'app.jwt.secret';

CALL ensure_risk_item('system-service', 'risk', 'risk.system.name', '组合风险系统', 1);
CALL ensure_risk_item('system-service', 'risk', 'risk.level.matrix', 'likelihood*impact', 2);
CALL ensure_risk_item('system-service', 'risk', 'risk.high.threshold', '12', 3);
CALL ensure_risk_item('system-service', 'risk', 'risk.major.threshold', '16', 4);
CALL ensure_risk_item('system-service', 'risk', 'risk.treatment.overdue.days', '0', 5);
CALL ensure_risk_item('system-service', 'risk', 'risk.assessment.default-status', 'OPEN', 6);
CALL ensure_risk_item('system-service', 'risk', 'risk.ai.external-data.enabled', '${RISK_AI_EXTERNAL_DATA_ENABLED:false}', 7);
CALL ensure_risk_item('system-service', 'risk', 'risk.ai.external-data.provider-name', '${RISK_AI_EXTERNAL_DATA_PROVIDER_NAME:External Risk Data}', 8);
CALL ensure_risk_item('system-service', 'risk', 'risk.ai.external-data.base-url', '${RISK_AI_EXTERNAL_DATA_BASE_URL:http://host.docker.internal:19090}', 9);
CALL ensure_risk_item('system-service', 'risk', 'risk.ai.external-data.api-path', '${RISK_AI_EXTERNAL_DATA_API_PATH:/api/v1/risk-profiles}', 10);
CALL ensure_risk_item('system-service', 'risk', 'risk.ai.external-data.query-path', '${RISK_AI_EXTERNAL_DATA_QUERY_PATH:/query}', 11);
CALL ensure_risk_item('system-service', 'risk', 'risk.ai.external-data.bearer-token', '${RISK_AI_EXTERNAL_DATA_BEARER_TOKEN:}', 12);
CALL ensure_risk_item('system-service', 'risk', 'risk.ai.external-data.api-key', '${RISK_AI_EXTERNAL_DATA_API_KEY:}', 13);
CALL ensure_risk_item('system-service', 'risk', 'spring.cloud.openfeign.client.config.external-risk-data.connectTimeout', '${RISK_AI_EXTERNAL_DATA_CONNECT_TIMEOUT_MS:3000}', 14);
CALL ensure_risk_item('system-service', 'risk', 'spring.cloud.openfeign.client.config.external-risk-data.readTimeout', '${RISK_AI_EXTERNAL_DATA_READ_TIMEOUT_MS:6000}', 15);

CALL ensure_risk_item('system-service', 'mq', 'app.mq.enabled', 'true', 1);
CALL ensure_risk_item('system-service', 'mq', 'app.mq.namesrv-addr', '${ROCKETMQ_NAMESRV_ADDR:rocketmq-namesrv:9876}', 2);
CALL ensure_risk_item('system-service', 'mq', 'app.mq.mail.topic', '${MAIL_MQ_TOPIC:risk-mail-topic}', 3);
CALL ensure_risk_item('system-service', 'mq', 'app.mq.mail.tag', '${MAIL_MQ_TAG:mail-send}', 4);
CALL ensure_risk_item('system-service', 'mq', 'app.mq.mail.producer-group', '${MAIL_MQ_PRODUCER_GROUP:risk-mail-producer-group}', 5);
CALL ensure_risk_item('system-service', 'mq', 'app.mq.mail.consumer-group', '${MAIL_MQ_CONSUMER_GROUP:risk-mail-consumer-group}', 6);
CALL ensure_risk_item('system-service', 'mq', 'app.mq.mail.consume-enabled', '${MAIL_MQ_CONSUME_ENABLED:true}', 7);
CALL ensure_risk_item('system-service', 'mq', 'spring.mail.host', '${MAIL_HOST:localhost}', 8);
CALL ensure_risk_item('system-service', 'mq', 'spring.mail.port', '${MAIL_PORT:25}', 9);
CALL ensure_risk_item('system-service', 'mq', 'spring.mail.username', '${MAIL_USERNAME:}', 10);
CALL ensure_risk_item('system-service', 'mq', 'spring.mail.password', '${MAIL_PASSWORD:}', 11);
CALL ensure_risk_item('system-service', 'mq', 'spring.mail.properties.mail.smtp.auth', '${MAIL_SMTP_AUTH:false}', 12);
CALL ensure_risk_item('system-service', 'mq', 'spring.mail.properties.mail.smtp.starttls.enable', '${MAIL_SMTP_STARTTLS:false}', 13);
CALL ensure_risk_item('system-service', 'mq', 'app.mail.from', '${MAIL_FROM:no-reply@risk.local}', 14);
CALL ensure_risk_item('system-service', 'mq', 'app.mail.send-enabled', '${MAIL_SEND_ENABLED:false}', 15);
CALL ensure_risk_item('system-service', 'mq', 'management.health.mail.enabled', '${MAIL_HEALTH_ENABLED:false}', 16);

-- This namespace is fully managed by this script. Remove legacy RabbitMQ and
-- any other stale keys so applications never resolve obsolete placeholders.
UPDATE ApolloConfigDB.Item i
  JOIN ApolloConfigDB.`Namespace` n ON n.Id = i.NamespaceId
   SET i.IsDeleted = 1,
       i.DeletedAt = UNIX_TIMESTAMP(NOW()) * 1000,
       i.DataChange_LastModifiedBy = 'apollo',
       i.DataChange_LastTime = NOW()
 WHERE n.AppId = 'system-service'
   AND n.NamespaceName = 'mq'
   AND i.IsDeleted = 0
   AND i.`Key` NOT IN (
     'app.mq.enabled',
     'app.mq.namesrv-addr',
     'app.mq.mail.topic',
     'app.mq.mail.tag',
     'app.mq.mail.producer-group',
     'app.mq.mail.consumer-group',
     'app.mq.mail.consume-enabled',
     'spring.mail.host',
     'spring.mail.port',
     'spring.mail.username',
     'spring.mail.password',
     'spring.mail.properties.mail.smtp.auth',
     'spring.mail.properties.mail.smtp.starttls.enable',
     'app.mail.from',
     'app.mail.send-enabled',
     'management.health.mail.enabled'
   );

UPDATE ApolloConfigDB.Item i
  JOIN ApolloConfigDB.`Namespace` n ON n.Id = i.NamespaceId
   SET i.IsDeleted = 1,
       i.DeletedAt = UNIX_TIMESTAMP(NOW()) * 1000,
       i.DataChange_LastModifiedBy = 'apollo',
       i.DataChange_LastTime = NOW()
 WHERE n.AppId = 'system-service'
   AND n.NamespaceName = 'risk'
   AND i.`Key` IN (
     'app.mq.enabled',
     'spring.rabbitmq.host',
     'spring.rabbitmq.port',
     'spring.rabbitmq.username',
     'spring.rabbitmq.password',
     'spring.rabbitmq.virtual-host',
     'app.mq.mail.exchange',
     'app.mq.mail.queue',
     'app.mq.mail.routing-key',
     'app.mq.mail.audit-exchange',
     'app.mq.mail.audit-queue',
     'app.mq.namesrv-addr',
     'app.mq.mail.topic',
     'app.mq.mail.tag',
     'app.mq.mail.producer-group',
     'app.mq.mail.consumer-group',
     'app.mq.mail.consume-enabled',
     'spring.mail.host',
     'spring.mail.port',
     'spring.mail.username',
     'spring.mail.password',
     'spring.mail.properties.mail.smtp.auth',
     'spring.mail.properties.mail.smtp.starttls.enable',
     'app.mail.from'
   );

CALL publish_risk_namespace('discovery-server', 'application');
CALL publish_risk_namespace('api-gateway', 'application');
CALL publish_risk_namespace('api-gateway', 'gateway');
CALL publish_risk_namespace('api-gateway', 'security');
CALL publish_risk_namespace('auth-service', 'application');
CALL publish_risk_namespace('auth-service', 'database');
CALL publish_risk_namespace('auth-service', 'security');
CALL publish_risk_namespace('user-service', 'application');
CALL publish_risk_namespace('user-service', 'database');
CALL publish_risk_namespace('user-service', 'security');
CALL publish_risk_namespace('system-service', 'application');
CALL publish_risk_namespace('system-service', 'database');
CALL publish_risk_namespace('system-service', 'security');
CALL publish_risk_namespace('system-service', 'risk');
CALL publish_risk_namespace('system-service', 'mq');

DROP PROCEDURE IF EXISTS ensure_risk_app;
DROP PROCEDURE IF EXISTS ensure_risk_namespace;
DROP PROCEDURE IF EXISTS ensure_risk_item;
DROP PROCEDURE IF EXISTS publish_risk_namespace;

SELECT AppId, Name, OwnerName
  FROM ApolloConfigDB.App
 WHERE AppId IN ('discovery-server', 'api-gateway', 'auth-service', 'user-service', 'system-service')
 ORDER BY AppId;
