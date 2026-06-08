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

The service provides test support endpoints for managing obligations data. These endpoints are available when running the stub service.

### Available Endpoints

#### Set Predefined Scenario

Set a predefined obligations scenario for any VPD ID:

```bash
POST /test-support/obligations/{vpdId}/scenario/{scenarioName}
```

**Available Scenarios:**
- `only-open` - Multiple open returns with no completed returns (ideal for testing dynamic UI)
- `only-completed` - All returns completed
- `mixed` - Mix of open and completed returns (default behavior)
- `none` - Empty obligations list

**Example:**
```bash
curl -X POST http://localhost:8142/test-support/obligations/GBWK0000001WK/scenario/only-open
```

**Response:**
```json
{
  "message": "Successfully set scenario 'only-open' for VPD ID GBWK0000001WK",
  "vpdId": "GBWK0000001WK",
  "scenario": "only-open",
  "obligationCount": 3
}
```

#### Clear Obligations for Specific VPD ID

Remove all obligations for a specific VPD ID:

```bash
DELETE /test-support/obligations/{vpdId}
```

**Example:**
```bash
curl -X DELETE http://localhost:8142/test-support/obligations/GBWK0000001WK
```

**Response:**
```json
{
  "message": "Successfully cleared obligations for VPD ID GBWK0000001WK",
  "vpdId": "GBWK0000001WK"
}
```

#### Clear All Obligations

Remove all obligations data from the database:

```bash
DELETE /test-support/obligations
```

**Example:**
```bash
curl -X DELETE http://localhost:8142/test-support/obligations
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
POST /test-support/obligations/{vpdId}/custom
Content-Type: application/json
```

**Example:**
```bash
curl -X POST http://localhost:8142/test-support/obligations/GBWK0000001WK/custom \
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

