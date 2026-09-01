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
import uk.gov.hmrc.vapingdutystubs.controllers.testonly.TestObligationsController
import uk.gov.hmrc.vapingdutystubs.data.obligations.ObligationsData
import uk.gov.hmrc.vapingdutystubs.models.obligations.{Identification, ObligationDetails, ObligationItem, ObligationState}
import uk.gov.hmrc.vapingdutystubs.models.returns.*
import uk.gov.hmrc.vapingdutystubs.models.returns.submit.ReturnCreateRequest
import uk.gov.hmrc.vapingdutystubs.repositories.{ObligationsRepository, ReturnSubmissionRepository}

import java.time.{Instant, LocalDate}
import scala.concurrent.Future

class TestObligationsControllerSpec extends SpecBase {

  private val SCENARIO_ONLY_OPEN = "only-open"
  private val SCENARIO_ONLY_COMPLETED = "only-completed"
  private val SCENARIO_MIXED = "mixed"
  private val SCENARIO_NONE = "none"
  private val SCENARIO_ERROR = "error"
  private val INVALID_SCENARIO = "invalid-scenario"

  val mockObligationsRepository: ObligationsRepository = mock[ObligationsRepository]
  val mockReturnSubmissionRepository: ReturnSubmissionRepository = mock[ReturnSubmissionRepository]

  val controller = new TestObligationsController(
    cc,
    mockObligationsRepository,
    mockReturnSubmissionRepository
  )

  val testPeriodKey = "27AJ"

  val openObligationDetails = ObligationDetails(
    openOrFulfilledStatus = "O",
    iCFromDate = LocalDate.of(2027, 12, 1),
    iCToDate = LocalDate.of(2027, 12, 31),
    iCDateReceived = None,
    iCDueDate = LocalDate.of(2028, 1, 31),
    periodKey = "27AL"
  )

  val fulfilledObligationDetails = ObligationDetails(
    openOrFulfilledStatus = "F",
    iCFromDate = LocalDate.of(2027, 10, 1),
    iCToDate = LocalDate.of(2027, 10, 31),
    iCDateReceived = Some(LocalDate.of(2027, 11, 15)),
    iCDueDate = LocalDate.of(2027, 11, 30),
    periodKey = testPeriodKey
  )

  val testObligationItem = ObligationItem(
    identification = Identification(
      referenceType = "ZVPD",
      referenceNumber = vpdId,
      incomeSourceType = None
    ),
    obligationDetails = Seq(openObligationDetails, fulfilledObligationDetails)
  )

  val testObligationState = ObligationState(
    vpdId = vpdId,
    obligations = Seq(testObligationItem)
  )

  val emptyObligationItem = ObligationItem(
    identification = Identification(
      referenceType = "ZVPD",
      referenceNumber = vpdId,
      incomeSourceType = None
    ),
    obligationDetails = Seq.empty
  )

  val emptyObligationState = ObligationState(
    vpdId = vpdId,
    obligations = Seq(emptyObligationItem)
  )

  val sampleReturnRequest: ReturnCreateRequest = ReturnCreateRequest(
    periodKey = testPeriodKey,
    vapingProductsProduced = VapingProductsProduced(
      vapingProdManufactured = "0",
      returns = Seq.empty
    ),
    overDeclaration = None,
    underDeclaration = None,
    spoiltProduct = None,
    totalDutyDue = TotalDutyDue(
      totalDutyDueVapingProducts = zeroAmount,
      totalDutyOverDeclaration = zeroAmount,
      totalDutyUnderDeclaration = zeroAmount,
      totalDutySpoiltProduct = zeroAmount,
      totalDue = zeroAmount
    ),
    otherOptions = None,
    declaration = sampleDeclarationDetails
  )

