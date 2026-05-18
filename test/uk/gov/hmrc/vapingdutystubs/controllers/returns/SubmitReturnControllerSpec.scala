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

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.vapingdutystubs.base.SpecBase
import uk.gov.hmrc.vapingdutystubs.config.Constants.Headers.xZVPD
import uk.gov.hmrc.vapingdutystubs.models.returns.{RegularReturn, ReturnSubmission, TotalDutyDue, VapingProductsProduced}
import uk.gov.hmrc.vapingdutystubs.models.returns.submit.{ReturnCreateRequest, ReturnCreateResponse}
import uk.gov.hmrc.vapingdutystubs.repositories.{ObligationsRepository, ReturnSubmissionRepository}

import scala.concurrent.Future

class SubmitReturnControllerSpec extends SpecBase {

  val mockReturnSubmissionRepository: ReturnSubmissionRepository = mock[ReturnSubmissionRepository]
  val mockObligationsRepository: ObligationsRepository = mock[ObligationsRepository]

  val controller = new SubmitReturnController(
    cc,
    mockReturnSubmissionRepository,
    mockObligationsRepository,
    uuidGenerator,
    clock
  )

  val testPeriodKey = "27AJ"

  val sampleReturnRequest = ReturnCreateRequest(
    periodKey = testPeriodKey,
    vapingProductsProduced = VapingProductsProduced(
      nilReturn = Seq.empty,
      regularReturn = Seq(RegularReturn(
        taxType = "301",
        dutyRate = BigDecimal("0.05"),
        amountProducedLiquid = BigDecimal("1000.50"),
        dutyDue = BigDecimal("50.03")
      ))
    ),
    totalDutyDue = TotalDutyDue(
      totalDutyDue = BigDecimal("50.03"),
      totalDutyDueVapingProducts = BigDecimal("50.03"),
      totalDutyOverDeclaration = BigDecimal("0.00"),
      totalDutySpoiltProduct = BigDecimal("0.00"),
      totalDutyUnderDeclaration = BigDecimal("0.00"),
      adjustmentAmount = BigDecimal("0.00")
    )
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockReturnSubmissionRepository, mockObligationsRepository)
  }

  "submitReturn must" - {
    "return 201 CREATED with JSON containing a ReturnCreateResponse when submission is successful" in {
      when(mockReturnSubmissionRepository.set(any[ReturnSubmission])).thenReturn(Future.successful(mock[ReturnSubmission]))
      when(mockObligationsRepository.markAsFulfilled(any[String], any[String], any[java.time.Instant]))
        .thenReturn(Future.successful(None))

      val result: Future[Result] = controller.submitReturn()(
        fakeRequestWithJsonBody(Json.toJson(sampleReturnRequest))
          .withHeaders(submitCorrelationIdHeader() :+ (xZVPD -> vpdId): _*)
      )

      status(result) mustBe CREATED
      
      val response = contentAsJson(result).as[ReturnCreateResponse]
      response.success.vpdReferenceNumber mustBe vpdId
      response.success.submissionID.isDefined mustBe true
      response.success.chargeReference.isDefined mustBe true
      response.success.amount mustBe BigDecimal("50.03")

      verify(mockReturnSubmissionRepository, times(1)).set(any[ReturnSubmission])
      verify(mockObligationsRepository, times(1)).markAsFulfilled(eqTo(vpdId), eqTo(testPeriodKey), any[java.time.Instant])
    }

    "return 400 BAD_REQUEST when JSON is invalid" in {
      val result: Future[Result] = controller.submitReturn()(
        fakeRequestWithJsonBody(Json.obj("invalid" -> "data"))
          .withHeaders(submitCorrelationIdHeader() :+ (xZVPD -> vpdId): _*)
      )

      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.obj("error" -> "Invalid request body")
    }
  }
}
