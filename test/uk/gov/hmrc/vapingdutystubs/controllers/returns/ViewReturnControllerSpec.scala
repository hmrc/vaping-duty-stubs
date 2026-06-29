/*
 * Copyright 2026 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR, NOT_FOUND, OK, UNPROCESSABLE_ENTITY}
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.vapingdutystubs.base.SpecBase
import uk.gov.hmrc.vapingdutystubs.models.{DownstreamError, EtmpDownstreamError}
import uk.gov.hmrc.vapingdutystubs.models.returns.*
import uk.gov.hmrc.vapingdutystubs.models.returns.submit.ReturnCreateRequest
import uk.gov.hmrc.vapingdutystubs.repositories.ReturnSubmissionRepository

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class ViewReturnControllerSpec extends SpecBase with MockitoSugar {

  override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  val mockReturnSubmissionRepository: ReturnSubmissionRepository = mock[ReturnSubmissionRepository]
  override val cc: ControllerComponents = stubControllerComponents()

  val controller = new ViewReturnController(cc, mockReturnSubmissionRepository)

  // Default stub to prevent null pointer exceptions in error response tests
  when(mockReturnSubmissionRepository.get(any(), any()))
    .thenReturn(Future.successful(None))

  override val vpdId = "GBWK1234567WK"
  val periodKey = "24AL"
  override val submissionId = "123456789012"

  val sampleReturnRequest: ReturnCreateRequest = ReturnCreateRequest(
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
      totalDutyOverDeclaration = BigDecimal("0.00"),
      totalDutyUnderDeclaration = BigDecimal("0.00"),
      totalDutySpoiltProduct = BigDecimal("0.00"),
      totalDue = BigDecimal("15752.63")
    ),
    otherOptions = None,
    declaration = DeclarationDetails(
      fullName = "John Smith",
      capacityInWhichSigned = "Director",
      signeesEmailAddress = "john.smith@example.com"
    )
  )

  val sampleSubmission: ReturnSubmission = ReturnSubmission(
    vpdId = vpdId,
    periodKey = periodKey,
    chargeReference = "XMVPD123456789012",
    submittedReturn = sampleReturnRequest,
    submittedAt = Instant.parse("2026-05-28T10:30:00Z"),
    submissionId = submissionId
  )

  "viewReturn" - {
    "must return OK with return data when submission exists" in {
      when(mockReturnSubmissionRepository.get(eqTo(vpdId), eqTo(periodKey)))
        .thenReturn(Future.successful(Some(sampleSubmission)))

      val request = FakeRequest()
      val result = controller.viewReturn(vpdId, periodKey)(request)

      status(result) shouldBe OK
      val responseJson = contentAsJson(result)
      (responseJson \ "success" \ "idDetails" \ "vpdReferenceNumber").as[String] shouldBe vpdId
      (responseJson \ "success" \ "chargeDetails" \ "periodKey").as[String] shouldBe periodKey
    }

    "must generate and return data when submission does not exist" in {
      when(mockReturnSubmissionRepository.get(eqTo(vpdId), eqTo(periodKey)))
        .thenReturn(Future.successful(None))
      when(mockReturnSubmissionRepository.set(any()))
        .thenReturn(Future.successful(sampleSubmission))

      val request = FakeRequest()
      val result = controller.viewReturn(vpdId, periodKey)(request)

      status(result) shouldBe OK
    }

    "when VPD ID triggers test error responses" - {
      "must return 400 Bad Request when VPD ID ends with 1" in {
        val testVpdId = "GBWK1234561WK"
        val request = FakeRequest()
        val result = controller.viewReturn(testVpdId, periodKey)(request)

        status(result) shouldBe BAD_REQUEST
        val errorResponse = contentAsJson(result).as[DownstreamError]
        errorResponse.error.code shouldBe "400"
        errorResponse.error.message should include("Invalid request payload")
        errorResponse.error.logID shouldBe "ABCDEF1234567890ABCDEF1234567890"
      }

      "must return 403 Forbidden when VPD ID ends with 2" in {
        val testVpdId = "GBWK1234562WK"
        val request = FakeRequest()
        val result = controller.viewReturn(testVpdId, periodKey)(request)

        status(result) shouldBe FORBIDDEN
        val errorResponse = contentAsJson(result).as[DownstreamError]
        errorResponse.error.code shouldBe "403"
        errorResponse.error.message shouldBe "Forbidden"
        errorResponse.error.logID shouldBe "ABCDEF1234567890ABCDEF1234567890"
      }

      "must return 404 Not Found when VPD ID ends with 3" in {
        val testVpdId = "GBWK1234563WK"
        val request = FakeRequest()
        val result = controller.viewReturn(testVpdId, periodKey)(request)

        status(result) shouldBe NOT_FOUND
        val errorResponse = contentAsJson(result).as[DownstreamError]
        errorResponse.error.code shouldBe "404"
        errorResponse.error.message shouldBe "Not Found"
        errorResponse.error.logID shouldBe "ABCDEF1234567890ABCDEF1234567890"
      }

      "must return 422 Unprocessable Entity when VPD ID ends with 5" in {
        val testVpdId = "GBWK1234565WK"
        val request = FakeRequest()
        val result = controller.viewReturn(testVpdId, periodKey)(request)

        status(result) shouldBe UNPROCESSABLE_ENTITY
        val errorResponse = contentAsJson(result).as[EtmpDownstreamError]
        errorResponse.error.code shouldBe "002"
        errorResponse.error.text shouldBe "ID Number missing or invalid"
        errorResponse.error.processingDate should not be empty
      }

      "must return 500 Internal Server Error when VPD ID ends with 8" in {
        val testVpdId = "GBWK1234568WK"
        val request = FakeRequest()
        val result = controller.viewReturn(testVpdId, periodKey)(request)

        status(result) shouldBe INTERNAL_SERVER_ERROR
        val errorResponse = contentAsJson(result).as[DownstreamError]
        errorResponse.error.code shouldBe "500"
        errorResponse.error.message should include("SAP PI system is currently unavailable")
        errorResponse.error.logID shouldBe "ABCDEF1234567890ABCDEF1234567890"
      }

      "must process normally when VPD ID ends with 0" in {
        val testVpdId = "GBWK1234560WK"
        when(mockReturnSubmissionRepository.get(eqTo(testVpdId), eqTo(periodKey)))
          .thenReturn(Future.successful(Some(sampleSubmission.copy(vpdId = testVpdId))))

        val request = FakeRequest()
        val result = controller.viewReturn(testVpdId, periodKey)(request)

        status(result) shouldBe OK
        val responseJson = contentAsJson(result)
        (responseJson \ "success" \ "idDetails" \ "vpdReferenceNumber").as[String] shouldBe testVpdId
      }
    }
  }
}
