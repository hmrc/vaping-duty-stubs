# Test-Only Endpoints

Test-only endpoints allow you to dynamically control stub data for testing. These endpoints are **only available when running with test-only routes enabled**.

## Table of Contents
- [Enabling Test-Only Routes](#enabling-test-only-routes)
- [When to Use Test-Only Endpoints](#when-to-use-test-only-endpoints)
- [Obligations Management](#obligations-management)
- [Financial Data Management](#financial-data-management)
- [Common Workflows](#common-workflows)

---

## Enabling Test-Only Routes

Test-only endpoints are **disabled by default**. To enable them:

```bash
sbt run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes
```

Or with Service Manager:
```bash
sm2 --start VAPING_DUTY_STUBS -Dapplication.router=testOnlyDoNotUseInAppConf.Routes
```

⚠️ **Important:** Test-only routes are never deployed to production environments.

---

## When to Use Test-Only Endpoints

### Use Pre-Defined VPD IDs When:
- ✅ You need quick, standard scenarios
- ✅ The built-in patterns match your test case
- ✅ You're doing exploratory testing

**Example:** `XIWK0000200WK` gives you a standard approved subscription

### Use Test-Only Endpoints When:
- ✅ You need specific obligation dates
- ✅ You want custom financial data
- ✅ You need to test data changes over time
- ✅ Pre-defined VPD IDs don't match your scenario

**Example:** Set up 5 overdue obligations for a specific VPD ID

---

## Obligations Management

### Set Predefined Scenario

**Endpoint:** `POST /test-only/obligations/:vpdId/scenario/:scenario`

Sets a predefined obligation scenario for a VPD ID.

**Available Scenarios:**

| Scenario | Description | Obligations Created |
|----------|-------------|-------------------|
| `only-open` | Only open (unfulfilled) obligations | 3 open returns |
| `only-completed` | Only completed (fulfilled) obligations | 36 completed returns (3 years) |
| `mixed` | Mix of open and completed obligations | 33 completed + 3 open |
| `none` | No obligations | Empty list |
| `error` | Triggers error responses | Error scenario |
| `single-due` | Single open obligation due in future | 1 open return |
| `single-due-with-completed` | Single open + completed obligations | 1 open + 3 completed |
| `single-due-one-overdue` | One due soon, one overdue | 1 due + 1 overdue + 3 completed |
| `single-due-multiple-overdue` | One due soon, multiple overdue | 1 due + multiple overdue + 3 completed |

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

### Scenario Details

#### only-open
Creates 3 open returns with no completed returns:
- One return due in 10 days (period 27AL)
- One return overdue by 5 days (period 27AK)
- One return due in 30 days (period 28AA)

#### only-completed
Creates 36 completed returns (3 years of monthly obligations):
- All obligations fulfilled with associated return submissions

#### mixed
Creates a mix of 36 obligations spanning 3 years:
- 33 completed returns
- 3 open returns (one due soon, one overdue, one due later)

#### single-due
Creates a single open obligation due in the future with no completed returns.

#### single-due-with-completed
Creates a single open obligation plus 3 completed obligations with associated returns.

#### single-due-one-overdue
Creates one open obligation due soon, one overdue obligation, and 3 completed obligations with returns.

#### single-due-multiple-overdue
Creates one open obligation due soon, multiple overdue obligations, and 3 completed obligations with returns.

### Set Custom Obligations

**Endpoint:** `POST /test-only/obligations/:vpdId/custom`

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

**Response:**
```json
{
  "message": "Successfully set custom obligations for VPD ID XIWK0904905WK",
  "vpdId": "XIWK0904905WK",
  "obligationCount": 1
}
```

### Clear Obligations for VPD ID

**Endpoint:** `POST /test-only/obligations/:vpdId/clear`

Clears all obligations for a specific VPD ID.

**Example:**
```bash
curl -X POST http://localhost:8142/test-only/obligations/XIWK0904905WK/clear
```

**Response:**
```json
{
  "message": "Successfully cleared obligations for VPD ID XIWK0904905WK",
  "vpdId": "XIWK0904905WK"
}
```

### Clear All Obligations

**Endpoint:** `POST /test-only/obligations/clear-all`

Clears all obligations data from the repository.

**Example:**
```bash
curl -X POST http://localhost:8142/test-only/obligations/clear-all
```

**Response:**
```json
{
  "message": "Successfully cleared all obligations and returns data"
}
```

---

## Financial Data Management

### Set Predefined Scenario

**Endpoint:** `POST /test-only/financial-data/:vpdId/scenario/:scenario`

Sets a predefined financial data scenario for a VPD ID.

**Available Scenarios:**

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

**Example:**
```bash
curl -X POST http://localhost:8142/test-only/financial-data/XIWK0000200WK/scenario/credit-balance
```

**Response:**
```json
{
  "message": "Successfully set scenario 'credit-balance' for VPD ID XIWK0000200WK",
  "vpdId": "XIWK0000200WK",
  "scenario": "credit-balance",
  "documentCount": 2,
  "noDataIdentified": false
}
```

### Set Custom Financial Data

**Endpoint:** `POST /test-only/financial-data/:vpdId/custom`

Sets custom financial data for a VPD ID using JSON payload.

**Example:**
```bash
curl -X POST http://localhost:8142/test-only/financial-data/XIWK0000200WK/custom \
  -H "Content-Type: application/json" \
  -d '{
    "vpdId": "XIWK0000200WK",
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
  "message": "Successfully set custom financial data for VPD ID XIWK0000200WK",
  "vpdId": "XIWK0000200WK",
  "documentCount": 1
}
```

### Clear Financial Data for VPD ID

**Endpoint:** `POST /test-only/financial-data/:vpdId/clear`

Clears all financial data for a specific VPD ID.

**Example:**
```bash
curl -X POST http://localhost:8142/test-only/financial-data/XIWK0000200WK/clear
```

**Response:**
```json
{
  "message": "Successfully cleared financial data for VPD ID XIWK0000200WK",
  "vpdId": "XIWK0000200WK"
}
```

### Clear All Financial Data

**Endpoint:** `POST /test-only/financial-data/clear-all`

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

---

## Common Workflows

### Workflow 1: Testing a Complete User Journey

```bash
# 1. Set up obligations with some completed and some open
curl -X POST http://localhost:8142/test-only/obligations/XIWK0000200WK/scenario/mixed

# 2. Set up financial data showing outstanding balance
curl -X POST http://localhost:8142/test-only/financial-data/XIWK0000200WK/scenario/single-outstanding

# 3. Now test your journey with XIWK0000200WK
# The VPD ID will have:
# - 33 completed obligations + 3 open obligations
# - A single outstanding payment
```

### Workflow 2: Testing Overdue Scenarios

```bash
# Set up multiple overdue obligations
curl -X POST http://localhost:8142/test-only/obligations/XIWK1000300WK/scenario/single-due-multiple-overdue

# Set up overdue financial data
curl -X POST http://localhost:8142/test-only/financial-data/XIWK1000300WK/scenario/overdue-balance

# Now XIWK1000300WK has:
# - Multiple overdue obligations
# - Overdue payments
# - Plus it's insolvent (from the 3 in the VPD ID pattern)
```

### Workflow 3: Testing Data Changes Over Time

```bash
# Day 1: Start with no obligations
curl -X POST http://localhost:8142/test-only/obligations/XIWK2000200WK/scenario/none

# Day 2: Add a single obligation
curl -X POST http://localhost:8142/test-only/obligations/XIWK2000200WK/scenario/single-due

# Day 3: Mark it as completed and add more
curl -X POST http://localhost:8142/test-only/obligations/XIWK2000200WK/scenario/single-due-with-completed
```

### Workflow 4: Resetting to Clean State

```bash
# Clear everything for a VPD ID
curl -X POST http://localhost:8142/test-only/obligations/XIWK0000200WK/clear
curl -X POST http://localhost:8142/test-only/financial-data/XIWK0000200WK/clear

# Or clear everything for all VPD IDs
curl -X POST http://localhost:8142/test-only/obligations/clear-all
curl -X POST http://localhost:8142/test-only/financial-data/clear-all
```

### Workflow 5: Override Pre-Seeded Payment Scenarios

```bash
# The pre-seeded VPD ID GBWK0900906WK has "outstanding not yet due"
# But you can override it:
curl -X POST http://localhost:8142/test-only/financial-data/GBWK0900906WK/scenario/credit-balance

# Now GBWK0900906WK shows a credit balance instead
```

---

## Tips and Best Practices

### 1. Use Meaningful VPD IDs
Choose VPD IDs that make sense for your test:
- Use XI prefix for full BTA integration
- Use the first digit to control email preferences
- Use the 3rd from end to control subscription status

**Example:** `XIWK2000300WK`
- XI = full BTA
- 2 = unverified email
- 3 = insolvent
- Then use test-only endpoints to add obligations/financial data

### 2. Combine Pre-Defined Patterns with Test-Only Endpoints

```bash
# Use XIWK1000300WK for:
# - Postal preference (digit 1)
# - Insolvent (digit 3)
# Then add custom obligations:
curl -X POST http://localhost:8142/test-only/obligations/XIWK1000300WK/scenario/single-due-multiple-overdue
```

### 3. Clear Data Between Test Runs

Always clear data between test runs to avoid interference:
```bash
curl -X POST http://localhost:8142/test-only/obligations/XIWK0000200WK/clear
curl -X POST http://localhost:8142/test-only/financial-data/XIWK0000200WK/clear
```

### 4. Use Scenarios for Quick Setup

Don't create custom JSON unless you need specific dates/amounts:
```bash
# Quick setup - use a scenario
curl -X POST http://localhost:8142/test-only/obligations/XIWK0000200WK/scenario/mixed

# Custom setup - only when you need specific control
curl -X POST http://localhost:8142/test-only/obligations/XIWK0000200WK/custom \
  -H "Content-Type: application/json" \
  -d '{ ... specific dates ... }'
```

---

## Related Documentation

- [VPD ID Examples](VPD-ID-EXAMPLES.md) - Complete guide to VPD ID patterns
- [Main README](../README.md) - Getting started and overview

---

## Troubleshooting

### Test-Only Routes Not Available

**Problem:** Getting 404 on test-only endpoints

**Solution:** Ensure you're running with test-only routes enabled:
```bash
sbt run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes
```

### Data Not Persisting

**Problem:** Data disappears after restart

**Solution:** This is expected behavior. Test-only data is stored in MongoDB but cleared on restart. Re-run your setup commands after restart.

### VPD ID Mismatch Error

**Problem:** Getting "VPD ID mismatch" error

**Solution:** Ensure the VPD ID in the URL matches the VPD ID in the JSON body:
```bash
# ❌ Wrong - IDs don't match
curl -X POST http://localhost:8142/test-only/obligations/XIWK0000200WK/custom \
  -d '{"vpdId": "XIWK0000300WK", ...}'

# ✅ Correct - IDs match
curl -X POST http://localhost:8142/test-only/obligations/XIWK0000200WK/custom \
  -d '{"vpdId": "XIWK0000200WK", ...}'
```

### Invalid Scenario Name

**Problem:** Getting "Invalid scenario" error

**Solution:** Check the scenario name matches exactly (case-sensitive):
```bash
# ❌ Wrong
curl -X POST http://localhost:8142/test-only/obligations/XIWK0000200WK/scenario/Mixed

# ✅ Correct
curl -X POST http://localhost:8142/test-only/obligations/XIWK0000200WK/scenario/mixed