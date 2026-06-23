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
import play.api.http.Status.{BAD_REQUEST, CREATED}
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.vapingdutystubs.base.SpecBase
import uk.gov.hmrc.vapingdutystubs.config.Constants.Headers.xZVPD
import uk.gov.hmrc.vapingdutystubs.models.returns.*
import uk.gov.hmrc.vapingdutystubs.models.returns.submit.ReturnCreateRequest
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
      totalDutyOverDeclaration = BigDecimal("0.00"),
      totalDutyUnderDeclaration = BigDecimal("0.00"),
      totalDutySpoiltProduct = BigDecimal("0.00"),
      adjustmentAmount = BigDecimal("0.00"),
      totalDue = BigDecimal("15752.63")
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
      
      val expectedSubmission = ReturnSubmission(
        vpdId = vpdId,
        periodKey = periodKey,
        chargeReference = chargeReference,
        submittedReturn = validReturnRequest,
        submittedAt = Instant.now(fixedClock),
        submissionId = submissionId
      )
      
      when(mockReturnSubmissionRepository.set(any())).thenReturn(Future.successful(expectedSubmission))
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
      (responseJson \ "success" \ "amount").as[BigDecimal] shouldBe BigDecimal("15752.63")
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
  }
}
