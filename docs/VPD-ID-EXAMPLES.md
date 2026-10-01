# VPD ID Examples - Comprehensive Guide

This guide provides complete examples of VPD IDs you can use for testing different scenarios with the Vaping Duty Stubs service.

## Table of Contents
- [Understanding VPD ID Structure](#understanding-vpd-id-structure)
- [XI vs GB Prefix](#xi-vs-gb-prefix)
- [Email and Contact Preferences](#email-and-contact-preferences)
- [Subscription Status Patterns](#subscription-status-patterns)
- [Pre-Seeded Payment Scenarios](#pre-seeded-payment-scenarios)
- [Error Testing VPD IDs](#error-testing-vpd-ids)
- [Complete Working Examples](#complete-working-examples)

---

## Understanding VPD ID Structure

VPD IDs follow this format: `(GB|XI)WK[7 digits]WK`

**Example:** `XIWK0000200WK`

The digits control different aspects of the stubbed response:

| Position | Controls | Values |
|----------|----------|--------|
| **Prefix** (XI/GB) | BTA integration | XI = full integration, GB = BTA stub only |
| **1st digit** | Email preferences & address | 0-9 (see table below) |
| **3rd from end** | Subscription status & insolvency | 2=Approved, 3=Insolvent, 7=Deregistered, 8=Revoked |
| **Last before WK** | Error triggering | 1=400, 2=403, 3=404, 4=409, 5=422, 8=500 |

---

## XI vs GB Prefix

> ⚠️ **Important:** Use **XI prefix** for full end-to-end testing!

| Prefix | BTA Integration | Use For |
|--------|----------------|---------|
| **XI** (e.g., `XIWK...`) | ✅ Full integration with this stub | End-to-end testing, complete user journeys |
| **GB** (e.g., `GBWK...`) | ⚠️ Uses BTA stub (limited) | Limited scenarios, not recommended for E2E |

**Recommendation:** Always use XI-prefixed VPD IDs unless you specifically need to test GB behavior.

---

## Email and Contact Preferences

The **first digit** in the VPD ID controls email preferences, verification status, and address type.

### Complete Email Preferences Matrix

| First Digit | Paperless | Email Verified | Email Bounced | Email Provided | Address Type | Example VPD ID |
|-------------|-----------|----------------|---------------|----------------|--------------|----------------|
| **0** | ✅ Digital | ✅ Yes | ❌ No | ✅ Yes | UK | `XIWK0000200WK` |
| **1** | ❌ Postal | ✅ Yes | ❌ No | ✅ Yes | UK | `XIWK1000200WK` |
| **2** | ❌ Postal | ❌ No | ❌ No | ✅ Yes | UK | `XIWK2000200WK` |
| **3** | ❌ Postal | ❌ No | ✅ **Yes** | ✅ Yes | UK | `XIWK3000200WK` |
| **4** | ❌ Postal | ❌ No | ❌ No | ❌ **None** | UK | `XIWK4000200WK` |
| **5** | ✅ Digital | ✅ Yes | ❌ No | ✅ Yes | 🌍 Canada | `XIWK5000200WK` |
| **6** | ✅ Digital | ✅ Yes | ❌ No | ✅ Yes | 🌍 Spain | `XIWK6000200WK` |
| **7** | ✅ Digital | ✅ Yes | ❌ No | ✅ Yes | 🌍 Hong Kong | `XIWK7000200WK` |
| **8** | ✅ Digital | ✅ Yes | ❌ No | ✅ Yes | No country code | `XIWK8000200WK` |
| **9** | ❌ Postal | ❌ No | ❌ No | ❌ **None** | UK | `XIWK9000200WK` |

### Address Details by First Digit

| Digit | Address Returned |
|-------|------------------|
| **0-4, 9** | Flat 123, 1 Example Road, London, AB1 2CD, GB |
| **5** | Flat 123, 1 Example Road, Toronto, P55555, CA |
| **6** | 1 Example Road, Barcelona, P66666, ES |
| **7** | Flat 123, 1 Example Road, District A, Hong Kong, HK |
| **8** | Building 1, Example City, P88888 (no country code) |

### Common Use Cases

| Scenario | Use This VPD ID | Why |
|----------|----------------|-----|
| Happy path - digital preference | `XIWK0000200WK` | Verified email, digital preference |
| Test email verification flow | `XIWK2000200WK` | Unverified email |
| Test bounced email handling | `XIWK3000200WK` | Only scenario with bounced email |
| Test no email address | `XIWK4000200WK` or `XIWK9000200WK` | No email provided |
| Test overseas address (Canada) | `XIWK5000200WK` | Toronto address |
| Test overseas address (Spain) | `XIWK6000200WK` | Barcelona address |
| Test postal preference | `XIWK1000200WK` | Postal preference, verified email |

---

## Subscription Status Patterns

The **3rd digit from the end** controls subscription approval status and insolvency.

### Status Pattern Matrix

| Pattern | Approval Status | Insolvency | Description | Example VPD ID |
|---------|----------------|------------|-------------|----------------|
| **2**xx | Approved (01) | Not Insolvent (N) | Standard approved subscription | `XIWK0000200WK` |
| **3**xx | Approved (01) | **Insolvent (Y)** | Approved but insolvent | `XIWK0000300WK` |
| **7**xx | Deregistered (04) | Not Insolvent (N) | Subscription deregistered | `XIWK0000700WK` |
| **8**xx | Revoked (05) | Not Insolvent (N) | Subscription revoked | `XIWK0000800WK` |

### Combining Patterns

You can combine email preferences with subscription status:

| VPD ID | Email Digit | Status Digit | Result |
|--------|-------------|--------------|--------|
| `XIWK0000200WK` | 0 (Digital) | 2 (Approved) | Digital preference, approved, not insolvent |
| `XIWK1000300WK` | 1 (Postal) | 3 (Insolvent) | Postal preference, approved, **insolvent** |
| `XIWK2000700WK` | 2 (Unverified) | 7 (Deregistered) | Unverified email, **deregistered** |
| `XIWK5000300WK` | 5 (Canada) | 3 (Insolvent) | Overseas address, approved, **insolvent** |

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

See [TEST-ONLY-ENDPOINTS.md](TEST-ONLY-ENDPOINTS.md) for more details.

---

## Error Testing VPD IDs

The **last digit before the final "WK"** triggers specific error responses.

### Returns API Errors (Submit & View Return)

| Last Digit | Status Code | Error Type | Example VPD ID | Use For |
|------------|-------------|------------|----------------|---------|
| **1** | 400 | Bad Request | `XIWK0000001WK` | Invalid request testing |
| **2** | 403 | Forbidden | `XIWK0000002WK` | Authorization error testing |
| **3** | 404 | Not Found | `XIWK0000003WK` | Not found scenarios (View Return only) |
| **4** | 409 | Conflict | `XIWK0000004WK` | Duplicate submission testing |
| **5** | 422 | Unprocessable Entity | `XIWK0000005WK` | Validation error testing |
| **8** | 500 | Internal Server Error | `XIWK0000008WK` | Downstream error testing |

### Obligations API Errors

| Last Digit | Status Code | Error Type | Example VPD ID |
|------------|-------------|------------|----------------|
| **5** | 422 | Unprocessable Entity | `XIWK0000005WK` |

### Subscription API 422 Errors

Use suffix pattern `5xx` (3rd from end = 5) to trigger 422 errors:

| VPD ID Suffix | Error Code | Error Message |
|---------------|------------|---------------|
| `501` | 001 | REGIME missing or invalid |
| `511` | 011 | ID_TYPE missing or invalid |
| `512` | 012 | ID_VALUE missing or invalid |
| `5xx` (other) | 003 | Request could not be processed |

**Examples:**
- `XIWK0000501WK` → 422 with code 001
- `XIWK0000511WK` → 422 with code 011
- `XIWK0000500WK` → 422 with code 003

---

## Complete Working Examples

### Example 1: Standard Happy Path Testing
```
VPD ID: XIWK0000200WK

What you get:
✅ XI prefix - full BTA integration
✅ Digital preference
✅ Verified email
✅ UK address
✅ Approved subscription
✅ Not insolvent

Use for: End-to-end happy path testing
```

### Example 2: Insolvent Business with Postal Preference
```
VPD ID: XIWK1000300WK

What you get:
✅ XI prefix - full BTA integration
❌ Postal preference (not digital)
✅ Verified email
✅ UK address
✅ Approved subscription
⚠️ INSOLVENT

Use for: Testing insolvency scenarios
```

### Example 3: Unverified Email Testing
```
VPD ID: XIWK2000200WK

What you get:
✅ XI prefix - full BTA integration
❌ Postal preference
❌ Email NOT verified
❌ Email NOT bounced
✅ Email address provided
✅ UK address
✅ Approved subscription

Use for: Email verification flow testing
```

### Example 4: Overseas Business (Canada)
```
VPD ID: XIWK5000200WK

What you get:
✅ XI prefix - full BTA integration
✅ Digital preference
✅ Verified email
🌍 Canada address (Toronto)
✅ Approved subscription

Use for: Testing overseas addresses
```

### Example 5: Error Testing - 400 Bad Request
```
VPD ID: XIWK0000001WK

What you get:
✅ XI prefix - full BTA integration
✅ Digital preference
✅ Verified email
⚠️ Triggers 400 error on Returns API

Use for: Testing error handling in returns submission
```

### Example 6: Pre-Seeded Payment Scenario
```
VPD ID: GBWK0900906WK

What you get:
⚠️ GB prefix - BTA stub only
✅ Pre-seeded financial data
💰 Outstanding charge, not yet due

Use for: Payment journey testing (note: limited BTA integration)
```

---

## Quick Selection Guide

**Choose your VPD ID based on what you're testing:**

| I want to test... | Use this VPD ID |
|-------------------|----------------|
| Standard happy path | `XIWK0000200WK` |
| Email verification flow | `XIWK2000200WK` |
| Bounced email | `XIWK3000200WK` |
| No email address | `XIWK4000200WK` |
| Insolvency | `XIWK1000300WK` |
| Deregistered business | `XIWK0000700WK` |
| Overseas address (Canada) | `XIWK5000200WK` |
| Overseas address (Spain) | `XIWK6000200WK` |
| 400 error on returns | `XIWK0000001WK` |
| 422 error | `XIWK0000005WK` |
| 500 error | `XIWK0000008WK` |
| Payment testing | `GBWK0900906WK` (pre-seeded) |

---

## Need More Control?

If the pre-defined VPD IDs don't meet your needs, you can:

1. **Use test-only endpoints** to set up any VPD ID with custom data
2. **Combine patterns** to create your own VPD IDs

See [TEST-ONLY-ENDPOINTS.md](TEST-ONLY-ENDPOINTS.md) for details on dynamic data setup.