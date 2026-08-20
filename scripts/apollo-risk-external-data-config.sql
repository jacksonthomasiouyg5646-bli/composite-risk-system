SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
USE ApolloConfigDB;

DROP PROCEDURE IF EXISTS ensure_external_risk_item;
DROP PROCEDURE IF EXISTS publish_external_risk_namespace;

DELIMITER //

CREATE PROCEDURE ensure_external_risk_item(
  IN p_key VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  IN p_value TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  IN p_line_num INT
)
BEGIN
  DECLARE v_namespace_id INT DEFAULT 0;

  SELECT Id INTO v_namespace_id
    FROM ApolloConfigDB.`Namespace`
   WHERE AppId = 'system-service'
     AND ClusterName = 'default'
     AND NamespaceName = 'risk'
     AND IsDeleted = 0
   LIMIT 1;

  IF v_namespace_id = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'system-service/risk namespace does not exist';
  END IF;

  IF EXISTS (SELECT 1 FROM ApolloConfigDB.Item WHERE NamespaceId = v_namespace_id AND `Key` = p_key) THEN
    UPDATE ApolloConfigDB.Item
       SET IsDeleted = 0,
           DeletedAt = NULL,
           DataChange_LastModifiedBy = 'apollo',
           DataChange_LastTime = NOW()
     WHERE NamespaceId = v_namespace_id
       AND `Key` = p_key;
  ELSE
    INSERT INTO ApolloConfigDB.Item
      (NamespaceId, `Key`, Type, Value, Comment, LineNum, IsDeleted, DataChange_CreatedBy, DataChange_CreatedTime, DataChange_LastModifiedBy, DataChange_LastTime)
    VALUES
      (v_namespace_id, p_key, 0, p_value, 'External risk data integration setting', p_line_num, 0, 'apollo', NOW(), 'apollo', NOW());
  END IF;
END//

CREATE PROCEDURE publish_external_risk_namespace()
BEGIN
  DECLARE v_namespace_id INT DEFAULT 0;
  DECLARE v_previous_release_id INT DEFAULT 0;
  DECLARE v_release_id INT DEFAULT 0;
  DECLARE v_release_key VARCHAR(64);
  DECLARE v_configurations MEDIUMTEXT DEFAULT '{}';

  SELECT Id INTO v_namespace_id
    FROM ApolloConfigDB.`Namespace`
   WHERE AppId = 'system-service'
     AND ClusterName = 'default'
     AND NamespaceName = 'risk'
     AND IsDeleted = 0
   LIMIT 1;

  SELECT CONCAT('{', IFNULL(GROUP_CONCAT(CONCAT(JSON_QUOTE(`Key`), ':', JSON_QUOTE(Value)) ORDER BY LineNum SEPARATOR ','), ''), '}')
    INTO v_configurations
    FROM ApolloConfigDB.Item
   WHERE NamespaceId = v_namespace_id
     AND IsDeleted = 0;

  SELECT IFNULL(MAX(Id), 0) INTO v_previous_release_id
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

  SET v_release_key = REPLACE(UUID(), '-', '');
  INSERT INTO ApolloConfigDB.`Release`
    (ReleaseKey, Name, Comment, AppId, ClusterName, NamespaceName, Configurations, IsAbandoned, IsDeleted, DataChange_CreatedBy, DataChange_CreatedTime, DataChange_LastModifiedBy, DataChange_LastTime)
  VALUES
    (v_release_key, CONCAT('risk-external-data-', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s')), 'Add external risk data settings', 'system-service', 'default', 'risk', v_configurations, 0, 0, 'apollo', NOW(), 'apollo', NOW());

  SET v_release_id = LAST_INSERT_ID();
  INSERT INTO ApolloConfigDB.`Commit`
    (ChangeSets, AppId, ClusterName, NamespaceName, Comment, IsDeleted, DataChange_CreatedBy, DataChange_CreatedTime, DataChange_LastModifiedBy, DataChange_LastTime)
  VALUES
    ('{}', 'system-service', 'default', 'risk', 'Add external risk data settings', 0, 'apollo', NOW(), 'apollo', NOW());

  INSERT INTO ApolloConfigDB.ReleaseHistory
    (AppId, ClusterName, NamespaceName, BranchName, ReleaseId, PreviousReleaseId, Operation, OperationContext, IsDeleted, DataChange_CreatedBy, DataChange_CreatedTime, DataChange_LastModifiedBy, DataChange_LastTime)
  VALUES
    ('system-service', 'default', 'risk', 'default', v_release_id, v_previous_release_id, 0, CONCAT('{"releaseKey":"', v_release_key, '"}'), 0, 'apollo', NOW(), 'apollo', NOW());

  INSERT INTO ApolloConfigDB.ReleaseMessage (Message, DataChange_LastTime)
  VALUES ('system-service+default+risk', NOW());
END//

DELIMITER ;

CALL ensure_external_risk_item('risk.ai.external-data.enabled', '${RISK_AI_EXTERNAL_DATA_ENABLED:false}', 7);
CALL ensure_external_risk_item('risk.ai.external-data.provider-name', '${RISK_AI_EXTERNAL_DATA_PROVIDER_NAME:External Risk Data}', 8);
CALL ensure_external_risk_item('risk.ai.external-data.base-url', '${RISK_AI_EXTERNAL_DATA_BASE_URL:http://host.docker.internal:19090}', 9);
CALL ensure_external_risk_item('risk.ai.external-data.api-path', '${RISK_AI_EXTERNAL_DATA_API_PATH:/api/v1/risk-profiles}', 10);
CALL ensure_external_risk_item('risk.ai.external-data.query-path', '${RISK_AI_EXTERNAL_DATA_QUERY_PATH:/query}', 11);
CALL ensure_external_risk_item('risk.ai.external-data.bearer-token', '${RISK_AI_EXTERNAL_DATA_BEARER_TOKEN:}', 12);
CALL ensure_external_risk_item('risk.ai.external-data.api-key', '${RISK_AI_EXTERNAL_DATA_API_KEY:}', 13);
CALL publish_external_risk_namespace();

SELECT `Key`, LENGTH(Value) AS value_length
  FROM ApolloConfigDB.Item
 WHERE NamespaceId = (
   SELECT Id FROM ApolloConfigDB.`Namespace`
    WHERE AppId = 'system-service' AND ClusterName = 'default' AND NamespaceName = 'risk' AND IsDeleted = 0
    LIMIT 1
 )
   AND `Key` LIKE 'risk.ai.external-data.%'
   AND IsDeleted = 0
 ORDER BY LineNum;

DROP PROCEDURE IF EXISTS ensure_external_risk_item;
DROP PROCEDURE IF EXISTS publish_external_risk_namespace;
