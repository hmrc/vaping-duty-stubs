# Returns API

This API provides endpoints for submitting and viewing Vaping Products Duty returns.

## Table of Contents
- [Submit Return](#submit-return)
- [View Return](#view-return)
- [VPD ID Test Error Triggering](#vpd-id-test-error-triggering)
- [Common Error Responses](#common-error-responses)

---

## Submit Return

**Endpoint:** `POST /vaping-products-duty/returns/:periodKey`

Submits a vaping duty return for a specific period.

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `periodKey` | String | Period key for the return (e.g., "27AL") |

### Required Headers

| Header | Description |
|--------|-------------|
| `Authorization` | Bearer token for authentication |
| `x-message-type` | Message type identifier |
| `x-regime-type` | Regime type (VPD) |
| `x-correlation-id` | Correlation ID for request tracking |
| `x-originating-system` | Originating system identifier |
| `x-receipt-date` | Receipt date (ISO 8601 format) |
| `x-transmitting-system` | Transmitting system identifier |
| `x-zvpd` | VPD ID (used for test error triggering) |

### Request Body Example

```json
{
  "periodKey": "27AL",
  "vapingProductsProduced": {
    "vapingProdManufactured": "1",
    "returns": [
      {
        "taxType": "641",
        "dutyRate": 10.50,
        "amountProducedLiquid": 1500.25,
        "dutyDue": 15752.63
      }
    ]
  },
  "totalDutyDue": {
    "totalDue": 1234.56
  },
  "declaration": {
    "fullName": "John Smith",
    "capacityInWhichSigned": "Director",
    "signeesEmailAddress": "john.smith@example.com"
  }
}
```

### Success Response

**Status:** `201 Created`

```json
{
  "success": {
    "processingDate": "2026-06-25T06:14:10.123Z",
    "vpdReferenceNumber": "GBWK0000000WK",
    "submissionID": "01234567-89ab-cdef-0123-456789abcdef",
    "chargeReference": "XMVPD0123456789AB",
    "amount": 1234.56,
    "paymentDueDate": "2026-07-25",
    "declaration": {
      "fullName": "John Smith",
      "capacityInWhichSigned": "Director",
      "signeesEmailAddress": "john.smith@example.com"
    }
  }
}
```

### Test Error Responses (VPD ID Based)

The last digit before the final "WK" in the VPD ID (from `x-zvpd` header) triggers specific error responses:

| Last Digit | Status Code | Error Type | Description | Example VPD ID |
|------------|-------------|------------|-------------|----------------|
| **1** | 400 | Bad Request | Invalid request payload. Missing required field 'periodKey'. | `GBWK0000001WK` |
| **2** | 403 | Forbidden | Forbidden | `GBWK0000002WK` |
| **4** | 409 | Conflict | Duplicate submission | `GBWK0000004WK` |
| **5** | 422 | Unprocessable Entity | Regime missing or invalid | `GBWK0000005WK` |
| **8** | 500 | Internal Server Error | SAP PI system is currently unavailable | `GBWK0000008WK` |

### Example Error Response (Standard Format)

```json
{
  "errorDetail": {
    "errorCode": "400",
    "errorMessage": "Invalid request payload. Missing required field 'periodKey'.",
    "source": "ABCDEF1234567890ABCDEF1234567890"
  }
}
```

### Example Error Response (ETMP Format)

```json
{
  "failures": {
    "code": "004",
    "reason": "Duplicate submission",
    "timestamp": "2026-06-25T06:14:10.123Z"
  }
}
```

### Normal Flow Errors

| Status Code | Scenario | Description |
|-------------|----------|-------------|
| 400 | Invalid JSON | Request body cannot be parsed as valid JSON |
| 400 | Validation Failure | Business validation failed (e.g., invalid period key format, negative amounts) |
| 500 | Repository Error | Database operation failed |

---

## View Return

**Endpoint:** `GET /vaping-products-duty/returns/:vpdReference/:periodKey`

Retrieves a previously submitted return for viewing.

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `vpdReference` | String | VPD ID (used for test error triggering) |
| `periodKey` | String | Period key (e.g., "27AL") |

### Success Response

**Status:** `200 OK`

```json
{
  "processingDate": "2026-06-25T06:14:10.123Z",
  "idDetails": {
    "vpdReference": "GBWK0000000WK",
    "submissionID": "01234567-89ab-cdef-0123-456789abcdef"
  },
  "chargeDetails": {
    "periodKey": "27AL",
    "chargeReference": "XMVPD0123456789AB",
    "periodFrom": "2027-12-01",
    "periodTo": "2027-12-31",
    "receiptDate": "2028-01-15T10:30:00Z"
  },
  "vapingProductsProduced": {
    "vapingProdManufactured": "1",
    "returns": [
      {
        "taxType": "641",
        "dutyRate": 10.50,
        "amountProducedLiquid": 1500.25,
        "dutyDue": 15752.63
      }
    ]
  },
  "totalDutyDue": {
    "totalDue": 1234.56
  },
  "declaration": {
    "fullName": "John Smith",
    "capacityInWhichSigned": "Director",
    "signeesEmailAddress": "john.smith@example.com"
  }
}
```

### Test Error Responses (VPD ID Based)

The last digit before the final "WK" in the VPD ID triggers specific error responses:

| Last Digit | Status Code | Error Type | Description | Example VPD ID |
|------------|-------------|------------|-------------|----------------|
| **1** | 400 | Bad Request | Invalid request payload. Missing required field 'periodKey'. | `GBWK0000001WK` |
| **2** | 403 | Forbidden | Forbidden | `GBWK0000002WK` |
| **3** | 404 | Not Found | Not Found | `GBWK0000003WK` |
| **5** | 422 | Unprocessable Entity | ID Number missing or invalid | `GBWK0000005WK` |
| **8** | 500 | Internal Server Error | SAP PI system is currently unavailable | `GBWK0000008WK` |

### Example Error Response (Standard Format)

```json
{
  "errorDetail": {
    "errorCode": "404",
    "errorMessage": "Not Found",
    "source": "ABCDEF1234567890ABCDEF1234567890"
  }
}
```

### Example Error Response (ETMP Format)

```json
{
  "failures": {
    "code": "002",
    "reason": "ID Number missing or invalid",
    "timestamp": "2026-06-25T06:14:10.123Z"
  }
}
```

### Normal Flow Behavior

When a return is not found in the repository for the given VPD ID and period key, the stub will:
1. Generate 33 return submissions for all fulfilled obligations for that VPD ID
2. Save them to the repository
3. Return the requested period's data if it exists in the generated set
4. Return a minimal response if the period is not in the fulfilled obligations

---

## VPD ID Test Error Triggering

Both returns endpoints support test error triggering based on the VPD ID format. The mechanism extracts the last digit before the "WK" suffix (3rd character from the end) to determine which error response to return.

**VPD ID Format:** `(GB|XI)WK[7 digits]WK`

**Example:** `GBWK0000001WK` → digit is `1` → triggers 400 Bad Request

```scala
// Extract the last digit before "WK" suffix (3rd character from end)
val lastDigit = vpdId.charAt(vpdId.length - 3).toString
```

### Error Testing Examples

| Test Scenario | VPD ID to Use | Expected Result |
|---------------|---------------|-----------------|
| Bad Request | `XIWK0000001WK` | 400 error response |
| Forbidden | `XIWK0000002WK` | 403 error response |
| Not Found (View only) | `XIWK0000003WK` | 404 error response |
| Conflict (Submit only) | `XIWK0000004WK` | 409 error response |
| Unprocessable Entity | `XIWK0000005WK` | 422 error response |
| Internal Server Error | `XIWK0000008WK` | 500 error response |

---

## Common Error Responses

### Standard Error Format

```json
{
  "errorDetail": {
    "errorCode": "string",
    "errorMessage": "string",
    "source": "string"
  }
}
```

### ETMP Error Format

```json
{
  "failures": {
    "code": "string",
    "reason": "string",
    "timestamp": "ISO 8601 timestamp"
  }
}
```

---

## Related Documentation

- [Obligations API](OBLIGATIONS-API.md) - View obligations that require returns
- [VPD ID Examples](VPD-ID-EXAMPLES.md) - VPD ID patterns for testing
- [Main README](../README.md) - Getting started and overview