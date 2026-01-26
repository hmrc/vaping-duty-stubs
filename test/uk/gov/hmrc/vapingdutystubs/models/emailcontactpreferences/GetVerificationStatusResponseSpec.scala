/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.vapingdutystubs.models.emailcontactpreferences

import play.api.libs.json.Json
import uk.gov.hmrc.vapingdutystubs.base.SpecBase

class GetVerificationStatusResponseSpec extends SpecBase {
  "GetVerificationStatusResponse must" - {
    "serialise to json" in new SetUp {
      Json.toJson(
        GetVerificationStatusResponse(emailVerificationStatuses.toList)
      ).toString mustBe getVerificationStatusResponseJson
    }

    "de-serialise from json" in new SetUp {
      Json
        .parse(getVerificationStatusResponseJson)
        .as[GetVerificationStatusResponse] mustBe GetVerificationStatusResponse(emailVerificationStatuses.toList)
    }
  }

  class SetUp {
    val getVerificationStatusResponseJson =
      """{"emails":[{"emailAddress":"john.doe@example.com","verified":true,"locked":false},{"emailAddress":"jane.doe@example.com","verified":false,"locked":true},{"emailAddress":"john.doe2@example.com","verified":false,"locked":false},{"emailAddress":"jane.doe2@example.com","verified":true,"locked":true}]}"""
  }
}
