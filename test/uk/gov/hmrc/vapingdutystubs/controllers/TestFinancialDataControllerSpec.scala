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

package uk.gov.hmrc.vapingdutystubs.controllers

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, verify, when}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.vapingdutystubs.base.SpecBase
import uk.gov.hmrc.vapingdutystubs.controllers.testonly.TestFinancialDataController
import uk.gov.hmrc.vapingdutystubs.models.financialdata.FinancialDataState
import uk.gov.hmrc.vapingdutystubs.repositories.FinancialDataRepository

import scala.concurrent.Future

class TestFinancialDataControllerSpec extends SpecBase {

  private val SCENARIO_OUTSTANDING_ONLY = "outstanding-only"
  private val SCENARIO_WITH_UNALLOCATED = "with-unallocated"
  private val SCENARIO_CLEARED_ONLY = "cleared-only"
  private val SCENARIO_MIXED = "mixed"
  private val SCENARIO_NONE = "none"
  private val SCENARIO_SINGLE_OUTSTANDING = "single-outstanding"
  private val SCENARIO_OVERDUE_BALANCE = "overdue-balance"
  private val SCENARIO_CREDIT_BALANCE = "credit-balance"
  private val SCENARIO_NOTHING_OWED = "nothing-owed"
  private val SCENARIO_INTEREST_PAYMENT = "interest-payment"
  private val INVALID_SCENARIO = "invalid-scenario"

  val mockFinancialDataRepository: FinancialDataRepository = mock[FinancialDataRepository]

  val controller = new TestFinancialDataController(cc, mockFinancialDataRepository)

  val testState: FinancialDataState = FinancialDataState(
    vpdId = vpdId,
    noDataIdentified = false,
    documentDetails = Seq.empty,
    lastUpdated = java.time.Instant.now(clock)
  )

  val noDataState: FinancialDataState = testState.copy(noDataIdentified = true)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockFinancialDataRepository)
  }

  "TestFinancialDataController" - {

    "setScenario" - {
      Seq(
        SCENARIO_OUTSTANDING_ONLY,
        SCENARIO_WITH_UNALLOCATED,
        SCENARIO_CLEARED_ONLY,
        SCENARIO_MIXED,
        SCENARIO_SINGLE_OUTSTANDING,
        SCENARIO_OVERDUE_BALANCE,
        SCENARIO_CREDIT_BALANCE,
        SCENARIO_NOTHING_OWED,
        SCENARIO_INTEREST_PAYMENT
      ).foreach { scenario =>
        s"must return OK when setting '$scenario' scenario" in {
          when(mockFinancialDataRepository.set(any())).thenReturn(Future.successful(testState))

          val result: Future[Result] = controller.setScenario(vpdId, scenario)(fakeRequest)

          status(result) mustBe OK

          val jsonResponse = contentAsJson(result)
          (jsonResponse \ "message").as[String] must include(scenario)
          (jsonResponse \ "vpdId").as[String] mustBe vpdId
          (jsonResponse \ "scenario").as[String] mustBe scenario
          (jsonResponse \ "noDataIdentified").as[Boolean] mustBe false

          verify(mockFinancialDataRepository).set(any())
        }
      }

      "must return OK with noDataIdentified=true when setting 'none' scenario" in {
        when(mockFinancialDataRepository.set(any())).thenReturn(Future.successful(noDataState))

        val result: Future[Result] = controller.setScenario(vpdId, SCENARIO_NONE)(fakeRequest)

        status(result) mustBe OK

        val jsonResponse = contentAsJson(result)
        (jsonResponse \ "scenario").as[String] mustBe SCENARIO_NONE
        (jsonResponse \ "documentCount").as[Int] mustBe 0
        (jsonResponse \ "noDataIdentified").as[Boolean] mustBe true

        verify(mockFinancialDataRepository).set(any())
      }

      "must return BAD_REQUEST when scenario name is invalid" in {
        val result: Future[Result] = controller.setScenario(vpdId, INVALID_SCENARIO)(fakeRequest)

        status(result) mustBe BAD_REQUEST

        val jsonResponse = contentAsJson(result)
        (jsonResponse \ "error").as[String] mustBe "Invalid scenario"
        (jsonResponse \ "provided").as[String] mustBe INVALID_SCENARIO
      }
    }

    "clearForVpdId" - {
      "must return OK when financial data exists and is cleared successfully" in {
        when(mockFinancialDataRepository.get(eqTo(vpdId))).thenReturn(Future.successful(Some(testState)))
        when(mockFinancialDataRepository.set(any())).thenReturn(Future.successful(testState.copy(documentDetails = Seq.empty)))

        val result: Future[Result] = controller.clearForVpdId(vpdId)(fakeRequest)

        status(result) mustBe OK
        (contentAsJson(result) \ "vpdId").as[String] mustBe vpdId

        verify(mockFinancialDataRepository).get(eqTo(vpdId))
        verify(mockFinancialDataRepository).set(any())
      }

      "must return NOT_FOUND when no financial data exists for the VPD ID" in {
        when(mockFinancialDataRepository.get(eqTo(vpdId))).thenReturn(Future.successful(None))

        val result: Future[Result] = controller.clearForVpdId(vpdId)(fakeRequest)

        status(result) mustBe NOT_FOUND
        (contentAsJson(result) \ "vpdId").as[String] mustBe vpdId
      }
    }

    "clearAll" - {
      "must return OK when all data is cleared successfully" in {
        when(mockFinancialDataRepository.clear).thenReturn(Future.successful(Some(())))

        val result: Future[Result] = controller.clearAll()(fakeRequest)

        status(result) mustBe OK
        (contentAsJson(result) \ "message").as[String] must include("Successfully cleared all financial data")

        verify(mockFinancialDataRepository).clear
      }
    }

    "setCustom" - {
      "must return OK when valid FinancialDataState is provided" in {
        when(mockFinancialDataRepository.set(eqTo(testState))).thenReturn(Future.successful(testState))

        val result: Future[Result] = controller.setCustom(vpdId)(
          FakeRequest()
            .withHeaders("Content-Type" -> "application/json")
            .withBody(AnyContentAsJson(Json.toJson(testState)))
        )

        status(result) mustBe OK
        (contentAsJson(result) \ "vpdId").as[String] mustBe vpdId
        (contentAsJson(result) \ "documentCount").as[Int] mustBe testState.documentDetails.size

        verify(mockFinancialDataRepository).set(eqTo(testState))
      }

      "must return BAD_REQUEST when VPD ID in URL doesn't match body" in {
        val mismatchedState = testState.copy(vpdId = "XMVPD9999999999")

        val result: Future[Result] = controller.setCustom(vpdId)(
          FakeRequest()
            .withHeaders("Content-Type" -> "application/json")
            .withBody(AnyContentAsJson(Json.toJson(mismatchedState)))
        )

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "error").as[String] mustBe "VPD ID mismatch"
      }

      "must return BAD_REQUEST when JSON is invalid" in {
        val result: Future[Result] = controller.setCustom(vpdId)(
          FakeRequest()
            .withHeaders("Content-Type" -> "application/json")
            .withBody(AnyContentAsJson(Json.obj("invalid" -> "data")))
        )

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "error").as[String] mustBe "Invalid JSON"
      }

      "must return BAD_REQUEST when no JSON body is provided" in {
        val result: Future[Result] = controller.setCustom(vpdId)(fakeRequest)

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "error").as[String] mustBe "Missing JSON body"
      }
    }
  }
}
