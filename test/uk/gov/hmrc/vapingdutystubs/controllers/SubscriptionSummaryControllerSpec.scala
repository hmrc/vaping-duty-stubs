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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.vapingdutystubs.base.SpecBase
import uk.gov.hmrc.vapingdutystubs.data.subscription.SubscriptionSummaryData
import uk.gov.hmrc.vapingdutystubs.models.subscription.{EmailPreferences, SubscriptionSummaryResponse}
import uk.gov.hmrc.vapingdutystubs.repositories.SubscriptionSummaryRepository

import java.time.Instant
import scala.concurrent.Future

class SubscriptionSummaryControllerSpec extends SpecBase {
  val mockRepository: SubscriptionSummaryRepository = mock[SubscriptionSummaryRepository]

  "getSubscriptionSummary must" - {
    "return 200 OK with the approval status Approved 01 when the vpdId suffix is 200" in new SetUp {
      val result: Future[Result] =
        controller.getSubscriptionSummary(vpdId("200"))(
          fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result)  mustBe OK
      headers(result) mustBe responseHeadersWithCorrelationId

      contentAsJson(result) mustBe Json.toJson(
        SubscriptionSummaryData
          .approvedSubscriptionSummary(
            now,
            "N",
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
          controller.getSubscriptionSummary(vpdId("200", emailFlag = emailFlag))(
            fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
          )

        status(result)  mustBe OK
        headers(result) mustBe responseHeadersWithCorrelationId

        contentAsJson(result) mustBe Json.toJson(
          SubscriptionSummaryData
            .approvedSubscriptionSummary(
              now,
            "N",
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
          controller.getSubscriptionSummary(vpdId("200", emailFlag = emailFlag))(
            fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
          )

        status(result)  mustBe OK
        headers(result) mustBe responseHeadersWithCorrelationId

        contentAsJson(result) mustBe Json.toJson(
          SubscriptionSummaryData
            .approvedSubscriptionSummary(
              now,
              "N",
              standardEmailPreferences,
              expectedCorrespondenceAddress
            )
        )
      }
    }

    "return 200 OK with the approval status Approved 01 when the vpdId suffix is 900" in new SetUp {
      val summary: SubscriptionSummaryResponse = SubscriptionSummaryData.approvedSubscriptionSummary(now, "N", standardEmailPreferences, ukCorrespondenceAddress)

      when(mockRepository.get(any())).thenReturn(Future.successful(Some(summary)))

      val result: Future[Result] =
        controller.getSubscriptionSummary(vpdId("900"))(
          fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result) mustBe OK
      headers(result) mustBe responseHeadersWithCorrelationId

      contentAsJson(result) mustBe Json.toJson(
        SubscriptionSummaryData
          .approvedSubscriptionSummary(
            now,
            "N",
            standardEmailPreferences,
            ukCorrespondenceAddress
          )
      )
    }

    "return 200 OK with the approval status Approved 01 and insolvencyStatus Y when the vpdId suffix is 300" in new SetUp {
      val result: Future[Result] =
        controller.getSubscriptionSummary(vpdId("300"))(
          fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result)  mustBe OK
      headers(result) mustBe responseHeadersWithCorrelationId

      contentAsJson(result) mustBe Json.toJson(
        SubscriptionSummaryData
          .approvedSubscriptionSummary(
            now,
            "Y",
            standardEmailPreferences,
            ukCorrespondenceAddress
          )
      )
    }

    "return 200 OK with the approval status DeRegistered 02 when the vpdId suffix is 700" in new SetUp {
      val result: Future[Result] =
        controller.getSubscriptionSummary(vpdId("700"))(
          fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result)  mustBe OK
      headers(result) mustBe responseHeadersWithCorrelationId

      contentAsJson(result) mustBe Json.toJson(
        SubscriptionSummaryData
          .deregisteredSubscriptionSummary(
            now,
            "N",
            standardEmailPreferences,
            ukCorrespondenceAddress
          )
      )
    }

    "return 200 OK with the approval status Revoked 03 when the vpdId suffix is 800" in new SetUp {
      val result: Future[Result] =
        controller.getSubscriptionSummary(vpdId("800"))(
          fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result)  mustBe OK
      headers(result) mustBe responseHeadersWithCorrelationId

      contentAsJson(result) mustBe Json.toJson(
        SubscriptionSummaryData
          .revokedSubscriptionSummary(
            now,
            "N",
            standardEmailPreferences,
            ukCorrespondenceAddress
          )
      )
    }

    "return 400 BAD_REQUEST if the suffix is 600" in new SetUp {
      val result: Future[Result] =
        controller.getSubscriptionSummary(vpdId("600"))(
          fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result)  mustBe BAD_REQUEST
      headers(result) mustBe responseHeadersWithCorrelationId
    }

    "return 404 NOT_FOUND if the suffix is 400" in new SetUp {
      val result: Future[Result] =
        controller.getSubscriptionSummary(vpdId("400"))(
          fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result)  mustBe NOT_FOUND
      headers(result) mustBe Map()
    }

    "return 422 UNPROCESSABLE_ENTITY if the suffix is 500" in new SetUp {
      val result: Future[Result] =
        controller.getSubscriptionSummary(vpdId("500"))(
          fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result) mustBe UNPROCESSABLE_ENTITY
      headers(result) mustBe responseHeadersWithCorrelationId
      contentAsString(result) mustBe Json.toJson(errorData.requestCouldNotBeProcessed).toString()
    }

    "return 422 UNPROCESSABLE_ENTITY with code 001 if the suffix is 501" in new SetUp {
      val result: Future[Result] =
        controller.getSubscriptionSummary(vpdId("501"))(
          fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result) mustBe UNPROCESSABLE_ENTITY
      headers(result) mustBe responseHeadersWithCorrelationId
      contentAsString(result) mustBe Json.toJson(errorData.regimeInvalid).toString()
    }

    "return 422 UNPROCESSABLE_ENTITY with code 011 if the suffix is 511" in new SetUp {
      val result: Future[Result] =
        controller.getSubscriptionSummary(vpdId("511"))(
          fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result) mustBe UNPROCESSABLE_ENTITY
      headers(result) mustBe responseHeadersWithCorrelationId
      contentAsString(result) mustBe Json.toJson(errorData.idTypeInvalid).toString()
    }

    "return 422 UNPROCESSABLE_ENTITY with code 012 if the suffix is 512" in new SetUp {
      val result: Future[Result] =
        controller.getSubscriptionSummary(vpdId("512"))(
          fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result) mustBe UNPROCESSABLE_ENTITY
      headers(result) mustBe responseHeadersWithCorrelationId
      contentAsString(result) mustBe Json.toJson(errorData.idValueInvalid).toString()
    }


    "return 500 INTERNAL_SERVER_ERROR if the suffix is 100" in new SetUp {
      val result: Future[Result] =
        controller.getSubscriptionSummary(vpdId("100"))(
          fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result)  mustBe INTERNAL_SERVER_ERROR
      headers(result) mustBe responseHeadersWithCorrelationId
    }

    "throw an exception if the vpdId suffix can't be parsed" in new SetUp {
      a[RuntimeException] mustBe thrownBy(
        controller.getSubscriptionSummary("")(
          fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
        )
      )
    }

    "throw an exception if the correlation ID header is not present" in new SetUp {
      an[IllegalArgumentException] mustBe thrownBy(
        controller.getSubscriptionSummary(vpdId("200"))(fakeRequest)
      )
    }
  }

  class SetUp {
    val badRegime = "not VPD"
    val badIdType = "not ZVPD"

    val now: Instant = Instant.now(clock)

    val controller = new SubscriptionSummaryController(
      appConfig,
      errorData,
      clock,
      cc,
      mockRepository
    )

    def vpdId(suffix: String, offFlags: String = "000", emailFlag: String = "0"): String =
      s"GBWK${emailFlag}$offFlags${suffix}WK"
  }
}
