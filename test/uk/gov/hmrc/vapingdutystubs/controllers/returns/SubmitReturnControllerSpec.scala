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

package uk.gov.hmrc.vapingdutystubs.controllers.returns

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.vapingdutystubs.base.SpecBase
import uk.gov.hmrc.vapingdutystubs.config.Constants.Headers.xZVPD
import uk.gov.hmrc.vapingdutystubs.models.returns.*
import uk.gov.hmrc.vapingdutystubs.models.returns.submit.ReturnCreateRequest
import uk.gov.hmrc.vapingdutystubs.models.{DownstreamError, EtmpDownstreamError}
import uk.gov.hmrc.vapingdutystubs.repositories.{ObligationsRepository, ReturnSubmissionRepository}
import uk.gov.hmrc.vapingdutystubs.utils.RandomUUIDGenerator

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.{ExecutionContext, Future}

class SubmitReturnControllerSpec extends SpecBase with MockitoSugar {

  override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  val mockReturnSubmissionRepository: ReturnSubmissionRepository = mock[ReturnSubmissionRepository]
  val mockObligationsRepository: ObligationsRepository = mock[ObligationsRepository]
  val mockUuidGenerator: RandomUUIDGenerator = mock[RandomUUIDGenerator]
  val fixedClock: Clock = Clock.fixed(Instant.parse("2026-05-28T10:30:00Z"), ZoneId.of("UTC"))
  override val cc: ControllerComponents = stubControllerComponents()

  val controller = new SubmitReturnController(
    cc,
    mockReturnSubmissionRepository,
    mockObligationsRepository,
    mockUuidGenerator,
    fixedClock
  )

  override val vpdId = "GBWK1234567WK"
  val periodKey = "24AL"
  override val submissionId = "123456789012"
  override val chargeReference = "XMVPD123456789012"

  val validReturnRequest: ReturnCreateRequest = ReturnCreateRequest(
    periodKey = periodKey,
    vapingProductsProduced = VapingProductsProduced(
      vapingProdManufactured = "1",
      returns = Seq(VapingReturn(
        taxType = "641",
        dutyRate = BigDecimal("10.50"),
        amountProducedLiquid = BigDecimal("1500.25"),
        dutyDue = BigDecimal("15752.63")
      ))
    ),
    overDeclaration = None,
    underDeclaration = None,
    spoiltProduct = None,
      totalDutyDue = TotalDutyDue(
        totalDutyDueVapingProducts = BigDecimal("15752.63"),
        totalDutyOverDeclaration = BigDecimal("1050.00"),
        totalDutyUnderDeclaration = BigDecimal("2100.00"),
        totalDutySpoiltProduct = BigDecimal("525.00"),
        totalDue = BigDecimal("16277.63")
      ),
    otherOptions = None,
    declaration = DeclarationDetails(
      fullName = "John Smith",
      capacityInWhichSigned = "Director",
      signeesEmailAddress = "john.smith@example.com"
    )
  )

