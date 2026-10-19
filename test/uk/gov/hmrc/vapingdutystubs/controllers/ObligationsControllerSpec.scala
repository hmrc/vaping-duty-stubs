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
import uk.gov.hmrc.vapingdutystubs.models.{DownstreamError, ObligationsDownstreamErrors}
import uk.gov.hmrc.vapingdutystubs.models.obligations.{Identification, ObligationDetails, ObligationItem, ObligationState, ObligationsResponse}
import uk.gov.hmrc.vapingdutystubs.repositories.ObligationsRepository

import java.time.{Instant, LocalDate}
import scala.concurrent.Future

class ObligationsControllerSpec extends SpecBase {

  val mockObligationsRepository: ObligationsRepository = mock[ObligationsRepository]

  val controller = new ObligationsController(
    cc,
    mockObligationsRepository
  )

  val testPeriodKey = "27KJ"
  val currentDate: LocalDate = LocalDate.now(clock)

  val openObligationDetails = ObligationDetails(
    openOrFulfilledStatus = "O",
    iCFromDate = LocalDate.of(2027, 12, 1),
    iCToDate = LocalDate.of(2027, 12, 31),
    iCDateReceived = None,
    iCDueDate = currentDate.plusDays(10),
    periodKey = "27KL"
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
          "referenceNumber" -> vpdId,
          "fromDate" -> "2027-01-01",
          "toDate" -> "2027-12-31"
        )
      ))

      status(result) mustBe OK
      
      val response = contentAsJson(result).as[ObligationsResponse]
      response.obligation.size mustBe 1
      response.obligation.head.identification.referenceType mustBe "ZVPD"
      response.obligation.head.identification.referenceNumber mustBe vpdId
      response.obligation.head.obligationDetails.size mustBe 2
      response.obligation.head.obligationDetails.head.openOrFulfilledStatus mustBe "O"
      response.obligation.head.obligationDetails(1).openOrFulfilledStatus mustBe "F"
      response.obligation.head.obligationDetails(1).periodKey mustBe testPeriodKey
    }

    "return 500 INTERNAL_SERVER_ERROR when the stored obligations have simulateError set" in {
      when(mockObligationsRepository.get(eqTo(vpdId)))
        .thenReturn(Future.successful(Some(testObligationState.copy(simulateError = true))))

      val result: Future[Result] = controller.get()(fakeRequestWithParameters(
        Map[String, String](
          "displayRequest" -> "A",
          "referenceNumber" -> vpdId,
          "fromDate" -> "2027-01-01",
          "toDate" -> "2027-12-31"
        )
      ))

      status(result) mustBe INTERNAL_SERVER_ERROR
      val errorResponse = contentAsJson(result).as[DownstreamError]
      errorResponse.error.code mustBe "500"
      errorResponse.error.message mustBe "Simulated obligations failure"
      errorResponse.error.logID mustBe "ABCDEF1234567890ABCDEF1234567890"
    }

    "return 422 UNPROCESSABLE_ENTITY last numeric digit in referenceNumber is a 5" in {
      when(mockObligationsRepository.get(eqTo("XIWK5555555WK")))
        .thenReturn(Future.successful(Some(testObligationState)))

      val result: Future[Result] = controller.get()(fakeRequestWithParameters(
        Map[String, String](
          "displayRequest" -> "A",
          "referenceNumber" -> "XIWK5555555WK",
          "fromDate" -> "2027-01-01",
          "toDate" -> "2027-12-31"
        )
      ))

      status(result) mustBe UNPROCESSABLE_ENTITY
      val errorResponse = contentAsJson(result).as[ObligationsDownstreamErrors]
      errorResponse.errors.code mustBe "025"
      errorResponse.errors.text mustBe "No associated data found"
      Instant.parse(errorResponse.errors.processingDate) mustBe a[Instant]
    }

    "return 200 OK with generated obligations when none are stored" in {
      when(mockObligationsRepository.get(eqTo(vpdId)))
        .thenReturn(Future.successful(None))

      when(mockObligationsRepository.set(any()))
        .thenReturn(Future.successful(testObligationState))

      val result: Future[Result] = controller.get()(fakeRequestWithParameters(
        Map[String, String](
          "displayRequest" -> "A",
          "referenceNumber" -> vpdId,
          "fromDate" -> "2027-01-01",
          "toDate" -> "2027-12-31"
        )
      ))

      status(result) mustBe OK
      
      val response = contentAsJson(result).as[ObligationsResponse]
      response.obligation.size mustBe 1
      response.obligation.head.obligationDetails.size mustBe 36
    }

    "filter stored obligations by date range" - {
      def monthObligation(year: Int, month: Int, periodKey: String): ObligationDetails = {
        val periodStart = LocalDate.of(year, month, 1)
        fulfilledObligationDetails.copy(
          iCFromDate = periodStart,
          iCToDate   = periodStart.withDayOfMonth(periodStart.lengthOfMonth),
          periodKey  = periodKey
        )
      }

      def stateWith(details: ObligationDetails*): ObligationState =
        testObligationState.copy(obligations = Seq(testObligationItem.copy(obligationDetails = details)))

      def getPeriodKeys(fromDate: String, toDate: String): Seq[String] = {
        val result = controller.get()(fakeRequestWithParameters(
          Map[String, String](
            "displayRequest"  -> "A",
            "referenceNumber" -> vpdId,
            "fromDate"        -> fromDate,
            "toDate"          -> toDate
          )
        ))

        status(result) mustBe OK
        contentAsJson(result).as[ObligationsResponse].obligation.head.obligationDetails.map(_.periodKey)
      }

      // Results observed by querying the Obligations API in QA with a single October obligation
      val october = monthObligation(2020, 10, "20AJ")

      val qaScenarios = Seq(
        ("2020-01-01", "2020-12-31", Seq("20AJ")),
        ("2020-10-13", "2020-12-31", Seq("20AJ")),
        ("2020-10-13", "2020-10-31", Seq("20AJ")),
        ("2020-10-13", "2020-10-30", Seq("20AJ")),
        ("2020-10-31", "2020-11-10", Seq("20AJ")),
        ("2020-10-10", "2020-10-11", Seq("20AJ")),
        ("2020-11-01", "2020-11-10", Seq.empty),
        ("2020-09-30", "2020-10-01", Seq("20AJ")),
        ("2020-09-20", "2020-09-30", Seq.empty)
      )

      qaScenarios.foreach { case (fromDate, toDate, expected) =>
        val outcome = if (expected.isEmpty) "exclude" else "include"
        s"must $outcome an October obligation when querying $fromDate to $toDate" in {
          when(mockObligationsRepository.get(eqTo(vpdId)))
            .thenReturn(Future.successful(Some(stateWith(october))))

          getPeriodKeys(fromDate, toDate) mustBe expected
        }
      }

      "must include the months at both ends of the range used by vaping-duty, and exclude the current month" in {
        when(mockObligationsRepository.get(eqTo(vpdId)))
          .thenReturn(Future.successful(Some(stateWith(
            monthObligation(2023, 9, "23AI"),
            monthObligation(2023, 10, "23AJ"),
            monthObligation(2026, 9, "26AI"),
            monthObligation(2026, 10, "26AJ")
          ))))

        // vaping-duty's range when today is 2026-10-16
        getPeriodKeys("2023-10-31", "2026-09-01") mustBe Seq("23AJ", "26AI")
      }

      val september = monthObligation(2020, 9, "20AI")
      val november  = monthObligation(2020, 11, "20AK")

      val multiObligationScenarios = Seq(
        // Query range contains all obligations
        ("2020-01-01", "2020-12-31", Seq("20AI", "20AJ", "20AK")),
        ("2020-09-01", "2020-11-30", Seq("20AI", "20AJ", "20AK")), // exactly the obligation bounds
        ("2020-08-31", "2020-12-01", Seq("20AI", "20AJ", "20AK")), // one day either side

        // Query range inside the obligations
        ("2020-10-10", "2020-10-20", Seq("20AJ")),
        ("2020-10-01", "2020-10-31", Seq("20AJ")),                 // exactly one month
        ("2020-09-30", "2020-10-01", Seq("20AI", "20AJ")),         // straddles month boundary
        ("2020-10-31", "2020-11-01", Seq("20AJ", "20AK")),         // straddles month boundary
        ("2020-09-02", "2020-11-29", Seq("20AI", "20AJ", "20AK")), // one day in from each end

        // Query range starts inside the obligations and ends after them
        ("2020-10-15", "2020-12-31", Seq("20AJ", "20AK")),
        ("2020-10-01", "2020-12-31", Seq("20AJ", "20AK")),         // starts on first day of month
        ("2020-09-30", "2020-12-31", Seq("20AI", "20AJ", "20AK")), // starts on last day of month
        ("2020-11-30", "2020-12-31", Seq("20AK")),                 // starts on last obligation day

        // Query range starts before the obligations and ends inside them
        ("2020-08-01", "2020-10-15", Seq("20AI", "20AJ")),
        ("2020-08-01", "2020-10-31", Seq("20AI", "20AJ")),         // ends on last day of month
        ("2020-08-01", "2020-11-01", Seq("20AI", "20AJ", "20AK")), // ends on first day of month
        ("2020-08-01", "2020-09-01", Seq("20AI")),                 // ends on first obligation day

        // Query range entirely outside the obligations
        ("2020-08-01", "2020-08-31", Seq.empty),
        ("2020-12-01", "2020-12-31", Seq.empty)
      )

      multiObligationScenarios.foreach { case (fromDate, toDate, expected) =>
        val outcome = if (expected.isEmpty) "no obligations" else expected.mkString(", ")
        s"must return $outcome from September to November obligations when querying $fromDate to $toDate" in {
          when(mockObligationsRepository.get(eqTo(vpdId)))
            .thenReturn(Future.successful(Some(stateWith(september, october, november))))

          getPeriodKeys(fromDate, toDate) mustBe expected
        }
      }

      "must preserve the stored order of matching obligations" in {
        when(mockObligationsRepository.get(eqTo(vpdId)))
          .thenReturn(Future.successful(Some(stateWith(november, september, october))))

        getPeriodKeys("2020-01-01", "2020-12-31") mustBe Seq("20AK", "20AI", "20AJ")
      }
    }
  }
}