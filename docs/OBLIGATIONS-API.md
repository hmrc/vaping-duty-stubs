# Obligations API

This API provides access to obligation periods and their fulfillment status for Vaping Products Duty returns.

## Table of Contents
- [Get Obligations](#get-obligations)
- [Test Error Responses](#test-error-responses)
- [Normal Flow Behavior](#normal-flow-behavior)
- [Test Support Endpoints](#test-support-endpoints)

---

## Get Obligations

**Endpoint:** `GET /etmp/obligations/:vpdId`

Returns obligation data for a VPD ID, including both open (unfulfilled) and completed (fulfilled) obligations.

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `vpdId` | String | VPD registration ID (format: `(GB\|XI)WK[7 digits]WK`) |

### Response Format

```json
{
  "obligations": [
    {
      "identification": null,
      "obligationDetails": {
        "openOrFulfilledStatus": "O",
        "iCFromDate": "2027-12-01",
        "iCToDate": "2027-12-31",
        "iCDateReceived": null,
        "iCDueDate": "2028-01-31",
        "periodKey": "27AL"
      }
    }
  ]
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `openOrFulfilledStatus` | String | `O` = Open (unfulfilled), `F` = Fulfilled (completed) |
| `iCFromDate` | String | Period start date (ISO 8601 format) |
| `iCToDate` | String | Period end date (ISO 8601 format) |
| `iCDateReceived` | String\|null | Date return was received (null for open obligations) |
| `iCDueDate` | String | Due date for return submission |
| `periodKey` | String | Unique period identifier (e.g., "27AL") |

---

## Test Error Responses

The last digit of the VPD ID (before the "WK" suffix) determines the response behavior for testing error scenarios.

### VPD ID Error Triggering

| Last Digit | Status Code | Error Type | Description | Example VPD ID |
|------------|-------------|------------|-----------------------|----------------|
| 5 | 422 | Unprocessable Entity | Simulated obligations unprocessable entity | `GBWK0000005WK` |

### Example Error Response

```json
{
  "code": "INVALID_REGIME",
  "reason": "Simulated obligations unprocessable entity"
}
```

---

## Normal Flow Behavior

For all other VPD IDs (not triggering error responses), the endpoint returns obligation data based on:

1. **Pre-seeded data** - Some VPD IDs have obligations pre-loaded at startup
2. **Test-only endpoints** - Use test support endpoints to set custom scenarios
3. **Default behavior** - Returns empty obligations list if no data is configured

### Default Response (No Data)

```json
{
  "obligations": []
}
```

---

## Test Support Endpoints

To dynamically manage obligation data for testing, use the test-only endpoints. These are only available when running with test-only routes enabled.

### Enable Test-Only Routes

```bash
sbt run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes
```

### Available Test-Only Endpoints

#### Set Predefined Scenario
```
POST /test-only/obligations/:vpdId/scenario/:scenario
```

Sets a predefined obligation scenario for a VPD ID.

**Available scenarios:**
- `only-open` - Only open (unfulfilled) obligations
- `only-completed` - Only completed (fulfilled) obligations
- `mixed` - Mix of open and completed obligations
- `none` - No obligations
- `error` - Triggers error responses for testing error handling
- `single-due` - Single open obligation due in the future
- `single-due-with-completed` - Single open obligation plus 3 completed obligations
- `single-due-one-overdue` - One open due soon, one overdue, and 3 completed obligations
- `single-due-multiple-overdue` - One open due soon, multiple overdue, and 3 completed obligations

**Example:**
```bash
curl -X POST http://localhost:8142/test-only/obligations/XIWK0904905WK/scenario/mixed
```

**Response:**
```json
{
  "message": "Successfully set scenario 'mixed' for VPD ID XIWK0904905WK",
  "vpdId": "XIWK0904905WK",
  "scenario": "mixed",
  "obligationCount": 36
}
```

#### Set Custom Obligations
```
POST /test-only/obligations/:vpdId/custom
```

Sets custom obligations for a VPD ID using JSON payload.

**Example:**
```bash
curl -X POST http://localhost:8142/test-only/obligations/XIWK0904905WK/custom \
  -H "Content-Type: application/json" \
  -d '{
    "vpdId": "XIWK0904905WK",
    "obligations": [
      {
        "identification": null,
        "obligationDetails": {
          "openOrFulfilledStatus": "O",
          "iCFromDate": "2027-12-01",
          "iCToDate": "2027-12-31",
          "iCDateReceived": null,
          "iCDueDate": "2028-01-31",
          "periodKey": "27AL"
        }
      }
    ]
  }'
```

#### Clear Obligations for VPD ID
```
POST /test-only/obligations/:vpdId/clear
```

Clears all obligations for a specific VPD ID.

**Example:**
```bash
curl -X POST http://localhost:8142/test-only/obligations/XIWK0904905WK/clear
```

#### Clear All Obligations
```
POST /test-only/obligations/clear-all
```

Clears all obligations data from the repository.

**Example:**
```bash
curl -X POST http://localhost:8142/test-only/obligations/clear-all
```

**Response:**
```json
{
  "message": "Successfully cleared all obligations data"
}
```

For complete details on test-only endpoints, see [Test-Only Endpoints](TEST-ONLY-ENDPOINTS.md).

---

## Related Documentation

- [Test-Only Endpoints](TEST-ONLY-ENDPOINTS.md) - Complete guide to test support endpoints
- [Returns API](RETURNS-API.md) - Submit and view returns for obligations
- [VPD ID Examples](VPD-ID-EXAMPLES.md) - VPD ID patterns for testing
- [Main README](../README.md) - Getting started and overview