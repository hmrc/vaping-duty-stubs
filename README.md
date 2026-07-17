# Vaping Duty Stubs

## Returning specific stubbed information

### CredId Indication

#### **GET** `/email-verification/verification-status/:credId`

This is based off the last digit in the credId.

```scala
credIdDigit = credId.takeRight(1)
```

| Case | Scenario |
| ---- | -------- |
| 8    | BadRequest Response |
| 9    | InternalServerError Response |
| 1    | alternate between NotFound and fixedScenarios |
| *    | alternate between fixedScenariosAllUnverified and fixedScenarios |

### **GET** `/etmp/RESTAdapter/vpd/subscription/:vpdId`

This endpoint will return information about the current user's subscription.

Example response (for vpdId=`"XIWK1104205WK"`)

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

### Email Indicator Digit
for paperlessPreference, as well as email verification 
subscription status.

```scala
val emailFlagDigit = "[0-9]".r.findFirstIn(vpdId).get.toInt
```

Extracts first int from received vpdId.

Sample vpdId: XIWK2104405WK

Sample extracted emailFlagDigit: 2

#### Email Flag Digit Values

| Cases | Scenario |
| ----- | -------- |
| 0, 5, 6, 7, 8 | PaperlessPreference.Digital (1), verified=true, bounce=false |
| 1             | PaperlessPreference.Postal (0), verified=true, bounce=false  |
| 2             | PaperlessPreference.Postal (0), verified=false, bounce=false |
| 3             | PaperlessPreference.Postal (0), verified=false, bounce=true  |
| 4, 9          | PaperlessPreference.Postal (0), <no email available (false, false)> |

#### Address Indicator Digit

The address indicator digit is the same digit as the email indicator digit.

| Case | Descriptor | Resultant Address |
| ---- | ---------- | ----------------- |
| 5    | Overseas address 1 | Flat 123<br>1 Example Road<br>Toronto<br>P55555<br>CA |
| 6    | Overseas address 2 | 1 Example Road<br>Barcelona<br>P66666<br>ES |
| 7    | Country code not in mapping | Flat 123<br>1 Example Road<br>District A<br>Hong Kong<br>HK |
| 8    | No country code    | Building 1<br>Example City<br>P88888 |
| *    | UK address         | Flat 123<br>1 Example Road<br>London<br>AB1 2CD<br>GB |

#### 422 Unprocessable Entity scenarios

The suffix digits `5xx` (third-from-last digit `5`) trigger a 422. The last two digits select which real upstream error code is returned:

| Suffix | Code | Text |
| ------ | ---- | ---- |
| 501    | 001  | REGIME missing or invalid |
| 511    | 011  | ID_TYPE missing or invalid |
| 512    | 012  | ID_VALUE missing or invalid |
| any other 5xx (e.g. 500) | 003 | Request could not be processed |

Example error response:
```json
{
  "errors": {
    "processingDate": "2026-02-25T10:44:26.089402Z",
    "code": "001",
    "text": "REGIME missing or invalid"
  }
}
```

### **PUT** `/etmp/RESTAdapter/email-contact-preference/:regime/:idType/:idValue`

Outcome is selected by the second digit of `idValue` (`getStubIndex`), with the same `{"errors": {"processingDate", "code", "text"}}` shape used for every 422 below:

| Digit | Status | Scenario |
| ----- | ------ | -------- |
| 0     | 200    | Success |
| 2     | 422    | code 012 - ID_VALUE missing or invalid |
| 3     | 422    | code 014 - Email Address missing or invalid |
| 4     | 422    | code 015 - Previous Amendment is in progress |
| 5     | 403    | Forbidden |
| 6     | 415    | Unsupported Media Type |
| 7     | 400    | Bad Request |
| 8     | 404    | Not Found |
| 9     | 200    | Success (dynamic, Mongo-backed) |
| other | 500    | Internal Server Error |

Two further 422 scenarios don't depend on the `idValue` digit:
- `regime` other than `VPD` → code 001 - REGIME missing or invalid
- `idType` other than `ZVPD` → code 011 - ID_TYPE missing or invalid
- request body has `paperlessPreference: true` with no `emailVerification` → code 013 - Email Verification missing

### Running this stub
#### Run the stub using sm2
To run the stub using sm2, use the following command:

```sh
sm2 --start VAPING_DUTY_STUBS
```

#### Run the stub locally
To run the stub locally without using sm2, first:

- clone the repository
- cd into the cloned repo in your shell
- run the following command:

```sh
sbt run
```

## Test Support Endpoints

These endpoints are only available when running with test-only routes enabled:

```bash
sbt run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes
```

### Obligations Management

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