  val testReturnSubmission = ReturnSubmission(
    vpdId = vpdId,
    periodKey = testPeriodKey,
    chargeReference = Some(chargeReference),
    submittedReturn = sampleReturnRequest,
    submittedAt = Instant.now(clock),
    submissionId = submissionId
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockObligationsRepository, mockReturnSubmissionRepository)
  }

  "TestObligationsController" - {

    "setScenario" - {

      "must return OK when setting 'only-open' scenario" in {
        when(mockObligationsRepository.set(any[ObligationState]()))
          .thenReturn(Future.successful(testObligationState))

        when(mockReturnSubmissionRepository.getAll(eqTo(vpdId)))
          .thenReturn(Future.successful(Seq.empty))

        val result: Future[Result] = controller.setScenario(vpdId, SCENARIO_ONLY_OPEN)(fakeRequest)

        status(result) mustBe OK

        val jsonResponse = contentAsJson(result)
        (jsonResponse \ "message").as[String] must include(SCENARIO_ONLY_OPEN)
        (jsonResponse \ "vpdId").as[String] mustBe vpdId
        (jsonResponse \ "scenario").as[String] mustBe SCENARIO_ONLY_OPEN
        (jsonResponse \ "obligationCount").as[Int] must be >= 0

        verify(mockObligationsRepository).set(any[ObligationState]())
      }

      "must return OK when setting 'only-completed' scenario" in {
        when(mockObligationsRepository.set(any[ObligationState]()))
          .thenReturn(Future.successful(testObligationState))

        when(mockReturnSubmissionRepository.set(any[ReturnSubmission]()))
          .thenReturn(Future.successful(testReturnSubmission))

        val result: Future[Result] = controller.setScenario(vpdId, SCENARIO_ONLY_COMPLETED)(fakeRequest)

        status(result) mustBe OK

        val jsonResponse = contentAsJson(result)
        (jsonResponse \ "message").as[String] must include(SCENARIO_ONLY_COMPLETED)
        (jsonResponse \ "vpdId").as[String] mustBe vpdId
        (jsonResponse \ "scenario").as[String] mustBe SCENARIO_ONLY_COMPLETED
        (jsonResponse \ "obligationCount").as[Int] must be >= 0

        verify(mockObligationsRepository).set(any[ObligationState]())
      }

      "must return OK when setting 'mixed' scenario" in {
        when(mockObligationsRepository.set(any[ObligationState]()))
          .thenReturn(Future.successful(testObligationState))

        when(mockReturnSubmissionRepository.set(any[ReturnSubmission]()))
          .thenReturn(Future.successful(testReturnSubmission))

        val result: Future[Result] = controller.setScenario(vpdId, SCENARIO_MIXED)(fakeRequest)

        status(result) mustBe OK

        val jsonResponse = contentAsJson(result)
        (jsonResponse \ "message").as[String] must include(SCENARIO_MIXED)
        (jsonResponse \ "vpdId").as[String] mustBe vpdId
        (jsonResponse \ "scenario").as[String] mustBe SCENARIO_MIXED
        (jsonResponse \ "obligationCount").as[Int] must be >= 0

        verify(mockObligationsRepository).set(any[ObligationState]())
      }

      "must return OK when setting 'none' scenario" in {
        when(mockObligationsRepository.set(any[ObligationState]()))
          .thenReturn(Future.successful(emptyObligationState))

        when(mockReturnSubmissionRepository.getAll(eqTo(vpdId)))
          .thenReturn(Future.successful(Seq.empty))

        val result: Future[Result] = controller.setScenario(vpdId, SCENARIO_NONE)(fakeRequest)

        status(result) mustBe OK

        val jsonResponse = contentAsJson(result)
        (jsonResponse \ "message").as[String] must include(SCENARIO_NONE)
        (jsonResponse \ "vpdId").as[String] mustBe vpdId
        (jsonResponse \ "scenario").as[String] mustBe SCENARIO_NONE
        (jsonResponse \ "obligationCount").as[Int] mustBe 0

        verify(mockObligationsRepository).set(any[ObligationState]())
      }

      "must return OK when setting 'error' scenario" in {
        when(mockObligationsRepository.set(eqTo(ObligationsData.simulatedError(vpdId))))
          .thenReturn(Future.successful(ObligationsData.simulatedError(vpdId)))

        when(mockReturnSubmissionRepository.getAll(eqTo(vpdId)))
          .thenReturn(Future.successful(Seq.empty))

        val result: Future[Result] = controller.setScenario(vpdId, SCENARIO_ERROR)(fakeRequest)

        status(result) mustBe OK

        val jsonResponse = contentAsJson(result)
        (jsonResponse \ "message").as[String] must include(SCENARIO_ERROR)
        (jsonResponse \ "vpdId").as[String] mustBe vpdId
        (jsonResponse \ "scenario").as[String] mustBe SCENARIO_ERROR
        (jsonResponse \ "obligationCount").as[Int] mustBe 0

        verify(mockObligationsRepository).set(eqTo(ObligationsData.simulatedError(vpdId)))
      }

      "must return BAD_REQUEST when scenario name is invalid" in {
        val result: Future[Result] = controller.setScenario(vpdId, INVALID_SCENARIO)(fakeRequest)

        status(result) mustBe BAD_REQUEST

        val jsonResponse = contentAsJson(result)
        (jsonResponse \ "error").as[String] mustBe "Invalid scenario"
        (jsonResponse \ "message").as[String] must include("Scenario must be one of")
        (jsonResponse \ "provided").as[String] mustBe INVALID_SCENARIO
      }
    }

    "clearForVpdId" - {

      "must return OK when obligations exist and are cleared successfully" in {
        when(mockObligationsRepository.get(eqTo(vpdId)))
          .thenReturn(Future.successful(Some(testObligationState)))

        when(mockObligationsRepository.set(any[ObligationState]()))
          .thenReturn(Future.successful(emptyObligationState))

        when(mockReturnSubmissionRepository.getAll(eqTo(vpdId)))
          .thenReturn(Future.successful(Seq(testReturnSubmission)))

        when(mockReturnSubmissionRepository.delete(eqTo(vpdId), eqTo(testPeriodKey)))
          .thenReturn(Future.successful(true))

        val result: Future[Result] = controller.clearForVpdId(vpdId)(fakeRequest)

        status(result) mustBe OK

        val jsonResponse = contentAsJson(result)
        (jsonResponse \ "message").as[String] must include("Successfully cleared obligations and returns")
        (jsonResponse \ "vpdId").as[String] mustBe vpdId

        verify(mockObligationsRepository).get(eqTo(vpdId))
        verify(mockObligationsRepository).set(any[ObligationState]())
        verify(mockReturnSubmissionRepository).getAll(eqTo(vpdId))
      }

      "must return OK when obligations exist but no returns to clear" in {
        when(mockObligationsRepository.get(eqTo(vpdId)))
          .thenReturn(Future.successful(Some(testObligationState)))

        when(mockObligationsRepository.set(any[ObligationState]()))
          .thenReturn(Future.successful(emptyObligationState))

        when(mockReturnSubmissionRepository.getAll(eqTo(vpdId)))
          .thenReturn(Future.successful(Seq.empty))

        val result: Future[Result] = controller.clearForVpdId(vpdId)(fakeRequest)

        status(result) mustBe OK

        val jsonResponse = contentAsJson(result)
        (jsonResponse \ "message").as[String] must include("Successfully cleared obligations and returns")
        (jsonResponse \ "vpdId").as[String] mustBe vpdId

        verify(mockObligationsRepository).get(eqTo(vpdId))
        verify(mockObligationsRepository).set(any[ObligationState]())
        verify(mockReturnSubmissionRepository).getAll(eqTo(vpdId))
      }

      "must return NOT_FOUND when no obligations exist for the VPD ID" in {
        when(mockObligationsRepository.get(eqTo(vpdId)))
          .thenReturn(Future.successful(None))

        val result: Future[Result] = controller.clearForVpdId(vpdId)(fakeRequest)

        status(result) mustBe NOT_FOUND

        val jsonResponse = contentAsJson(result)
        (jsonResponse \ "message").as[String] must include("No obligations found")
        (jsonResponse \ "vpdId").as[String] mustBe vpdId

        verify(mockObligationsRepository).get(eqTo(vpdId))
      }
    }

    "clearAll" - {

      "must return OK when all data is cleared successfully" in {
        when(mockObligationsRepository.clear)
          .thenReturn(Future.successful(Some(())))

        when(mockReturnSubmissionRepository.clear)
          .thenReturn(Future.successful(Some(())))

        val result: Future[Result] = controller.clearAll()(fakeRequest)

        status(result) mustBe OK

        val jsonResponse = contentAsJson(result)
        (jsonResponse \ "message").as[String] must include("Successfully cleared all obligations and returns data")

        verify(mockObligationsRepository).clear
        verify(mockReturnSubmissionRepository).clear
      }
    }

    "setCustom" - {

      "must return OK when valid ObligationState is provided" in {
        when(mockObligationsRepository.set(eqTo(testObligationState)))
          .thenReturn(Future.successful(testObligationState))

        val result: Future[Result] = controller.setCustom(vpdId)(
          FakeRequest()
            .withHeaders("Content-Type" -> "application/json")
            .withBody(AnyContentAsJson(Json.toJson(testObligationState)))
        )

        status(result) mustBe OK

        val jsonResponse = contentAsJson(result)
        (jsonResponse \ "message").as[String] must include("Successfully set custom obligations")
        (jsonResponse \ "vpdId").as[String] mustBe vpdId
        (jsonResponse \ "obligationCount").as[Int] mustBe testObligationState.obligations.head.obligationDetails.size

        verify(mockObligationsRepository).set(eqTo(testObligationState))
      }

      "must return BAD_REQUEST when VPD ID in URL doesn't match body" in {
        val mismatchedVpdId = "XMVPD9999999999"
        val mismatchedState = testObligationState.copy(vpdId = mismatchedVpdId)

        val result: Future[Result] = controller.setCustom(vpdId)(
          FakeRequest()
            .withHeaders("Content-Type" -> "application/json")
            .withBody(AnyContentAsJson(Json.toJson(mismatchedState)))
        )

        status(result) mustBe BAD_REQUEST

        val jsonResponse = contentAsJson(result)
        (jsonResponse \ "error").as[String] mustBe "VPD ID mismatch"
        (jsonResponse \ "message").as[String] must include("VPD ID in URL")
        (jsonResponse \ "message").as[String] must include("does not match VPD ID in body")
      }

      "must return BAD_REQUEST when JSON is invalid" in {
        val invalidJson = Json.obj(
          "invalid" -> "data",
          "missing" -> "required fields"
        )

        val result: Future[Result] = controller.setCustom(vpdId)(
          FakeRequest()
            .withHeaders("Content-Type" -> "application/json")
            .withBody(AnyContentAsJson(invalidJson))
        )

        status(result) mustBe BAD_REQUEST

        val jsonResponse = contentAsJson(result)
        (jsonResponse \ "error").as[String] mustBe "Invalid JSON"
        (jsonResponse \ "message").as[String] must include("Could not parse obligation state")
      }

      "must return BAD_REQUEST when no JSON body is provided" in {
        val result: Future[Result] = controller.setCustom(vpdId)(fakeRequest)

        status(result) mustBe BAD_REQUEST

        val jsonResponse = contentAsJson(result)
        (jsonResponse \ "error").as[String] mustBe "Missing JSON body"
        (jsonResponse \ "message").as[String] must include("Request body must contain valid ObligationState JSON")
      }
    }
  }
}
