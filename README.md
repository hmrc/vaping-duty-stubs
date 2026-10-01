# Vaping Duty Stubs

This is the stub microservice for the Vaping Products Duty service, providing test endpoints that simulate external APIs for local development and testing.

## Purpose

This stub service simulates:
- **ETMP** stubbed endpoints for subscription, obligations, financial data and returns
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
- Colima (for MongoDB container)
- Service Manager 2 (for integration with other services)

## Quick Start

1. Ensure Colima is running: `colima start`
2. Start the stub: `sm2 --start VAPING_DUTY_STUBS`
3. Test it works: `curl http://localhost:8142/etmp/RESTAdapter/vpd/subscription/XIWK0000200WK`
4. Use VPD ID patterns to trigger different test scenarios (see [VPD ID Examples](docs/VPD-ID-EXAMPLES.md))

## VPD ID Quick Examples

> 💡 **Tip:** Use **XI prefix** (e.g., `XIWK...`) for full end-to-end testing via BTA. GB prefix uses BTA stub with limited functionality.

### Most Common Testing Scenarios

| VPD ID | What You Get | Use For |
|--------|--------------|---------|
| `XIWK0000200WK` | ✅ Digital preference, verified email, approved | Happy path testing |
| `XIWK1000300WK` | ⚠️ Postal preference, insolvent | Insolvency scenarios |
| `XIWK2000200WK` | ❌ Unverified email | Email verification flows |
| `XIWK5000200WK` | 🌍 Overseas address (Canada) | International addresses |
| `XIWK0000001WK` | ⚠️ Triggers 400 errors | Error handling testing |

### Pre-Seeded Payment Scenarios

These VPD IDs have financial data already set up (no test-only endpoints needed):

| VPD ID | Balance State | Use For |
|--------|---------------|---------|
| `GBWK0900906WK` | Outstanding not yet due | Payment journey testing |
| `GBWK0900907WK` | Overdue payment | Overdue scenarios |
| `GBWK0900909WK` | In credit | Credit balance scenarios |
| `GBWK0900900WK` | Zero balance | Nothing owed scenarios |

**Note:** You can override these or set up any VPD ID using [test-only endpoints](docs/TEST-ONLY-ENDPOINTS.md)

📖 **See [Complete VPD ID Examples Guide](docs/VPD-ID-EXAMPLES.md) for all scenarios including:**
- Complete email/contact preferences matrix
- All error testing combinations
- Subscription status patterns
- Address variations

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

2. Start Colima and MongoDB (if not already running):
   ```bash
   colima start
   docker ps | grep mongo  # Verify MongoDB container is running
   ```

3. Run the stub:
   ```bash
   sbt run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes
   ```

The stub will be available at: http://localhost:8142

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

### VPD ID Prefix: XI vs GB

The VPD ID prefix determines how the service integrates with BTA (Business Tax Account):

- **XI prefix** (e.g., `XIWK0000200WK`) - Integrates with this stub via BTA
  - ✅ Allows full end-to-end journey in staging and locally
  - ✅ All stub endpoints are accessible through BTA integration
  
- **GB prefix** (e.g., `GBWK0000200WK`) - Uses BTA stub instead
  - ⚠️ Does NOT allow end-to-end journey in staging or locally
  - ⚠️ BTA stub has limited functionality compared to this stub

**Recommendation:** Use XI-prefixed VPD IDs for testing complete user journeys in non-production environments.

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

This stub service provides the following APIs for testing Vaping Products Duty services:

### Core APIs

- **[Subscription API](docs/SUBSCRIPTION-API.md)** - Retrieve subscription details and manage subscription data
  - Get subscription summary
  - VPD ID pattern-based responses
  - Email and address indicators
  - Subscription status patterns

- **[Obligations API](docs/OBLIGATIONS-API.md)** - Query obligation periods and statuses
  - Get obligations for a VPD ID
  - Test error responses
  - Test-only endpoints for managing obligations

- **[Financial Data API](docs/FINANCIAL-DATA-API.md)** - Access financial transaction data and payment information
  - Get financial data
  - Pre-seeded payment scenarios
  - Test-only endpoints for managing financial data

- **[Returns API](docs/RETURNS-API.md)** - Submit and view vaping duty returns
  - Submit return
  - View return
  - VPD ID test error triggering

### Supporting APIs

- **[Email Verification API](docs/EMAIL-VERIFICATION-API.md)** - Email verification workflow and status checking
  - Verify email
  - Get verification status
  - CredId indication patterns

- **[Email Contact Preferences API](docs/EMAIL-CONTACT-PREFERENCES-API.md)** - Manage contact preferences and paperless settings
  - Get contact preferences
  - Update contact preferences
  - Error scenarios

