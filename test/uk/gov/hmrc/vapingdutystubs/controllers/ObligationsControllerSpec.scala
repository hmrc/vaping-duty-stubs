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

package uk.gov.hmrc.vapingdutystubs.controllers

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, when}
import play.api.mvc.Result
import uk.gov.hmrc.vapingdutystubs.base.SpecBase
import uk.gov.hmrc.vapingdutystubs.models.obligations.{ObligationDetails, ObligationItem, ObligationState, ObligationsResponse}
import uk.gov.hmrc.vapingdutystubs.repositories.ObligationsRepository

import java.time.LocalDate
import scala.concurrent.Future

class ObligationsControllerSpec extends SpecBase {

  val mockObligationsRepository: ObligationsRepository = mock[ObligationsRepository]

  val controller = new ObligationsController(
    cc,
    mockObligationsRepository
  )

  val testPeriodKey = "27AJ"
  val currentDate = LocalDate.now()

  val openObligation = ObligationItem(
    identification = None,
    obligationDetails = ObligationDetails(
      openOrFulfilledStatus = "O",
      iCFromDate = LocalDate.of(2027, 12, 1),
      iCToDate = LocalDate.of(2027, 12, 31),
      iCDateReceived = None,
      iCDueDate = currentDate.plusDays(10),
      periodKey = "27AL"
    )
  )

  val fulfilledObligation = ObligationItem(
    identification = None,
    obligationDetails = ObligationDetails(
      openOrFulfilledStatus = "F",
      iCFromDate = LocalDate.of(2027, 10, 1),
      iCToDate = LocalDate.of(2027, 10, 31),
      iCDateReceived = Some(LocalDate.of(2027, 11, 15)),
      iCDueDate = LocalDate.of(2027, 11, 30),
      periodKey = testPeriodKey
    )
  )

  val testObligationState = ObligationState(
    vpdId = vpdId,
    obligations = Seq(openObligation, fulfilledObligation)
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockObligationsRepository)
  }

  "get must" - {
    "return 200 OK with stored obligations when they exist" in {
      when(mockObligationsRepository.get(eqTo(vpdId)))
        .thenReturn(Future.successful(Some(testObligationState)))

      val result: Future[Result] = controller.get()(fakeRequestWithParameters(
        Map[String, String](
          "displayRequest" -> "A",
          "referenceNumber" -> vpdId
        )
      ))

      status(result) mustBe OK
      
      val response = contentAsJson(result).as[ObligationsResponse]
      response.obligation.size mustBe 2
      response.obligation.head.obligationDetails.openOrFulfilledStatus mustBe "O"
      response.obligation(1).obligationDetails.openOrFulfilledStatus mustBe "F"
      response.obligation(1).obligationDetails.periodKey mustBe testPeriodKey
    }

    "return 200 OK with generated obligations when none are stored" in {
      when(mockObligationsRepository.get(eqTo(vpdId)))
        .thenReturn(Future.successful(None))

      when(mockObligationsRepository.set(any()))
        .thenReturn(Future.successful(testObligationState))

      val result: Future[Result] = controller.get()(fakeRequestWithParameters(
        Map[String, String](
          "displayRequest" -> "A",
          "referenceNumber" -> vpdId
        )
      ))

      status(result) mustBe OK
      
      val response = contentAsJson(result).as[ObligationsResponse]
      response.obligation.size mustBe 3 // Generated mock data has 3 obligations
    }
  }
}