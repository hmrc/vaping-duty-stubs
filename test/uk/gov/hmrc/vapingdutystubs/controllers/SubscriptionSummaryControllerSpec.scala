/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vapingdutystubs.controllers

import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.vapingdutystubs.base.SpecBase
import uk.gov.hmrc.vapingdutystubs.data.subscription.SubscriptionSummaryData
import uk.gov.hmrc.vapingdutystubs.models.subscription.ApprovalType.*
import uk.gov.hmrc.vapingdutystubs.models.subscription.{ApprovalType, EmailPreferences}

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
            false,
            ApprovalType.values.toSet,
            standardEmailPreferences,
            ukCorrespondenceAddress
          )
      )
    }

    ApprovalType.values.foreach { approvalType =>
      val offFlags = approvalType match {
        case Beer                  => "01"
        case CiderOrPerry          => "02"
        case Wine                  => "04"
        case Spirits               => "08"
        case OtherFermentedProduct => "64"
      }
      s"return 200 OK with the ${approvalType.entryName} approval type switched off when the regime offFlags are $offFlags" in new SetUp {
        val result: Future[Result] =
          controller.getSubscriptionSummary(regime, idType, appaId("200", offFlags))(
            fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
          )

        status(result)  mustBe OK
        headers(result) mustBe responseHeadersWithCorrelationId

        contentAsJson(result) mustBe Json.toJson(
          SubscriptionSummaryData
            .approvedSubscriptionSummary(
              now,
              false,
              false,
              ApprovalType.values.toSet - approvalType,
              standardEmailPreferences,
              ukCorrespondenceAddress
            )
        )
      }
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
              false,
              ApprovalType.values.toSet,
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
              false,
              ApprovalType.values.toSet,
              standardEmailPreferences,
              expectedCorrespondenceAddress
            )
        )
      }
    }

    // TODO: Remove when ECP fields are included in real API
    "return 200 OK with approved status and no contact information when the appaId suffix is 000" in new SetUp {
      val result: Future[Result] =
        controller.getSubscriptionSummary(regime, idType, appaId("000"))(
          fakeRequest.withHeaders(submitCorrelationIdHeader(): _*)
        )

      status(result)  mustBe OK
      headers(result) mustBe responseHeadersWithCorrelationId

      contentAsJson(result) mustBe Json.toJson(
        SubscriptionSummaryData
          .subscriptionSummaryWithoutContactPreferences(
            now,
            false,
            false,
            ApprovalType.values.toSet
          )
      )
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
          .deregisteredSubscriptionSummary(
            now,
            false,
            false,
            ApprovalType.values.toSet,
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
          .revokedSubscriptionSummary(
            now,
            false,
            false,
            ApprovalType.values.toSet,
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
    val badRegime = "not AD"
    val badIdType = "not ZAD"

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
