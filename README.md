# Vaping Duty Stubs

This is the stub microservice for the Vaping Products Duty service, providing test endpoints that simulate external APIs for local development and testing.

## Purpose

This stub service simulates:
- **ETMP (Enterprise Tax Management Platform)** endpoints for subscription, obligations, and returns
- **Email verification** services
- **Contact preference** management
- **Financial data** for payments and balances

## Related Services

| Service | Repository | Purpose |
|---------|-----------|---------|
| Frontend | [vaping-duty-frontend](https://github.com/hmrc/vaping-duty-frontend) | User-facing web application |
| Backend | [vaping-duty](https://github.com/hmrc/vaping-duty) | Returns and obligations API |
| Account | [vaping-duty-account](https://github.com/hmrc/vaping-duty-account) | Subscription and account management |
| Finance | [vaping-duty-finance](https://github.com/hmrc/vaping-duty-finance) | Payment processing |

## Technology Stack

- **Language**: Scala 3.3.6
- **Framework**: Play Framework
- **Database**: MongoDB (for stateful test scenarios)
- **Port**: 8142

## Requirements

- JRE 21+
- SBT
- MongoDB
- Service Manager 2 (for integration with other services)

## Running the Stub

### Running with Service Manager

Start the stub as part of the full Vaping Duty service stack:
```bash
sm2 --start VAPING_DUTY_ALL
```

Or start just the stub:
```bash
sm2 --start VAPING_DUTY_STUBS
```

The stub will be available at: http://localhost:8142

### Running Locally (Standalone)

1. Clone the repository:
   ```bash
   git clone git@github.com:hmrc/vaping-duty-stubs.git
   cd vaping-duty-stubs
   ```

2. Start MongoDB (if not already running):
   ```bash
   brew services start mongodb-community  # macOS
   ```

3. Run the stub:
   ```bash
   sbt run
   ```

The stub will be available at: http://localhost:8142

### Running with Test-Only Routes

To enable test support endpoints for managing stub 
```bash
sbt run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes
```

## Testing

### Run All Tests (Unit + Integration)

```bash
sbt runAllChecks
```

This executes:
- Unit tests
- Integration tests
- Coverage analysis

### Run Unit Tests Only

```bash
sbt runLocalChecks
```

### Test Structure

- **Unit tests**: `test/` directory
- **Integration tests**: `it/test/` directory
- **Test data generators**: `test/uk/gov/hmrc/vapingdutystubs/testUtils/`

## Architecture

### Stub Behavior Patterns

The stub uses **pattern-based routing** where VPD IDs and other identifiers contain digits that determine the response:

- **Email Flag Digit** (first digit in VPD ID) → Controls email preferences and verification status
- **Status Pattern Digit** (third-from-last digit) → Controls subscription approval and insolvency status
- **Error Trigger Digit** (last digit before "WK") → Triggers specific error responses for testing

This allows developers to test different scenarios by simply changing the VPD ID.

### Data Storage

- **MongoDB repositories** store stateful data (obligations, financial data, returns, subscriptions)
- **Startup seeding** (`StartupSeeder`) pre-populates common test scenarios
- **Test-only endpoints** allow dynamic data manipulation during testing

### Key Components

- **Controllers**: Handle HTTP requests and delegate to services
- **Repositories**: MongoDB-backed storage for test data
- **Data generators**: Create realistic test data
- **Models**: JSON serialization for API requests/responses

## API Documentation

### Subscription API

#### **GET** `/etmp/RESTAdapter/vpd/subscription/:vpdId`

Returns subscription information for a VPD ID.

**Example response** (for vpdId=`"XIWK1104205WK"`):

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

### VPD ID Pattern-Based Responses

The stub uses specific digits in the VPD ID to determine response behavior:

## Returning Specific Stubbed Information

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

#### Subscription Status Patterns (Third-from-last digit)

The third digit from the end determines the subscription approval status and insolvency state:

| Pattern | Approval Status | Insolvency Status | Description | Example VPD ID |
|---------|----------------|-------------------|-------------|----------------|
| **2**xx | Approved (01) | Not Insolvent (N) | Standard approved subscription | `GBWK0000200WK` |
| **3**xx | Approved (01) | **Insolvent (Y)** | Approved but insolvent | `GBWK0000300WK` |
| **7**xx | Deregistered (04) | Not Insolvent (N) | Subscription deregistered | `GBWK0000700WK` |
| **8**xx | Revoked (05) | Not Insolvent (N) | Subscription revoked | `GBWK0000800WK` |

> **Note:**\
The approval status (approved/deregistered/revoked) is independent of the Insolvent flag. A manufacturer could be Insolvent from any of these statuses. We have chosen to only represent the Approved + Insolvent case here as this is sufficient to test the insolvent behaviour externally. Unit tests show that the other combinations work.

**Approval Status Codes:**
- `01` = Approved
- `04` = Deregistered  
- `05` = Revoked

**Insolvency Status Values:**
- `Y` = Insolvent
- `N` = Not Insolvent

**Combination Examples:**

These patterns can be combined with email flags (first digit) for different scenarios:

| VPD ID | Email Flag | Status Pattern | Result |
|--------|-----------|----------------|--------|
| `GBWK0000200WK` | 0 (Digital) | 2 (Approved) | Approved, not insolvent, digital preference |
| `GBWK1000300WK` | 1 (Postal) | 3 (Insolvent) | Approved, insolvent, postal preference |
| `GBWK5000300WK` | 5 (Overseas 1) | 3 (Insolvent) | Approved, insolvent, overseas address 1 |
| `GBWK0000700WK` | 0 (Digital) | 7 (Deregistered) | Deregistered, not insolvent |
| `GBWK2000800WK` | 2 (Unverified) | 8 (Revoked) | Revoked, unverified email |

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

## Obligations API

### **GET** `/etmp/obligations/:vpdId`

This endpoint returns obligation data for a VPD ID.

#### Test Error Responses (VPD ID Based)

The last digit of the VPD ID (before the "WK" suffix) determines the response:

| Last Digit | Status Code | Error Type | Description           | Example VPD ID |
|------------|-------------|------------|-----------------------|----------------|
| 5 | 422 | Unprocessable Entity | Simulated obligations unprocessable entity | `GBWK0000005WK` |

**Example Error Response:**
```json
{
  "code": "INVALID_REGIME",
  "reason": "Simulated obligations unprocessable entity"
}
```

For all other VPD IDs, the endpoint returns obligation data based on the configured scenario (see Test Support Endpoints section below).

---

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

## BTA Summary Payments Scenarios

Fixed VPD IDs are seeded at startup (see `StartupSeeder`) to cover the payment/balance scenarios needed
by the BTA summary tile (`vaping-duty-account`'s `GET /vpd/summary/:vpdId`). Digits `1`-`5` and `8` are
already reserved by the returns/obligations error-simulation and sample-obligations sets above, so these
use the remaining unused digits.

| VPD ID | Scenario | Balance |
|---|---|---|
| `GBWK0900906WK` | Single outstanding charge, not yet due | Positive, single charge reference |
| `GBWK0900907WK` | Single outstanding charge, overdue | Positive, single charge reference |
| `GBWK0900909WK` | Unallocated payment on account only | Negative (in credit) |
| `GBWK0900900WK` | No outstanding or unallocated amounts | Zero (nothing owed) |

These can also be re-seeded on demand, or set to any other existing financial-data scenario, via the
existing test-only endpoints:
```
POST /test-only/financial-data/:vpdId/scenario/:scenario
POST /test-only/financial-data/:vpdId/custom
```

## Troubleshooting

### Common Issues

**MongoDB Connection Errors**
- Ensure MongoDB is running: `brew services start mongodb-community` (macOS)
- Check MongoDB is accessible on default port 27017
- Verify connection string in `application.conf`

**Port Already in Use (8142)**
- Check if another instance is running: `lsof -i :8142`
- Stop any conflicting service: `sm2 --stop VAPING_DUTY_STUBS`
- Kill the process if needed: `kill -9 <PID>`

**Test-Only Routes Not Available**
- Ensure you're running with the test router:
  ```bash
  sbt run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes
  ```
- Test-only routes are at `/test-only/*` endpoints

**Stub Returning Unexpected Responses**
- Check the VPD ID pattern - specific digits trigger specific behaviors
- Review the pattern tables in this README for digit meanings
- Use test-only endpoints to set custom scenarios if needed
- Clear and reseed data using test-only endpoints

**Data Not Persisting Between Restarts**
- This is expected behavior - stub data is seeded at startup
- Use test-only endpoints to set up required scenarios after restart
- For persistent test scenarios, consider using Service Manager

**Integration with Other Services**
- Ensure all required services are running via Service Manager
- Check service URLs in `application.conf` match your setup
- Verify the stub is accessible at http://localhost:8142
- Check logs for connection errors to MongoDB

### Debugging Tips

**View Stub Logs**
```bash
# When running locally
# Logs appear in console output

# When running via Service Manager
sm2 --logs VAPING_DUTY_STUBS
```

**Test Endpoint Availability**
```bash
# Ping endpoint
curl http://localhost:8142/ping/ping

# Check subscription endpoint
curl http://localhost:8142/etmp/RESTAdapter/vpd/subscription/GBWK0000200WK
```

**Clear All Test Data**
```bash
# Clear obligations
curl -X POST http://localhost:8142/test-only/obligations/clear-all

# Clear financial data (if endpoint exists)
curl -X POST http://localhost:8142/test-only/financial-data/clear-all
```

## Quick Reference

### Common VPD ID Patterns

| VPD ID | Purpose | Behavior |
|--------|---------|----------|
| `GBWK0000200WK` | Standard approved | Digital preference, not insolvent |
| `GBWK1000300WK` | Insolvent scenario | Postal preference, insolvent |
| `GBWK0000001WK` | Error testing | Triggers 400 Bad Request on returns |
| `GBWK0000005WK` | Error testing | Triggers 422 Unprocessable Entity |
| `GBWK0900906WK` | Payment scenario | Outstanding charge not yet due |

### Key Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/ping/ping` | GET | Health check |
| `/etmp/RESTAdapter/vpd/subscription/:vpdId` | GET | Get subscription |
| `/etmp/obligations/:vpdId` | GET | Get obligations |
| `/vaping-products-duty/returns/:periodKey` | POST | Submit return |
| `/vaping-products-duty/returns/:vpdId/:periodKey` | GET | View return |
| `/test-only/obligations/:vpdId/scenario/:scenario` | POST | Set obligation scenario |
| `/test-only/obligations/clear-all` | POST | Clear all obligations |

## License

This code is open source software licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).

