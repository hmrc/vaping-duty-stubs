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

#### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
