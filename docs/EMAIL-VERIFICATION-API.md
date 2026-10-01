# Email Verification API

This API provides email verification services for Vaping Products Duty registrations.

## Table of Contents
- [About Credential ID](#about-credential-id)
- [Verify Email](#verify-email)
- [Get Verification Status](#get-verification-status)
- [CredId Indication Patterns](#credid-indication-patterns)
- [Test Support](#test-support)

---

## About Credential ID

The `credId` (credential ID) is a **16-digit numeric identifier** provided by the Government Gateway authentication service. It uniquely identifies an authenticated user's credential.

### Format
- **Type:** Numeric string
- **Length:** 16 digits
- **Example:** `7811131233899460`

### In Production
In a production environment, the credId is obtained from the HMRC auth service when a user authenticates via Government Gateway. Client applications should extract this value from the authenticated session/auth context.

### For Stub Testing
When testing with this stub, you can use any 16-digit numeric value. The stub uses the **last digit** of the credId to determine response behavior (see [CredId Indication Patterns](#credid-indication-patterns) below).

---

## Verify Email

**Endpoint:** `POST /email-verification/verify-email`

Initiates email verification for a user.

### Request Body

```json
{
  "credId": "7811131233899460",
  "email": "user@example.com",
  "templateId": "vaping_duty_email_verification",
  "templateParameters": {},
  "linkExpiryDuration": "P1D",
  "continueUrl": "https://www.tax.service.gov.uk/vaping-duty/continue"
}
```

### Request Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `credId` | String | Yes | 16-digit numeric Government Gateway credential ID |
| `email` | String | Yes | Email address to verify |
| `templateId` | String | Yes | Email template identifier |
| `templateParameters` | Object | No | Template parameters |
| `linkExpiryDuration` | String | No | Link expiry duration (ISO 8601 duration) |
| `continueUrl` | String | Yes | URL to redirect after verification |

### Success Response

**Status:** `201 Created`

```json
{
  "redirectUri": "/email-verification/journey/1234567890"
}
```

### Error Responses

| Status Code | Scenario | Description |
|-------------|----------|-------------|
| 400 | Bad Request | Invalid request payload |
| 409 | Conflict | Email already verified |
| 500 | Internal Server Error | Service unavailable |

---

## Get Verification Status

**Endpoint:** `GET /email-verification/verification-status/:credId`

Retrieves the email verification status for a credential ID.

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `credId` | String | 16-digit numeric Government Gateway credential ID |

### Success Response

**Status:** `200 OK`

```json
{
  "emails": [
    {
      "emailAddress": "user@example.com",
      "verified": true,
      "locked": false
    }
  ]
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `emailAddress` | String | The email address |
| `verified` | Boolean | Whether the email is verified |
| `locked` | Boolean | Whether the email is locked |

---

## CredId Indication Patterns

The stub uses the **last digit** in the credId to determine response behavior:

```scala
credIdDigit = credId.takeRight(1)
```

### CredId Digit Behavior

| Last Digit | Scenario |
| ---------- | -------- |
| 8 | BadRequest Response (400) |
| 9 | InternalServerError Response (500) |
| 1 | Alternate between NotFound and fixed scenarios |
| * (other) | Alternate between all unverified and fixed scenarios |

### Fixed Scenarios

The stub alternates between predefined email verification scenarios:

**Scenario 1: Verified Email**
```json
{
  "emails": [
    {
      "emailAddress": "verified@example.com",
      "verified": true,
      "locked": false
    }
  ]
}
```

**Scenario 2: Unverified Email**
```json
{
  "emails": [
    {
      "emailAddress": "unverified@example.com",
      "verified": false,
      "locked": false
    }
  ]
}
```

**Scenario 3: Locked Email**
```json
{
  "emails": [
    {
      "emailAddress": "locked@example.com",
      "verified": false,
      "locked": true
    }
  ]
}
```

### Example CredIds for Testing

| CredId | Expected Behavior |
|--------|-------------------|
| `7811131233899460` | Returns fixed scenarios (200 OK) |
| `7811131233899461` | Alternates between NotFound and fixed scenarios |
| `7811131233899462` | Alternates between all unverified and fixed scenarios |
| `7811131233899468` | Returns 400 Bad Request |
| `7811131233899469` | Returns 500 Internal Server Error |

---

## Test Support

### State Management

The stub maintains email verification state in MongoDB. This allows testing of:
- Email verification workflows
- State transitions (unverified → verified)
- Multiple email addresses per user
- Locked email scenarios

### Test-Only Endpoints

While there are no dedicated test-only endpoints for email verification, the state can be influenced by:
1. **CredId patterns** - Use specific credId digits to trigger different responses
2. **Subscription API** - The VPD ID email flag digit affects email verification status in subscription responses

### Testing Email Verification Flow

1. **Start with unverified email:**
   ```bash
   curl http://localhost:8142/email-verification/verification-status/7811131233899462
   ```

2. **Initiate verification:**
   ```bash
   curl -X POST http://localhost:8142/email-verification/verify-email \
     -H "Content-Type: application/json" \
     -d '{
       "credId": "7811131233899462",
       "email": "user@example.com",
       "templateId": "vaping_duty_email_verification",
       "continueUrl": "https://www.tax.service.gov.uk/vaping-duty/continue"
     }'
   ```

3. **Check status again:**
   ```bash
   curl http://localhost:8142/email-verification/verification-status/7811131233899462
   ```

---

## Related Documentation

- [Subscription API](SUBSCRIPTION-API.md) - Email verification status in subscription data
- [Email Contact Preferences API](EMAIL-CONTACT-PREFERENCES-API.md) - Manage contact preferences
- [VPD ID Examples](VPD-ID-EXAMPLES.md) - VPD ID patterns for testing
- [Main README](../README.md) - Getting started and overview