**Example:**
```bash
curl -X POST http://localhost:8142/test-only/obligations/XIWK0904905WK/scenario/mixed
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
    "obligations": [...]
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

#### Set Custom Obligations

Post custom obligations JSON for advanced testing scenarios:

```bash
POST /test-only/obligations/{vpdId}/custom
Content-Type: application/json
```

**Example:**
```bash
curl -X POST http://localhost:8142/test-only/obligations/GBWK0000001WK/custom \
  -H "Content-Type: application/json" \
  -d '{
    "vpdId": "GBWK0000001WK",
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
  "message": "Successfully set custom obligations for VPD ID GBWK0000001WK",
  "vpdId": "GBWK0000001WK",
  "obligationCount": 1
}
```

### Scenario Details

#### only-open Scenario
Creates 3 open returns with no completed returns:
- One return due in 10 days (period 27AL)
- One return overdue by 5 days (period 27AK)
- One return due in 30 days (period 28AA)

#### only-completed Scenario
Creates 3 completed returns with no open returns:
- Three fulfilled returns from previous periods (27AJ, 27AI, 27AH)

#### mixed Scenario
Creates a mix of open and completed returns (default):
- Two open returns (one due soon, one overdue)
- One completed return

#### none Scenario
Creates an empty obligations list for testing edge cases.

## Returns API

### VPD ID Test Error Triggering

Both returns endpoints support test error triggering based on the VPD ID format. The mechanism extracts the last digit before the "WK" suffix (3rd character from the end) to determine which error response to return.

**VPD ID Format:** `(GB|XI)WK[7 digits]WK`

**Example:** `GBWK0000001WK` → digit is `1` → triggers 400 Bad Request

```scala
// Extract the last digit before "WK" suffix (3rd character from end)
val lastDigit = vpdId.charAt(vpdId.length - 3).toString
```

### Submit Return

**Endpoint:** `POST /vaping-products-duty/returns/:periodKey`

**Headers Required:**
- `Authorization`
- `x-message-type`
- `x-regime-type`
- `x-correlation-id`
- `x-originating-system`
- `x-receipt-date`
- `x-transmitting-system`
- `x-zvpd` (VPD ID - used for test error triggering)

#### Test Error Responses (VPD ID Based)

| Last Digit | Status Code | Error Type | Description | Example VPD ID |
|------------|-------------|------------|-------------|----------------|
| 1 | 400 | Bad Request | Invalid request payload. Missing required field 'periodKey'. | `GBWK0000001WK` |
| 2 | 403 | Forbidden | Forbidden | `GBWK0000002WK` |
| 4 | 409 | Conflict | Duplicate submission | `GBWK0000004WK` |
| 5 | 422 | Unprocessable Entity | Regime missing or invalid | `GBWK0000005WK` |
| 8 | 500 | Internal Server Error | SAP PI system is currently unavailable | `GBWK0000008WK` |

**Example Error Response (Standard Format):**
```json
{
  "errorDetail": {
    "errorCode": "400",
    "errorMessage": "Invalid request payload. Missing required field 'periodKey'.",
    "source": "ABCDEF1234567890ABCDEF1234567890"
  }
}
```

**Example Error Response (ETMP Format):**
```json
{
  "failures": {
    "code": "004",
    "reason": "Duplicate submission",
    "timestamp": "2026-06-25T06:14:10.123Z"
  }
}
```

#### Normal Flow Errors

| Status Code | Scenario | Description |
|-------------|----------|-------------|
| 400 | Invalid JSON | Request body cannot be parsed as valid JSON |
| 400 | Validation Failure | Business validation failed (e.g., invalid period key format, negative amounts) |
| 500 | Repository Error | Database operation failed |

#### Success Response

**Status:** `201 Created`

**Example Response:**
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

### View Return

**Endpoint:** `GET /vaping-products-duty/returns/:vpdReference/:periodKey`

**Path Parameters:**
- `vpdReference` - VPD ID (used for test error triggering)
- `periodKey` - Period key (e.g., "27AL")

#### Test Error Responses (VPD ID Based)

| Last Digit | Status Code | Error Type | Description | Example VPD ID |
|------------|-------------|------------|-------------|----------------|
| 1 | 400 | Bad Request | Invalid request payload. Missing required field 'periodKey'. | `GBWK0000001WK` |
| 2 | 403 | Forbidden | Forbidden | `GBWK0000002WK` |
| 3 | 404 | Not Found | Not Found | `GBWK0000003WK` |
| 5 | 422 | Unprocessable Entity | ID Number missing or invalid | `GBWK0000005WK` |
| 8 | 500 | Internal Server Error | SAP PI system is currently unavailable | `GBWK0000008WK` |

**Example Error Response (Standard Format):**
```json
{
  "errorDetail": {
    "errorCode": "404",
    "errorMessage": "Not Found",
    "source": "ABCDEF1234567890ABCDEF1234567890"
  }
}
```

**Example Error Response (ETMP Format):**
```json
{
  "failures": {
    "code": "002",
    "reason": "ID Number missing or invalid",
    "timestamp": "2026-06-25T06:14:10.123Z"
  }
}
```

#### Normal Flow Behavior

When a return is not found in the repository for the given VPD ID and period key, the stub will:
1. Generate 33 return submissions for all fulfilled obligations for that VPD ID
2. Save them to the repository
3. Return the requested period's data if it exists in the generated set
4. Return a minimal response if the period is not in the fulfilled obligations

#### Success Response

**Status:** `200 OK`

**Example Response:**
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
  "vapingProducts": {
    "nicotineProducts": 1000,
    "nonNicotineProducts": 500
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