### Additional Documentation

- **[VPD ID Examples](docs/VPD-ID-EXAMPLES.md)** - Valid VPD ID patterns for testing different scenarios
- **[Test-Only Endpoints](docs/TEST-ONLY-ENDPOINTS.md)** - Additional endpoints for dynamic test data management

---

## Quick API Reference

### Most Common Endpoints

| Endpoint | Method | Purpose | Documentation |
|----------|--------|---------|---------------|
| `/etmp/RESTAdapter/vpd/subscription/:idValue` | GET | Get subscription | [Subscription API](docs/SUBSCRIPTION-API.md) |
| `/etmp/RESTAdapter/cross-regime/taxpayer-obligations` | GET | Get obligations | [Obligations API](docs/OBLIGATIONS-API.md) |
| `/etmp/RESTAdapter/cross-regime/taxpayer/financial-data/query` | POST | Get financial data | [Financial Data API](docs/FINANCIAL-DATA-API.md) |
| `/etmp/RESTAdapter/vpd/returns` | POST | Submit return | [Returns API](docs/RETURNS-API.md) |
| `/etmp/RESTAdapter/vpd/returns/:vpdReference/:periodKey` | GET | View return | [Returns API](docs/RETURNS-API.md) |
| `/email-verification/verification-status/:credId` | GET | Get email verification status | [Email Verification API](docs/EMAIL-VERIFICATION-API.md) |
| `/etmp/RESTAdapter/email-contact-preference/:regime/:idType/:idValue` | PUT | Update contact preferences | [Email Contact Preferences API](docs/EMAIL-CONTACT-PREFERENCES-API.md) |

### Test-Only Endpoints (require test router)

| Endpoint | Method | Purpose | Documentation |
|----------|--------|---------|---------------|
| `/test-only/obligations/:vpdId/scenario/:scenario` | POST | Set obligation scenario | [Test-Only Endpoints](docs/TEST-ONLY-ENDPOINTS.md) |
| `/test-only/obligations/clear-all` | POST | Clear all obligations | [Test-Only Endpoints](docs/TEST-ONLY-ENDPOINTS.md) |
| `/test-only/financial-data/:vpdId/scenario/:scenario` | POST | Set financial data scenario | [Test-Only Endpoints](docs/TEST-ONLY-ENDPOINTS.md) |
| `/test-only/financial-data/clear-all` | POST | Clear all financial data | [Test-Only Endpoints](docs/TEST-ONLY-ENDPOINTS.md) |

---

## Example: Testing a Complete Journey

```bash
# 1. Check subscription status
curl http://localhost:8142/etmp/RESTAdapter/vpd/subscription/XIWK0000200WK

# 2. Get obligations
curl http://localhost:8142/etmp/obligations/XIWK0000200WK

# 3. Get financial data
curl "http://localhost:8142/enterprise/financial-data/VPD/ZVPD/XIWK0000200WK?onlyOpenItems=true"

# 4. Submit a return (requires headers)
curl -X POST http://localhost:8142/vaping-products-duty/returns/27AL \
  -H "Content-Type: application/json" \
  -H "x-zvpd: XIWK0000200WK" \
  -d '{"periodKey": "27AL", ...}'

# 5. View the submitted return
curl http://localhost:8142/vaping-products-duty/returns/XIWK0000200WK/27AL
```

---

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
- Ensure Colima is running: `colima status`
- Start Colima if needed: `colima start`
- Verify MongoDB container is running: `docker ps | grep mongo`
- Check MongoDB is accessible on port 27017
- Verify connection string in `application.conf`

**Colima Issues After Machine Restart**
- After restarting your machine, Colima may need to be force stopped and restarted:
  ```bash
  colima stop --force
  colima start
  ```
- Verify Colima is running: `colima status`
- Check MongoDB container is up: `docker ps | grep mongo`

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

> **Note:** Use **XI** prefix (e.g., `XIWK...`) for full end-to-end testing via BTA. GB prefix uses BTA stub with limited functionality.

| VPD ID | Purpose | Behavior |
|--------|---------|----------|
| `XIWK0000200WK` | Standard approved (XI) | Digital preference, not insolvent, full BTA integration |
| `GBWK0000200WK` | Standard approved (GB) | Digital preference, not insolvent, BTA stub only |
| `XIWK1000300WK` | Insolvent scenario (XI) | Postal preference, insolvent, full BTA integration |
| `GBWK0000001WK` | Error testing | Triggers 400 Bad Request on returns |
| `XIWK0000005WK` | Error testing | Triggers 422 Unprocessable Entity |
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

