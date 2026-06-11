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
import org.mockito.Mockito.{reset, when}
import play.api.mvc.Result
import uk.gov.hmrc.vapingdutystubs.base.SpecBase
import uk.gov.hmrc.vapingdutystubs.models.returns.{RegularReturn, ReturnSubmission, TotalDutyDue, VapingProductsProduced}
import uk.gov.hmrc.vapingdutystubs.models.returns.submit.ReturnCreateRequest
import uk.gov.hmrc.vapingdutystubs.models.returns.view.ReturnDisplayResponse
import uk.gov.hmrc.vapingdutystubs.repositories.ReturnSubmissionRepository

import java.time.Instant
import scala.concurrent.Future

class ViewReturnControllerSpec extends SpecBase {

  val mockReturnSubmissionRepository: ReturnSubmissionRepository = mock[ReturnSubmissionRepository]

  val controller = new ViewReturnController(
    cc,
    mockReturnSubmissionRepository
  )

  val testPeriodKey = "27AJ"
  val testChargeReference = "XMVPD0123456789ab"
  val testSubmissionId = "submission-001"
  val testSubmittedAt = Instant.parse("2027-11-15T10:30:00Z")

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
    ),
    declaration = sampleDeclarationDetails
  )

  val testSubmission = ReturnSubmission(
    vpdId = vpdId,
    periodKey = testPeriodKey,
    chargeReference = testChargeReference,
    submittedReturn = sampleReturnRequest,
    submittedAt = testSubmittedAt,
    submissionId = testSubmissionId
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockReturnSubmissionRepository)
  }

  "viewReturn must" - {
    "return 200 OK with stored return data when submission exists" in {
      when(mockReturnSubmissionRepository.get(eqTo(vpdId), eqTo(testPeriodKey)))
        .thenReturn(Future.successful(Some(testSubmission)))

      val result: Future[Result] = controller.viewReturn(vpdId, testPeriodKey)(fakeRequest)

      status(result) mustBe OK
      
      val response = contentAsJson(result).as[ReturnDisplayResponse]
      response.success.idDetails.value.vpdReference mustBe vpdId
      response.success.idDetails.value.submissionId.value mustBe testSubmissionId
      response.success.chargeDetails.value.chargeReference.value mustBe testChargeReference
      response.success.chargeDetails.value.periodKey mustBe testPeriodKey
    }

    "return 200 OK with generated data when submission does not exist" in {
      when(mockReturnSubmissionRepository.get(eqTo(vpdId), eqTo(testPeriodKey)))
        .thenReturn(Future.successful(None))

      val result: Future[Result] = controller.viewReturn(vpdId, testPeriodKey)(fakeRequest)

      status(result) mustBe OK
      
      val response = contentAsJson(result).as[ReturnDisplayResponse]
      response.success.idDetails.value.vpdReference mustBe vpdId
      response.success.chargeDetails.value.periodKey mustBe testPeriodKey
    }
  }
}