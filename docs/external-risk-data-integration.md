# External Risk Data Integration

The AI credit analysis endpoint can enrich its local database assessment with an external enterprise-risk provider.

## Activation

Configure the following keys in the Apollo `system-service` / `risk` namespace. The default values preserve local-only analysis.

| Key | Purpose |
| --- | --- |
| `risk.ai.external-data.enabled` | Enables the external query. Set to `true` only after the provider is reachable. |
| `risk.ai.external-data.provider-name` | Provider label displayed in the AI analysis result. |
| `risk.ai.external-data.base-url` | Provider base URL. |
| `risk.ai.external-data.api-path` | Provider API prefix. |
| `risk.ai.external-data.query-path` | Customer risk query path. |
| `risk.ai.external-data.bearer-token` | Optional bearer token. Keep the actual value in a secret-backed runtime variable. |
| `risk.ai.external-data.api-key` | Optional API key. Keep the actual value in a secret-backed runtime variable. |

The default Apollo values resolve from `RISK_AI_EXTERNAL_DATA_*` runtime variables. Do not store real tokens in source code or in a shared configuration export.

## Provider Request

The system sends a `POST` request to:

```text
{base-url}{api-path}{query-path}
```

Only the minimum lookup identity is sent:

```json
{
  "customerNo": "CUST202607210001",
  "customerName": "Example Corporate Customer",
  "scene": "CREDIT_RISK_ANALYSIS"
}
```

`Authorization: Bearer <token>` and `X-Api-Key` are sent only when configured.

## Provider Response

The adapter accepts either a top-level object or a `data` / `result` / `payload` wrapper. Field names may use camel case or snake case.

```json
{
  "data": {
    "providerName": "External Enterprise Data",
    "dataSource": "enterprise-risk",
    "riskScore": 82,
    "creditScore": 610,
    "riskLevel": "HIGH",
    "dishonestPersonFlag": false,
    "sanctionsFlag": false,
    "taxArrearsFlag": true,
    "courtCaseCount": 2,
    "enforcementCount": 1,
    "negativeNewsCount": 3,
    "riskTags": ["tax arrears"],
    "riskSignals": ["tax arrears notice"],
    "updatedAt": "2026-07-21T19:00:00+08:00"
  }
}
```

External risk signals only add evidence to the local assessment. They cannot lower a risk score produced from local overdue, default, rating, exposure, or blacklist records.

## Resilience and Data Boundaries

OpenFeign invokes the provider with a three-second connect timeout and a six-second read timeout. Circuit-breaker fallback returns a local analysis with `external_data.status = UNAVAILABLE`; the credit analysis endpoint remains available.

Do not send contract, drawdown, collateral, customer contact, or identity-document data to the provider through this interface. Any additional field requires data classification, supplier agreement, and security review.
