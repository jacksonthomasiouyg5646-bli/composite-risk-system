SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
USE ApolloConfigDB;

SELECT Id INTO @namespace_id
  FROM ApolloConfigDB.`Namespace`
 WHERE AppId = 'system-service'
   AND ClusterName = 'default'
   AND NamespaceName = 'risk'
   AND IsDeleted = 0
 LIMIT 1;

UPDATE ApolloConfigDB.Item
   SET Value = '组合风险系统',
       Comment = '组合风险系统名称',
       IsDeleted = 0,
       DeletedAt = NULL,
       DataChange_LastModifiedBy = 'apollo',
       DataChange_LastTime = NOW()
 WHERE NamespaceId = @namespace_id
   AND `Key` = 'risk.system.name';

SELECT CONCAT('{', IFNULL(GROUP_CONCAT(CONCAT(JSON_QUOTE(`Key`), ':', JSON_QUOTE(Value)) ORDER BY LineNum SEPARATOR ','), ''), '}')
  INTO @configurations
  FROM ApolloConfigDB.Item
 WHERE NamespaceId = @namespace_id
   AND IsDeleted = 0;

SELECT IFNULL(MAX(Id), 0) INTO @previous_release_id
  FROM ApolloConfigDB.`Release`
 WHERE AppId = 'system-service'
   AND ClusterName = 'default'
   AND NamespaceName = 'risk'
   AND IsDeleted = 0
   AND IsAbandoned = 0;

UPDATE ApolloConfigDB.`Release`
   SET IsAbandoned = 1,
       DataChange_LastModifiedBy = 'apollo',
       DataChange_LastTime = NOW()
 WHERE AppId = 'system-service'
   AND ClusterName = 'default'
   AND NamespaceName = 'risk'
   AND IsDeleted = 0
   AND IsAbandoned = 0;

SET @release_key = REPLACE(UUID(), '-', '');
INSERT INTO ApolloConfigDB.`Release`
  (ReleaseKey, Name, Comment, AppId, ClusterName, NamespaceName, Configurations, IsAbandoned, IsDeleted, DataChange_CreatedBy, DataChange_CreatedTime, DataChange_LastModifiedBy, DataChange_LastTime)
VALUES
  (@release_key, CONCAT('risk-system-name-', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s')), '更新组合风险系统名称', 'system-service', 'default', 'risk', @configurations, 0, 0, 'apollo', NOW(), 'apollo', NOW());

SET @release_id = LAST_INSERT_ID();
INSERT INTO ApolloConfigDB.`Commit`
  (ChangeSets, AppId, ClusterName, NamespaceName, Comment, IsDeleted, DataChange_CreatedBy, DataChange_CreatedTime, DataChange_LastModifiedBy, DataChange_LastTime)
VALUES
  ('{}', 'system-service', 'default', 'risk', '更新组合风险系统名称', 0, 'apollo', NOW(), 'apollo', NOW());

INSERT INTO ApolloConfigDB.ReleaseHistory
  (AppId, ClusterName, NamespaceName, BranchName, ReleaseId, PreviousReleaseId, Operation, OperationContext, IsDeleted, DataChange_CreatedBy, DataChange_CreatedTime, DataChange_LastModifiedBy, DataChange_LastTime)
VALUES
  ('system-service', 'default', 'risk', 'default', @release_id, @previous_release_id, 0, CONCAT('{"releaseKey":"', @release_key, '"}'), 0, 'apollo', NOW(), 'apollo', NOW());

INSERT INTO ApolloConfigDB.ReleaseMessage (Message, DataChange_LastTime)
VALUES ('system-service+default+risk', NOW());

SELECT `Key`, Value
  FROM ApolloConfigDB.Item
 WHERE NamespaceId = @namespace_id
   AND `Key` = 'risk.system.name'
   AND IsDeleted = 0;
