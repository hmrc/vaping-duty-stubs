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
import play.api.mvc.Result
import uk.gov.hmrc.vapingdutystubs.base.SpecBase
import uk.gov.hmrc.vapingdutystubs.models.financialdata.{DocumentDetails, FinancialDataResponse, FinancialDataState, LineItemDetails}
import uk.gov.hmrc.vapingdutystubs.repositories.FinancialDataRepository

import java.time.{Instant, LocalDate}
import scala.concurrent.Future

class FinancialDataControllerSpec extends SpecBase {

  val mockFinancialDataRepository: FinancialDataRepository = mock[FinancialDataRepository]

  val controller = new FinancialDataController(cc, mockFinancialDataRepository, clock)

  private def queryRequestJson(idNumber: String) = Json.obj(
    "taxRegime" -> "VPD",
    "taxpayerInformation" -> Json.obj("idType" -> "ZVPD", "idNumber" -> idNumber),
    "selectionCriteria" -> Json.obj(
      "dateRange" -> Json.obj("dateType" -> "POSTING", "dateFrom" -> "2026-10-01", "dateTo" -> "2026-12-31"),
      "includeClearedItems" -> true,
      "includeStatisticalItems" -> false,
      "includePaymentOnAccount" -> false
    ),
    "dataEnrichment" -> Json.obj(
      "addRegimeTotalisation" -> true,
      "addLockInformation" -> false,
      "addPenaltyDetails" -> true,
      "addPostedInterestDetails" -> false,
      "addAccruingInterestDetails" -> true
    )
  )

  val testDocument: DocumentDetails = DocumentDetails(
    documentNumber = "1000000000001",
    documentType = "TRM New Charge",
    chargeReferenceNumber = Some("XMVPD0000000001"),
    businessPartnerNumber = s"BP$vpdId",
    contractAccountNumber = s"CA$vpdId",
    contractAccountCategory = "Excise",
    contractObjectNumber = s"CO$vpdId",
    contractObjectType = "ZVPD",
    postingDate = LocalDate.of(2026, 10, 31),
    issueDate = LocalDate.of(2026, 10, 31),
    documentTotalAmount = BigDecimal("500.00"),
    documentClearedAmount = BigDecimal(0),
    documentOutstandingAmount = BigDecimal("500.00"),
    lineItemDetails = Seq(LineItemDetails(
      itemNumber = "0001",
      subItemNumber = "001",
      mainTransaction = "4060",
      subTransaction = "3392",
      chargeDescription = "VPD Return",
      periodFromDate = LocalDate.of(2026, 10, 1),
      periodToDate = LocalDate.of(2026, 10, 31),
      periodKey = "26KJ",
      netDueDate = LocalDate.of(2026, 11, 14),
      formBundleNumber = "FB1",
      statisticalKey = "1",
      amount = BigDecimal("500.00")
    ))
  )

  val testState: FinancialDataState = FinancialDataState(
    vpdId = vpdId,
    noDataIdentified = false,
    documentDetails = Seq(testDocument),
    lastUpdated = Instant.now(clock)
  )

  val noDataState: FinancialDataState = FinancialDataState(
    vpdId = vpdId,
    noDataIdentified = true,
    documentDetails = Seq.empty,
    lastUpdated = Instant.now(clock)
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockFinancialDataRepository)
  }

  "query must" - {
    "return 201 Created with stored documents when they exist" in {
      when(mockFinancialDataRepository.get(eqTo(vpdId))).thenReturn(Future.successful(Some(testState)))

      val result: Future[Result] = controller.query()(fakeRequestWithJsonBody(queryRequestJson(vpdId)))

      status(result) mustBe CREATED
      val response = contentAsJson(result).as[FinancialDataResponse]
      response.success.financialData.documentDetails.size mustBe 1
      response.success.financialData.documentDetails.head.chargeReferenceNumber mustBe Some("XMVPD0000000001")
    }

    "return 422 Unprocessable Entity with code 018 when no data is identified" in {
      when(mockFinancialDataRepository.get(eqTo(vpdId))).thenReturn(Future.successful(Some(noDataState)))

      val result: Future[Result] = controller.query()(fakeRequestWithJsonBody(queryRequestJson(vpdId)))

      status(result) mustBe UNPROCESSABLE_ENTITY
      val jsonResponse = contentAsJson(result)
      (jsonResponse \ "errors" \ "code").as[String] mustBe "018"
      (jsonResponse \ "errors" \ "text").as[String] mustBe "No Data Identified"
    }

    "return 201 Created with generated default data when none is stored, and persist it" in {
      when(mockFinancialDataRepository.get(eqTo(vpdId))).thenReturn(Future.successful(None))
      when(mockFinancialDataRepository.set(any())).thenReturn(Future.successful(testState))

      val result: Future[Result] = controller.query()(fakeRequestWithJsonBody(queryRequestJson(vpdId)))

      status(result) mustBe CREATED
      verify(mockFinancialDataRepository).set(any())
    }

    "return 400 Bad Request when the request body cannot be parsed" in {
      val result: Future[Result] = controller.query()(fakeRequestWithJsonBody(Json.obj("foo" -> "bar")))

      status(result) mustBe BAD_REQUEST
    }
  }
}
