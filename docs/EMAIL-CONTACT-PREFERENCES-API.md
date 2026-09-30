# Email Contact Preferences API

This API provides endpoints for managing email contact preferences for Vaping Products Duty registrations.

## Table of Contents
- [Get Contact Preferences](#get-contact-preferences)
- [Update Contact Preferences](#update-contact-preferences)
- [Error Scenarios](#error-scenarios)
- [Test Support](#test-support)

---

## Get Contact Preferences

**Endpoint:** `GET /vaping-products-duty/subscription/contact-preference/:vpdId`

Retrieves the current contact preferences for a VPD registration.

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `vpdId` | String | VPD registration ID (format: `(GB\|XI)WK[7 digits]WK`) |

### Success Response

**Status:** `200 OK`

```json
{
  "paperlessPreference": true,
  "emailVerification": {
    "emailAddress": "user@example.com",
    "emailVerified": true
  }
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `paperlessPreference` | Boolean | Whether paperless (digital) preference is enabled |
| `emailVerification.emailAddress` | String | The registered email address |
| `emailVerification.emailVerified` | Boolean | Whether the email has been verified |

---

## Update Contact Preferences

**Endpoint:** `PUT /etmp/RESTAdapter/email-contact-preference/:regime/:idType/:idValue`

Updates the contact preferences for a VPD registration.

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `regime` | String | Regime type (must be `VPD`) |
| `idType` | String | ID type (must be `ZVPD`) |
| `idValue` | String | VPD registration ID (format: `(GB\|XI)WK[7 digits]WK`) |

### Request Body

```json
{
  "paperlessPreference": true,
  "emailVerification": {
    "emailAddress": "newemail@example.com",
    "emailVerified": true
  }
}
```

### Request Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `paperlessPreference` | Boolean | Yes | Whether to enable paperless (digital) preference |
| `emailVerification` | Object | Conditional | Required if `paperlessPreference` is true |
| `emailVerification.emailAddress` | String | Conditional | Email address (required if emailVerification provided) |
| `emailVerification.emailVerified` | Boolean | Conditional | Email verification status (required if emailVerification provided) |

### Success Response

**Status:** `200 OK`

```json
{
  "processingDate": "2026-09-30T10:00:00Z"
}
```

---

## Error Scenarios

The stub uses the **second digit** of `idValue` to determine the response behavior for testing error scenarios.

### IdValue Digit Pattern

```scala
val stubIndex = idValue.charAt(1).toString.toInt
```

**Example:** `GBWK0904905WK` → second digit is `9` → returns 200 with dynamic Mongo-backed response

### Error Response Matrix

| Second Digit | Status | Scenario | Error Code |
| ------------ | ------ | -------- | ---------- |
| 0 | 200 | Success | - |
| 2 | 422 | Unprocessable Entity | 012 - ID_VALUE missing or invalid |
| 3 | 422 | Unprocessable Entity | 014 - Email Address missing or invalid |
| 4 | 422 | Unprocessable Entity | 015 - Previous Amendment is in progress |
| 5 | 403 | Forbidden | - |
| 6 | 415 | Unsupported Media Type | - |
| 7 | 400 | Bad Request | - |
| 8 | 404 | Not Found | - |
| 9 | 200 | Success (dynamic, Mongo-backed) | - |
| other | 500 | Internal Server Error | - |

### Additional 422 Scenarios

These scenarios don't depend on the `idValue` digit:

| Condition | Error Code | Error Message |
|-----------|------------|---------------|
| `regime` ≠ `VPD` | 001 | REGIME missing or invalid |
| `idType` ≠ `ZVPD` | 011 | ID_TYPE missing or invalid |
| `paperlessPreference: true` with no `emailVerification` | 013 | Email Verification missing |

### Example Error Response (422)

```json
{
  "errors": {
    "processingDate": "2026-09-30T10:00:00Z",
    "code": "012",
    "text": "ID_VALUE missing or invalid"
  }
}
```

### Example VPD IDs for Testing

| VPD ID | Expected Behavior |
|--------|-------------------|
| `GBWK0000200WK` | Success (200) |
| `GBWK0200200WK` | 422 - ID_VALUE missing or invalid |
| `GBWK0300200WK` | 422 - Email Address missing or invalid |
| `GBWK0400200WK` | 422 - Previous Amendment is in progress |
| `GBWK0500200WK` | 403 Forbidden |
| `GBWK0600200WK` | 415 Unsupported Media Type |
| `GBWK0700200WK` | 400 Bad Request |
| `GBWK0800200WK` | 404 Not Found |
| `GBWK0900200WK` | Success with dynamic data (200) |

---

## Test Support

### State Management

The stub maintains contact preferences in MongoDB when using digit `9` in the second position of the VPD ID. This allows testing of:
- Preference updates over time
- Email address changes
- Paperless preference toggling

### Testing Contact Preference Updates

1. **Get current preferences:**
   ```bash
   curl http://localhost:8142/vaping-products-duty/subscription/contact-preference/GBWK0900200WK
   ```

2. **Update to paperless with verified email:**
   ```bash
   curl -X PUT http://localhost:8142/etmp/RESTAdapter/email-contact-preference/VPD/ZVPD/GBWK0900200WK \
     -H "Content-Type: application/json" \
     -d '{
       "paperlessPreference": true,
       "emailVerification": {
         "emailAddress": "newemail@example.com",
         "emailVerified": true
       }
     }'
   ```

3. **Verify the update:**
   ```bash
   curl http://localhost:8142/vaping-products-duty/subscription/contact-preference/GBWK0900200WK
   ```

### Testing Error Scenarios

**Test 422 - Missing Email Verification:**
```bash
curl -X PUT http://localhost:8142/etmp/RESTAdapter/email-contact-preference/VPD/ZVPD/GBWK0000200WK \
  -H "Content-Type: application/json" \
  -d '{
    "paperlessPreference": true
  }'
```

**Test 422 - Invalid Regime:**
```bash
curl -X PUT http://localhost:8142/etmp/RESTAdapter/email-contact-preference/INVALID/ZVPD/GBWK0000200WK \
  -H "Content-Type: application/json" \
  -d '{
    "paperlessPreference": false
  }'
```

**Test 422 - Invalid ID Type:**
```bash
curl -X PUT http://localhost:8142/etmp/RESTAdapter/email-contact-preference/VPD/INVALID/GBWK0000200WK \
  -H "Content-Type: application/json" \
  -d '{
    "paperlessPreference": false
  }'
```

---

## Related Documentation

- [Subscription API](SUBSCRIPTION-API.md) - View subscription data including contact preferences
- [Email Verification API](EMAIL-VERIFICATION-API.md) - Email verification workflow
- [VPD ID Examples](VPD-ID-EXAMPLES.md) - VPD ID patterns for testing
- [Main README](../README.md) - Getting started and overview