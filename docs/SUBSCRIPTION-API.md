# Subscription API

This API provides access to subscription information for Vaping Products Duty (VPD) registrations.

## Table of Contents
- [Get Subscription Summary](#get-subscription-summary)
- [VPD ID Pattern-Based Responses](#vpd-id-pattern-based-responses)
- [Email Indicator Digit](#email-indicator-digit)
- [Address Indicator Digit](#address-indicator-digit)
- [Subscription Status Patterns](#subscription-status-patterns)
- [422 Unprocessable Entity Scenarios](#422-unprocessable-entity-scenarios)

---

## Get Subscription Summary

**Endpoint:** `GET /etmp/RESTAdapter/vpd/subscription/:vpdId`

Returns subscription information for a VPD ID.

### Example Response

For `vpdId="XIWK1104205WK"`:

```json
{
  "processingDate": "2026-02-25T10:44:26.089402Z",
  "organisationName": "testAwNwaIL Ltd",
  "paperlessPreference": "0",
  "emailAddress": "john.doe@example.com",
  "verifiedEmail": "1",
  "bouncedEmail": "0",
  "addressLine1": "Flat 123",
  "addressLine2": "1 Example Road",
  "postCode": "AB1 2CD",
  "approvalStatus": "01",
  "insolvencyFlag": "0"
}
```

---

## VPD ID Pattern-Based Responses

The stub uses specific digits in the VPD ID to determine response behavior. This allows developers to test different scenarios by simply changing the VPD ID.

### VPD ID Structure

Format: `(GB|XI)WK[7 digits]WK`

Example: `XIWK0000200WK`

| Position | Controls | Values |
|----------|----------|--------|
| **Prefix** (XI/GB) | BTA integration | XI = full integration, GB = BTA stub only |
| **1st digit** | Email preferences & address | 0-9 (see tables below) |
| **3rd from end** | Subscription status & insolvency | 2=Approved, 3=Insolvent, 7=Deregistered, 8=Revoked |
| **Last before WK** | Error triggering | 1=400, 2=403, 5=422, 8=500 |

---

## Email Indicator Digit

The **first digit** in the VPD ID controls email preferences, verification status, and address type.

```scala
val emailFlagDigit = "[0-9]".r.findFirstIn(vpdId).get.toInt
```

Extracts first int from received vpdId.

**Sample vpdId:** `XIWK2104405WK`  
**Sample extracted emailFlagDigit:** `2`

### Email Flag Digit Values

| Cases | Scenario |
| ----- | -------- |
| 0, 5, 6, 7, 8 | PaperlessPreference.Digital (1), verified=true, bounce=false |
| 1             | PaperlessPreference.Postal (0), verified=true, bounce=false  |
| 2             | PaperlessPreference.Postal (0), verified=false, bounce=false |
| 3             | PaperlessPreference.Postal (0), verified=false, bounce=true  |
| 4, 9          | PaperlessPreference.Postal (0), <no email available (false, false)> |

### Complete Email Preferences Matrix

| First Digit | Paperless | Email Verified | Email Bounced | Email Provided | Example VPD ID |
|-------------|-----------|----------------|---------------|----------------|----------------|
| **0** | ✅ Digital | ✅ Yes | ❌ No | ✅ Yes | `XIWK0000200WK` |
| **1** | ❌ Postal | ✅ Yes | ❌ No | ✅ Yes | `XIWK1000200WK` |
| **2** | ❌ Postal | ❌ No | ❌ No | ✅ Yes | `XIWK2000200WK` |
| **3** | ❌ Postal | ❌ No | ✅ **Yes** | ✅ Yes | `XIWK3000200WK` |
| **4** | ❌ Postal | ❌ No | ❌ No | ❌ **None** | `XIWK4000200WK` |
| **5** | ✅ Digital | ✅ Yes | ❌ No | ✅ Yes | `XIWK5000200WK` |
| **6** | ✅ Digital | ✅ Yes | ❌ No | ✅ Yes | `XIWK6000200WK` |
| **7** | ✅ Digital | ✅ Yes | ❌ No | ✅ Yes | `XIWK7000200WK` |
| **8** | ✅ Digital | ✅ Yes | ❌ No | ✅ Yes | `XIWK8000200WK` |
| **9** | ❌ Postal | ❌ No | ❌ No | ❌ **None** | `XIWK9000200WK` |

---

## Address Indicator Digit

The address indicator digit is the same digit as the email indicator digit.

| Case | Descriptor | Resultant Address |
| ---- | ---------- | ----------------- |
| 5    | Overseas address 1 | Flat 123<br>1 Example Road<br>Toronto<br>P55555<br>CA |
| 6    | Overseas address 2 | 1 Example Road<br>Barcelona<br>P66666<br>ES |
| 7    | Country code not in mapping | Flat 123<br>1 Example Road<br>District A<br>Hong Kong<br>HK |
| 8    | No country code    | Building 1<br>Example City<br>P88888 |
| *    | UK address         | Flat 123<br>1 Example Road<br>London<br>AB1 2CD<br>GB |

---

## Subscription Status Patterns

The **third digit from the end** determines the subscription approval status and insolvency state.

### Status Pattern Matrix

| Pattern | Approval Status | Insolvency Status | Description | Example VPD ID |
|---------|----------------|-------------------|-------------|----------------|
| **2**xx | Approved (01) | Not Insolvent (N) | Standard approved subscription | `GBWK0000200WK` |
| **3**xx | Approved (01) | **Insolvent (Y)** | Approved but insolvent | `GBWK0000300WK` |
| **7**xx | Deregistered (04) | Not Insolvent (N) | Subscription deregistered | `GBWK0000700WK` |
| **8**xx | Revoked (05) | Not Insolvent (N) | Subscription revoked | `GBWK0000800WK` |

> **Note:**  
> The approval status (approved/deregistered/revoked) is independent of the Insolvent flag. A manufacturer could be Insolvent from any of these statuses. We have chosen to only represent the Approved + Insolvent case here as this is sufficient to test the insolvent behaviour externally. Unit tests show that the other combinations work.

**Approval Status Codes:**
- `01` = Approved
- `04` = Deregistered  
- `05` = Revoked

**Insolvency Status Values:**
- `Y` = Insolvent
- `N` = Not Insolvent

### Combination Examples

These patterns can be combined with email flags (first digit) for different scenarios:

| VPD ID | Email Flag | Status Pattern | Result |
|--------|-----------|----------------|--------|
| `GBWK0000200WK` | 0 (Digital) | 2 (Approved) | Approved, not insolvent, digital preference |
| `GBWK1000300WK` | 1 (Postal) | 3 (Insolvent) | Approved, insolvent, postal preference |
| `GBWK5000300WK` | 5 (Overseas 1) | 3 (Insolvent) | Approved, insolvent, overseas address 1 |
| `GBWK0000700WK` | 0 (Digital) | 7 (Deregistered) | Deregistered, not insolvent |
| `GBWK2000800WK` | 2 (Unverified) | 8 (Revoked) | Revoked, unverified email |

---

## 422 Unprocessable Entity Scenarios

The suffix digits `5xx` (third-from-last digit `5`) trigger a 422. The last two digits select which real upstream error code is returned:

| Suffix | Code | Text |
| ------ | ---- | ---- |
| 501    | 001  | REGIME missing or invalid |
| 511    | 011  | ID_TYPE missing or invalid |
| 512    | 012  | ID_VALUE missing or invalid |
| any other 5xx (e.g. 500) | 003 | Request could not be processed |

### Example Error Response

```json
{
  "errors": {
    "processingDate": "2026-02-25T10:44:26.089402Z",
    "code": "001",
    "text": "REGIME missing or invalid"
  }
}
```

### Example VPD IDs for 422 Testing

- `XIWK0000501WK` → 422 with code 001 (REGIME missing or invalid)
- `XIWK0000511WK` → 422 with code 011 (ID_TYPE missing or invalid)
- `XIWK0000512WK` → 422 with code 012 (ID_VALUE missing or invalid)
- `XIWK0000500WK` → 422 with code 003 (Request could not be processed)

---

## Related Documentation

- [VPD ID Examples](VPD-ID-EXAMPLES.md) - Complete guide to VPD ID patterns
- [Email Contact Preferences API](EMAIL-CONTACT-PREFERENCES-API.md) - Manage contact preferences
- [Main README](../README.md) - Getting started and overview