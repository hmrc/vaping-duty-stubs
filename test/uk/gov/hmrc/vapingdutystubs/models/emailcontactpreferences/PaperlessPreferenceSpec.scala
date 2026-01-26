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

import java.time.Instant

class PaperlessPreferenceSpec extends SpecBase {
  "PaperlessPreferenceSubmission must" - {
    "serialise to json" in new SetUp {
      Json.toJson(paperlessPreference).toString mustBe paperlessPreferenceSubmissionJson
    }

    "deserialise from json" in new SetUp {
      Json
        .parse(paperlessPreferenceSubmissionJson)
        .as[PaperlessPreferenceSubmission] mustBe paperlessPreference
    }
  }

  "PaperlessPreferenceSubmittedSuccess must" - {
    "serialise to json" in new SetUp {
      Json
        .toJson(
          PaperlessPreferenceSubmittedSuccess(
            PaperlessPreferenceSubmittedResponse(Instant.now(clock), "910000000000")
          )
        )
        .toString mustBe paperlessPreferenceSubmittedSuccessJson
    }

    "de-serialise from json" in new SetUp {
      Json
        .parse(paperlessPreferenceSubmittedSuccessJson)
        .as[PaperlessPreferenceSubmittedSuccess] mustBe PaperlessPreferenceSubmittedSuccess(
        PaperlessPreferenceSubmittedResponse(Instant.now(clock), "910000000000")
      )
    }
  }

  class SetUp {
    val paperlessPreferenceSubmissionJson =
      """{"paperlessPreference":"1","emailAddress":"john.doe@example.com","emailVerification":"1","bouncedEmail":"0"}"""

    val paperlessPreferenceSubmittedSuccessJson =
      """{"success":{"processingDate":"2024-06-11T15:07:47.838Z","formBundleNumber":"910000000000"}}"""
  }
}
