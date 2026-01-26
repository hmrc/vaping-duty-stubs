/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.vapingdutystubs.controllers

import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.vapingdutystubs.base.SpecBase
import uk.gov.hmrc.vapingdutystubs.controllers.emailcontactpreferences.EmailContactPreferencesController
import uk.gov.hmrc.vapingdutystubs.models.emailcontactpreferences.{PaperlessPreferenceSubmittedResponse, PaperlessPreferenceSubmittedSuccess}

import java.time.Instant
import scala.concurrent.Future

class EmailContactPreferencesControllerSpec extends SpecBase {
  "submitPreferences must" - {
    "submit a preference for paperless communication with email details" in new SetUp {
      val vpdId                  = getVpdId('0')
      val result: Future[Result] =
        emailContactPreferencesController.submitPreferences(regime, idType, vpdId)(
          fakeRequestWithJsonBody(Json.toJson(paperlessPreference))
            .withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result)        mustBe OK
      headers(result)       mustBe responseHeadersWithCorrelationId
      contentAsJson(result) mustBe Json.toJson(
        PaperlessPreferenceSubmittedSuccess(
          PaperlessPreferenceSubmittedResponse(
            Instant.now(clock),
            "910000000000"
          )
        )
      )
    }

    "submit a preference for paper communication" in new SetUp {
      val vpdId                  = getVpdId('0')
      val result: Future[Result] =
        emailContactPreferencesController.submitPreferences(regime, idType, vpdId)(
          fakeRequestWithJsonBody(Json.toJson(paperPreference)).withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result)        mustBe OK
      headers(result)       mustBe responseHeadersWithCorrelationId
      contentAsJson(result) mustBe Json.toJson(
        PaperlessPreferenceSubmittedSuccess(
          PaperlessPreferenceSubmittedResponse(
            Instant.now(clock),
            "910000000000"
          )
        )
      )
    }

    "correctly return a 400" in new SetUp {
      val vpdId                 = getVpdId('7')
      val result: Future[Result] =
        emailContactPreferencesController.submitPreferences(regime, idType, vpdId)(
          fakeRequestWithJsonBody(Json.toJson(paperlessPreference))
            .withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result)  mustBe BAD_REQUEST
      headers(result) mustBe responseHeadersWithCorrelationId
    }

    "correctly return a 404" in new SetUp {
      val vpdId                  = getVpdId('8')
      val result: Future[Result] =
        emailContactPreferencesController.submitPreferences(regime, idType, vpdId)(
          fakeRequestWithJsonBody(Json.toJson(paperlessPreference))
            .withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result)  mustBe NOT_FOUND
      headers(result) mustBe Map()
    }

    "correctly return a 500" in new SetUp {
      val vpdId                  = getVpdId('9')
      val result: Future[Result] =
        emailContactPreferencesController.submitPreferences(regime, idType, vpdId)(
          fakeRequestWithJsonBody(Json.toJson(paperlessPreference))
            .withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result)  mustBe INTERNAL_SERVER_ERROR
      headers(result) mustBe responseHeadersWithCorrelationId
    }

    "return a 422 if the request payload specified paperless but email verified property is not present" in new SetUp {
      val vpdId                  = getVpdId('0')
      val result: Future[Result] =
        emailContactPreferencesController.submitPreferences(regime, idType, vpdId)(
          fakeRequestWithJsonBody(Json.toJson(paperlessPreferenceEmailNoVerification))
            .withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result)          mustBe UNPROCESSABLE_ENTITY
      headers(result)         mustBe responseHeadersWithCorrelationId
      contentAsString(result) mustBe Json.toJson(errorData.etmpUnprocessableEntity).toString()
    }

    "return a 422 if the regime is not VPD" in new SetUp {
      val vpdId                  = getVpdId('0')
      val result: Future[Result] =
        emailContactPreferencesController.submitPreferences(badRegime, idType, vpdId)(
          fakeRequestWithJsonBody(Json.toJson(paperlessPreference))
            .withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result)  mustBe UNPROCESSABLE_ENTITY
      headers(result) mustBe responseHeadersWithCorrelationId
    }

    "return a 422 if the idType is not ZVPD" in new SetUp {
      val vpdId                  = getVpdId('0')
      val result: Future[Result] =
        emailContactPreferencesController.submitPreferences(regime, badIdType, vpdId)(
          fakeRequestWithJsonBody(Json.toJson(paperlessPreference))
            .withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result)  mustBe UNPROCESSABLE_ENTITY
      headers(result) mustBe responseHeadersWithCorrelationId
    }

    "throw an error if the correlation ID header is not present" in new SetUp {
      val vpdId = getVpdId('0')
      an[IllegalArgumentException] mustBe thrownBy(
        emailContactPreferencesController.submitPreferences(regime, idType, vpdId)(
          fakeRequestWithJsonBody(Json.toJson(paperlessPreference))
        )
      )
    }
  }

  class SetUp {
    val badRegime = "not VPD"
    val badIdType = "not ZVPD"

    val now = Instant.now(clock)

    val emailContactPreferencesController = new EmailContactPreferencesController(errorData, clock, cc)

    def getVpdId(c: Char): String =
      s"${vpdId.take(6)}$c${vpdId.drop(7)}"

  }
}