  "submitReturn" - {
    "must return CREATED with correct response when valid return is submitted" in {
      when(mockUuidGenerator.uuid).thenReturn(submissionId)
      when(mockUuidGenerator.uuidHyphenTrimmed).thenReturn("123456789012")
      
      val submission = ReturnSubmission(
        vpdId = vpdId,
        periodKey = periodKey,
        chargeReference = Some(chargeReference),
        submittedReturn = validReturnRequest,
        submittedAt = Instant.now(fixedClock),
        submissionId = submissionId
      )
      
      when(mockReturnSubmissionRepository.set(any())).thenReturn(Future.successful(submission))
      when(mockObligationsRepository.markAsFulfilled(any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.submitReturn()(
        fakeRequestWithJsonBody(Json.toJson(validReturnRequest))
          .withHeaders(
            xZVPD -> vpdId,
            "Content-Type" -> "application/json"
          )
      )

      status(result) shouldBe CREATED
      val responseJson = contentAsJson(result)
      (responseJson \ "success" \ "vpdReferenceNumber").as[String] shouldBe vpdId
      (responseJson \ "success" \ "amount").as[BigDecimal] shouldBe BigDecimal("16277.63")
    }

    "must return CREATED with no charge reference when totalDue is zero" in {
      when(mockUuidGenerator.uuid).thenReturn(submissionId)
      when(mockUuidGenerator.uuidHyphenTrimmed).thenReturn("123456789012")

      val nilReturnRequest = validReturnRequest.copy(
        vapingProductsProduced = VapingProductsProduced(
          vapingProdManufactured = "0",
          returns = Seq.empty
        ),
        totalDutyDue = TotalDutyDue(
          totalDutyDueVapingProducts = BigDecimal("0.00"),
          totalDutyOverDeclaration = BigDecimal("0.00"),
          totalDutyUnderDeclaration = BigDecimal("0.00"),
          totalDutySpoiltProduct = BigDecimal("0.00"),
          totalDue = BigDecimal("0.00")
        )
      )

      val submission = ReturnSubmission(
        vpdId = vpdId,
        periodKey = periodKey,
        chargeReference = None,
        submittedReturn = nilReturnRequest,
        submittedAt = Instant.now(fixedClock),
        submissionId = submissionId
      )

      when(mockReturnSubmissionRepository.set(any())).thenReturn(Future.successful(submission))
      when(mockObligationsRepository.markAsFulfilled(any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.submitReturn()(
        fakeRequestWithJsonBody(Json.toJson(nilReturnRequest))
          .withHeaders(
            xZVPD -> vpdId,
            "Content-Type" -> "application/json"
          )
      )

      status(result) shouldBe CREATED
      val responseJson = contentAsJson(result)
      (responseJson \ "success" \ "vpdReferenceNumber").as[String] shouldBe vpdId
      (responseJson \ "success" \ "chargeReference").asOpt[String] shouldBe None
      (responseJson \ "success" \ "amount").as[BigDecimal] shouldBe BigDecimal("0.00")
    }

    "must return BAD_REQUEST when vapingProdManufactured is 1 but returns array is empty" in {
      val invalidRequest = validReturnRequest.copy(
        vapingProductsProduced = VapingProductsProduced(
          vapingProdManufactured = "1",
          returns = Seq.empty
        )
      )

      val result = controller.submitReturn()(
        fakeRequestWithJsonBody(Json.toJson(invalidRequest))
          .withHeaders(
            xZVPD -> vpdId,
            "Content-Type" -> "application/json"
          )
      )

      status(result) shouldBe BAD_REQUEST
      val responseJson = contentAsJson(result)
      (responseJson \ "error").as[String] should include("returns array is empty")
    }

    "must return BAD_REQUEST when vapingProdManufactured is 0 but returns array is not empty" in {
      val invalidRequest = validReturnRequest.copy(
        vapingProductsProduced = VapingProductsProduced(
          vapingProdManufactured = "0",
          returns = Seq(VapingReturn("641", BigDecimal("10.50"), BigDecimal("100"), BigDecimal("1050")))
        )
      )

      val result = controller.submitReturn()(
        fakeRequestWithJsonBody(Json.toJson(invalidRequest))
          .withHeaders(
            xZVPD -> vpdId,
            "Content-Type" -> "application/json"
          )
      )

      status(result) shouldBe BAD_REQUEST
      val responseJson = contentAsJson(result)
      (responseJson \ "error").as[String] should include("returns array is not empty")
    }

    "when VPD ID triggers test error responses" - {
      "must return 400 Bad Request when VPD ID ends with 1" in {
        val testVpdId = "GBWK1234561WK"
        val result = controller.submitReturn()(
          fakeRequestWithJsonBody(Json.toJson(validReturnRequest))
            .withHeaders(
              xZVPD -> testVpdId,
              "Content-Type" -> "application/json"
            )
        )

        status(result) shouldBe BAD_REQUEST
        val errorResponse = contentAsJson(result).as[DownstreamError]
        errorResponse.error.code shouldBe "400"
        errorResponse.error.message should include("Invalid request payload")
        errorResponse.error.logID shouldBe "ABCDEF1234567890ABCDEF1234567890"
      }

      "must return 403 Forbidden when VPD ID ends with 2" in {
        val testVpdId = "GBWK1234562WK"
        val result = controller.submitReturn()(
          fakeRequestWithJsonBody(Json.toJson(validReturnRequest))
            .withHeaders(
              xZVPD -> testVpdId,
              "Content-Type" -> "application/json"
            )
        )

        status(result) shouldBe FORBIDDEN
        val errorResponse = contentAsJson(result).as[DownstreamError]
        errorResponse.error.code shouldBe "403"
        errorResponse.error.message shouldBe "Forbidden"
        errorResponse.error.logID shouldBe "ABCDEF1234567890ABCDEF1234567890"
      }

      "must return 409 Conflict when VPD ID ends with 4" in {
        val testVpdId = "GBWK1234564WK"
        val result = controller.submitReturn()(
          fakeRequestWithJsonBody(Json.toJson(validReturnRequest))
            .withHeaders(
              xZVPD -> testVpdId,
              "Content-Type" -> "application/json"
            )
        )

        status(result) shouldBe CONFLICT
        val errorResponse = contentAsJson(result).as[EtmpDownstreamError]
        errorResponse.error.code shouldBe "004"
        errorResponse.error.text shouldBe "Duplicate submission"
        errorResponse.error.processingDate should not be empty
      }

      "must return 422 Unprocessable Entity when VPD ID ends with 5" in {
        val testVpdId = "GBWK1234565WK"
        val result = controller.submitReturn()(
          fakeRequestWithJsonBody(Json.toJson(validReturnRequest))
            .withHeaders(
              xZVPD -> testVpdId,
              "Content-Type" -> "application/json"
            )
        )

        status(result) shouldBe UNPROCESSABLE_ENTITY
        val errorResponse = contentAsJson(result).as[EtmpDownstreamError]
        errorResponse.error.code shouldBe "001"
        errorResponse.error.text shouldBe "Regime missing or invalid"
        errorResponse.error.processingDate should not be empty
      }

      "must return 500 Internal Server Error when VPD ID ends with 8" in {
        val testVpdId = "GBWK1234568WK"
        val result = controller.submitReturn()(
          fakeRequestWithJsonBody(Json.toJson(validReturnRequest))
            .withHeaders(
              xZVPD -> testVpdId,
              "Content-Type" -> "application/json"
            )
        )

        status(result) shouldBe INTERNAL_SERVER_ERROR
        val errorResponse = contentAsJson(result).as[DownstreamError]
        errorResponse.error.code shouldBe "500"
        errorResponse.error.message should include("SAP PI system is currently unavailable")
        errorResponse.error.logID shouldBe "ABCDEF1234567890ABCDEF1234567890"
      }

      "must process normally when VPD ID ends with 0" in {
        val testVpdId = "GBWK1234560WK"
        when(mockUuidGenerator.uuid).thenReturn(submissionId)
        when(mockUuidGenerator.uuidHyphenTrimmed).thenReturn("123456789012")

        val submission = ReturnSubmission(
          vpdId = testVpdId,
          periodKey = periodKey,
          chargeReference = Some(chargeReference),
          submittedReturn = validReturnRequest,
          submittedAt = Instant.now(fixedClock),
          submissionId = submissionId
        )

        when(mockReturnSubmissionRepository.set(any())).thenReturn(Future.successful(submission))
        when(mockObligationsRepository.markAsFulfilled(any(), any(), any())).thenReturn(Future.successful(None))

        val result = controller.submitReturn()(
          fakeRequestWithJsonBody(Json.toJson(validReturnRequest))
            .withHeaders(
              xZVPD -> testVpdId,
              "Content-Type" -> "application/json"
            )
        )

        status(result) shouldBe CREATED
        val responseJson = contentAsJson(result)
        (responseJson \ "success" \ "vpdReferenceNumber").as[String] shouldBe testVpdId
      }
    }
  }
}
