# Financial Data API

This API provides access to financial transaction data, including charges, payments, and account balances for Vaping Products Duty.

## Table of Contents
- [Get Financial Data](#get-financial-data)
- [Pre-Seeded Payment Scenarios](#pre-seeded-payment-scenarios)
- [Test Support Endpoints](#test-support-endpoints)
- [Financial Data Scenarios](#financial-data-scenarios)

---

## Get Financial Data

**Endpoint:** `GET /enterprise/financial-data/:regimeType/:idType/:idValue`

Returns financial data including charges, payments, and balances for a VPD registration.

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `regimeType` | String | Regime type (use `VPD` for Vaping Products Duty) |
| `idType` | String | ID type (use `ZVPD` for VPD registrations) |
| `idValue` | String | VPD registration ID (format: `(GB\|XI)WK[7 digits]WK`) |

### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `dateFrom` | String | No | Start date for financial data (ISO 8601 format) |
| `dateTo` | String | No | End date for financial data (ISO 8601 format) |
| `onlyOpenItems` | Boolean | No | If true, only return outstanding items |
| `includeLocks` | Boolean | No | Include locked items |
| `calculateAccruedInterest` | Boolean | No | Calculate accrued interest |
| `customerPaymentInformation` | Boolean | No | Include customer payment information |

### Example Request

```bash
curl "http://localhost:8142/enterprise/financial-data/VPD/ZVPD/GBWK0000200WK?onlyOpenItems=true"
```

---

## Pre-Seeded Payment Scenarios

These VPD IDs have **financial data pre-loaded at startup**. You can use them immediately without calling test-only endpoints.

| VPD ID | Balance State | Outstanding Amount | Use For |
|--------|---------------|-------------------|---------|
| `GBWK0900906WK` | Outstanding, not yet due | Positive | Payment journey testing |
| `GBWK0900907WK` | Outstanding, **overdue** | Positive | Overdue payment scenarios |
| `GBWK0900909WK` | **In credit** | Negative | Credit balance scenarios |
| `GBWK0900900WK` | **Zero balance** | Zero | Nothing owed scenarios |

### Important Notes

✅ **Pre-seeded:** These work immediately after startup  
✅ **Can be overridden:** Use test-only endpoints to change their data  
✅ **Can be applied to any VPD ID:** Use test-only endpoints to set up any VPD ID with these scenarios

**Example: Override a pre-seeded VPD ID**
```bash
curl -X POST http://localhost:8142/test-only/financial-data/GBWK0900906WK/scenario/credit-balance
```

---

## Test Support Endpoints

To dynamically manage financial data for testing, use the test-only endpoints. These are only available when running with test-only routes enabled.

### Enable Test-Only Routes

```bash
sbt run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes
```

### Available Test-Only Endpoints

#### Set Predefined Scenario
```
POST /test-only/financial-data/:vpdId/scenario/:scenario
```

Sets a predefined financial data scenario for a VPD ID.

**Example:**
```bash
curl -X POST http://localhost:8142/test-only/financial-data/GBWK0000200WK/scenario/credit-balance
```

**Response:**
```json
{
  "message": "Successfully set scenario 'credit-balance' for VPD ID GBWK0000200WK",
  "vpdId": "GBWK0000200WK",
  "scenario": "credit-balance",
  "documentCount": 2,
  "noDataIdentified": false
}
```

#### Set Custom Financial Data
```
POST /test-only/financial-data/:vpdId/custom
```

Sets custom financial data for a VPD ID using JSON payload.

**Example:**
```bash
curl -X POST http://localhost:8142/test-only/financial-data/GBWK0000200WK/custom \
  -H "Content-Type: application/json" \
  -d '{
    "vpdId": "GBWK0000200WK",
    "noDataIdentified": false,
    "documentDetails": [
      {
        "chargeReference": "XMVPD0123456789AB",
        "documentType": "ZPAY",
        "documentDescription": "Payment on Account",
        "documentDate": "2026-01-15",
        "documentDueDate": "2026-02-15",
        "documentOutstandingAmount": 1000.00,
        "documentTotalAmount": 1000.00,
        "lineItemDetails": []
      }
    ],
    "lastUpdated": "2026-09-29T16:00:00Z"
  }'
```

**Response:**
```json
{
  "message": "Successfully set custom financial data for VPD ID GBWK0000200WK",
  "vpdId": "GBWK0000200WK",
  "documentCount": 1
}
```

#### Clear Financial Data for VPD ID
```
POST /test-only/financial-data/:vpdId/clear
```

Clears all financial data for a specific VPD ID.

**Example:**
```bash
curl -X POST http://localhost:8142/test-only/financial-data/GBWK0000200WK/clear
```

**Response:**
```json
{
  "message": "Successfully cleared financial data for VPD ID GBWK0000200WK",
  "vpdId": "GBWK0000200WK"
}
```

#### Clear All Financial Data
```
POST /test-only/financial-data/clear-all
```

Clears all financial data from the repository.

**Example:**
```bash
curl -X POST http://localhost:8142/test-only/financial-data/clear-all
```

**Response:**
```json
{
  "message": "Successfully cleared all financial data"
}
```

For complete details on test-only endpoints, see [Test-Only Endpoints](TEST-ONLY-ENDPOINTS.md).

---

## Financial Data Scenarios

### Available Scenarios

| Scenario | Description | Use For |
|----------|-------------|---------|
| `outstanding-only` | Only outstanding charges, no payments | Testing payment journeys |
| `with-unallocated` | Outstanding charges with unallocated payments | Complex payment scenarios |
| `cleared-only` | Only cleared/paid charges | Paid account testing |
| `mixed` | Mix of outstanding, cleared, and unallocated items | Realistic scenarios |
| `none` | No financial data | Empty account testing |
| `single-outstanding` | Single outstanding charge | Simple payment testing |
| `overdue-balance` | Outstanding charges that are overdue | Overdue payment testing |
| `credit-balance` | Account in credit (negative balance) | Credit scenarios |
| `nothing-owed` | Zero balance, nothing outstanding | Zero balance testing |
| `interest-payment` | Charges with interest applied | Interest scenarios |
| `partially-paid` | Charges that have been partially paid | Partial payment testing |
| `overpayment-with-payment-on-account` | Overpayment scenario with payment on account | Overpayment scenarios |

---

## Related Documentation

- [Test-Only Endpoints](TEST-ONLY-ENDPOINTS.md) - Complete guide to test support endpoints
- [VPD ID Examples](VPD-ID-EXAMPLES.md) - VPD ID patterns for testing
- [Main README](../README.md) - Getting started and overview