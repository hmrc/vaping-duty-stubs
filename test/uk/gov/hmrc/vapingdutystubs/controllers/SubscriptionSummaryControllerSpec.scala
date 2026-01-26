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
import uk.gov.hmrc.vapingdutystubs.data.subscription.SubscriptionSummaryData
import uk.gov.hmrc.vapingdutystubs.models.subscription.EmailPreferences

import java.time.Instant
import scala.concurrent.Future

class SubscriptionSummaryControllerSpec extends SpecBase {
  "getSubscriptionSummary must" - {
    "return 200 OK with the approval status Approved 01 when the appaId suffix is 200" in new SetUp {
      val result: Future[Result] =
        controller.getSubscriptionSummary(regime, idType, appaId("200"))(
          fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result)  mustBe OK
      headers(result) mustBe responseHeadersWithCorrelationId

      contentAsJson(result) mustBe Json.toJson(
        SubscriptionSummaryData
          .approvedSubscriptionSummary(
            now,
            false,
            standardEmailPreferences,
            ukCorrespondenceAddress
          )
      )
    }

    Seq(
      ("0", standardEmailPreferences),
      ("1", EmailPreferences(false, Some(standardEmailAddress), Some(true), Some(false))),
      ("2", EmailPreferences(false, Some(standardEmailAddress), Some(false), Some(false))),
      ("3", EmailPreferences(false, Some(standardEmailAddress), Some(true), Some(true))),
      ("9", EmailPreferences(false, None, None, None))
    ).foreach { case (emailFlag, expectedEmailPreferences) =>
      s"return 200 OK with the correct email preferences when the emailFlag is $emailFlag" in new SetUp {
        val result: Future[Result] =
          controller.getSubscriptionSummary(regime, idType, appaId("200", emailFlag = emailFlag))(
            fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
          )

        status(result)  mustBe OK
        headers(result) mustBe responseHeadersWithCorrelationId

        contentAsJson(result) mustBe Json.toJson(
          SubscriptionSummaryData
            .approvedSubscriptionSummary(
              now,
              false,
              expectedEmailPreferences,
              ukCorrespondenceAddress
            )
        )
      }
    }

    Seq(
      ("0", ukCorrespondenceAddress),
      ("5", overseasAddress1),
      ("6", overseasAddress2),
      ("7", unrecognisedCountryAddress),
      ("8", noCountryAddress)
    ).foreach { case (emailFlag, expectedCorrespondenceAddress) =>
      s"return 200 OK with the correct correspondence address when the emailFlag is $emailFlag" in new SetUp {
        val result: Future[Result] =
          controller.getSubscriptionSummary(regime, idType, appaId("200", emailFlag = emailFlag))(
            fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
          )

        status(result)  mustBe OK
        headers(result) mustBe responseHeadersWithCorrelationId

        contentAsJson(result) mustBe Json.toJson(
          SubscriptionSummaryData
            .approvedSubscriptionSummary(
              now,
              false,
              standardEmailPreferences,
              expectedCorrespondenceAddress
            )
        )
      }
    }

    "return 200 OK with the approval status DeRegistered 02 when the appaId suffix is 700" in new SetUp {
      val result: Future[Result] =
        controller.getSubscriptionSummary(regime, idType, appaId("700"))(
          fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result)  mustBe OK
      headers(result) mustBe responseHeadersWithCorrelationId

      contentAsJson(result) mustBe Json.toJson(
        SubscriptionSummaryData
          .withDrawnSubscriptionSummary(
            now,
            false,
            standardEmailPreferences,
            ukCorrespondenceAddress
          )
      )
    }

    "return 200 OK with the approval status Revoked 03 when the appaId suffix is 800" in new SetUp {
      val result: Future[Result] =
        controller.getSubscriptionSummary(regime, idType, appaId("800"))(
          fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result)  mustBe OK
      headers(result) mustBe responseHeadersWithCorrelationId

      contentAsJson(result) mustBe Json.toJson(
        SubscriptionSummaryData
          .rejectedSubscriptionSummary(
            now,
            false,
            standardEmailPreferences,
            ukCorrespondenceAddress
          )
      )
    }

    "return 400 BAD_REQUEST if the suffix is 600" in new SetUp {
      val result: Future[Result] =
        controller.getSubscriptionSummary(regime, idType, appaId("600"))(
          fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result)  mustBe BAD_REQUEST
      headers(result) mustBe responseHeadersWithCorrelationId
    }

    "return 404 NOT_FOUND if the suffix is 400" in new SetUp {
      val result: Future[Result] =
        controller.getSubscriptionSummary(regime, idType, appaId("400"))(
          fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result)  mustBe NOT_FOUND
      headers(result) mustBe Map()
    }

    "return 500 INTERNAL_SERVER_ERROR if the suffix is 900" in new SetUp {
      val result: Future[Result] =
        controller.getSubscriptionSummary(regime, idType, appaId("900"))(
          fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result)  mustBe INTERNAL_SERVER_ERROR
      headers(result) mustBe responseHeadersWithCorrelationId
    }

    "return 422 UNPROCESSABLE_ENTITY if the regime is not AD" in new SetUp {
      val result: Future[Result] =
        controller.getSubscriptionSummary(badRegime, idType, appaId("200"))(
          fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result)  mustBe UNPROCESSABLE_ENTITY
      headers(result) mustBe responseHeadersWithCorrelationId
    }

    "return 422 UNPROCESSABLE_ENTITY if the idType is not ZAD" in new SetUp {
      val result: Future[Result] =
        controller.getSubscriptionSummary(regime, badIdType, appaId("200"))(
          fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result)  mustBe UNPROCESSABLE_ENTITY
      headers(result) mustBe responseHeadersWithCorrelationId
    }

    "throw an exception if the appaId suffix can't be parsed" in new SetUp {
      a[RuntimeException] mustBe thrownBy(
        controller.getSubscriptionSummary(regime, idType, "")(
          fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
        )
      )
    }

    "throw an exception if the correlation ID header is not present" in new SetUp {
      an[IllegalArgumentException] mustBe thrownBy(
        controller.getSubscriptionSummary(regime, idType, appaId("200"))(fakeRequest)
      )
    }
  }

  class SetUp {
    val badRegime = "not VPD"
    val badIdType = "not ZVPD"

    val now = Instant.now(clock)

    val controller = new SubscriptionSummaryController(
      errorData,
      clock,
      cc
    )

    def appaId(suffix: String, offFlags: String = "00", emailFlag: String = "0"): String =
      s"XMADP${emailFlag}0000$offFlags$suffix"
  }
}
