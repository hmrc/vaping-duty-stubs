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

package uk.gov.hmrc.vapingdutystubs.services.returns

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers.*
import uk.gov.hmrc.vapingdutystubs.base.SpecBase
import uk.gov.hmrc.vapingdutystubs.models.financialdata.FinancialDataState
import uk.gov.hmrc.vapingdutystubs.models.returns.*
import uk.gov.hmrc.vapingdutystubs.models.returns.submit.ReturnCreateRequest
import uk.gov.hmrc.vapingdutystubs.repositories.{FinancialDataRepository, ObligationsRepository, ReturnSubmissionRepository}
import uk.gov.hmrc.vapingdutystubs.utils.{ChargeReferenceGenerator, RandomUUIDGenerator}

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class ReturnSubmissionServiceSpec extends SpecBase with ScalaFutures with org.scalatest.BeforeAndAfterEach {

  private val mockReturnSubmissionRepository: ReturnSubmissionRepository = mock[ReturnSubmissionRepository]
  private val mockObligationsRepository: ObligationsRepository           = mock[ObligationsRepository]
  private val mockFinancialDataRepository: FinancialDataRepository       = mock[FinancialDataRepository]
  private val mockChargeReferenceGenerator: ChargeReferenceGenerator     = mock[ChargeReferenceGenerator]
  private val mockUuidGenerator: RandomUUIDGenerator                     = mock[RandomUUIDGenerator]
  private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-05-28T10:30:00Z"), ZoneId.of("UTC"))

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(
      mockReturnSubmissionRepository,
      mockObligationsRepository,
      mockFinancialDataRepository,
      mockChargeReferenceGenerator,
      mockUuidGenerator
    )
  }

  private val service = new ReturnSubmissionService(
    mockReturnSubmissionRepository,
    mockObligationsRepository,
    mockFinancialDataRepository,
    mockChargeReferenceGenerator,
    mockUuidGenerator,
    fixedClock
  )

  override val vpdId = "GBWK1234467WK"
  private val periodKey = "24AA"
  override val chargeReference = "XMVPD123456789012"
  override val submissionId = "123456789012"

  private val validReturnRequest = ReturnCreateRequest(
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

  "ReturnSubmissionService" - {

    "processSubmission must" - {

      "successfully process a valid return with charge reference" in {
        when(mockUuidGenerator.uuid).thenReturn(submissionId)
        when(mockChargeReferenceGenerator.generate(any[BigDecimal])).thenReturn(Some(chargeReference))

        val expectedSubmission = ReturnSubmission(
          vpdId = vpdId,
          periodKey = periodKey,
          chargeReference = Some(chargeReference),
          submittedReturn = validReturnRequest,
          submittedAt = Instant.now(fixedClock),
          submissionId = submissionId
        )

        when(mockReturnSubmissionRepository.set(any[ReturnSubmission]))
          .thenReturn(Future.successful(expectedSubmission))
        when(mockObligationsRepository.markAsFulfilled(any[String], any[String], any[Instant]))
          .thenReturn(Future.successful(None))
        when(mockFinancialDataRepository.get(any[String]))
          .thenReturn(Future.successful(None))
        when(mockFinancialDataRepository.set(any[FinancialDataState]))
          .thenReturn(Future.successful(FinancialDataState(
            vpdId = vpdId,
            noDataIdentified = false,
            documentDetails = Seq.empty,
            lastUpdated = Instant.now(fixedClock)
          )))

        val result = service.processSubmission(vpdId, validReturnRequest)

        whenReady(result) { response =>
          response shouldBe Right(expectedSubmission)
          verify(mockChargeReferenceGenerator).generate(eqTo(BigDecimal("15752.63")))
          verify(mockReturnSubmissionRepository).set(any[ReturnSubmission])
          verify(mockObligationsRepository).markAsFulfilled(eqTo(vpdId), eqTo(periodKey), any[Instant])
          verify(mockFinancialDataRepository).get(eqTo(vpdId))
          verify(mockFinancialDataRepository).set(any[FinancialDataState])
        }
      }

      "successfully process a nil return without charge reference" in {
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

        when(mockUuidGenerator.uuid).thenReturn(submissionId)
        when(mockChargeReferenceGenerator.generate(any[BigDecimal])).thenReturn(None)

        val expectedSubmission = ReturnSubmission(
          vpdId = vpdId,
          periodKey = periodKey,
          chargeReference = None,
          submittedReturn = nilReturnRequest,
          submittedAt = Instant.now(fixedClock),
          submissionId = submissionId
        )

        when(mockReturnSubmissionRepository.set(any[ReturnSubmission]))
          .thenReturn(Future.successful(expectedSubmission))
        when(mockObligationsRepository.markAsFulfilled(any[String], any[String], any[Instant]))
          .thenReturn(Future.successful(None))

        val result = service.processSubmission(vpdId, nilReturnRequest)

        whenReady(result) { response =>
          response shouldBe Right(expectedSubmission)
          verify(mockChargeReferenceGenerator).generate(eqTo(BigDecimal("0.00")))
          verify(mockReturnSubmissionRepository).set(any[ReturnSubmission])
          verify(mockObligationsRepository).markAsFulfilled(eqTo(vpdId), eqTo(periodKey), any[Instant])
        }
      }

      "return validation error when vapingProdManufactured is 1 but returns array is empty" in {
        val invalidRequest = validReturnRequest.copy(
          vapingProductsProduced = VapingProductsProduced(
            vapingProdManufactured = "1",
            returns = Seq.empty
          )
        )

        val result = service.processSubmission(vpdId, invalidRequest)

        whenReady(result) { response =>
          response.isLeft shouldBe true
          response.left.getOrElse("") should include("returns array is empty")
        }
      }

      "return validation error when vapingProdManufactured is 0 but returns array is not empty" in {
        val invalidRequest = validReturnRequest.copy(
          vapingProductsProduced = VapingProductsProduced(
            vapingProdManufactured = "0",
            returns = Seq(VapingReturn("641", BigDecimal("10.50"), BigDecimal("100"), BigDecimal("1050")))
          )
        )

        val result = service.processSubmission(vpdId, invalidRequest)

        whenReady(result) { response =>
          response.isLeft shouldBe true
          response.left.getOrElse("") should include("returns array is not empty")
        }
      }

      "append to existing financial data when it exists" in {
        when(mockUuidGenerator.uuid).thenReturn(submissionId)
        when(mockChargeReferenceGenerator.generate(any[BigDecimal])).thenReturn(Some(chargeReference))

        val expectedSubmission = ReturnSubmission(
          vpdId = vpdId,
          periodKey = periodKey,
          chargeReference = Some(chargeReference),
          submittedReturn = validReturnRequest,
          submittedAt = Instant.now(fixedClock),
          submissionId = submissionId
        )

        val existingFinancialData = FinancialDataState(
          vpdId = vpdId,
          noDataIdentified = false,
          documentDetails = Seq.empty,
          lastUpdated = Instant.now(fixedClock).minusSeconds(3600)
        )

        when(mockReturnSubmissionRepository.set(any[ReturnSubmission]))
          .thenReturn(Future.successful(expectedSubmission))
        when(mockObligationsRepository.markAsFulfilled(any[String], any[String], any[Instant]))
          .thenReturn(Future.successful(None))
        when(mockFinancialDataRepository.get(any[String]))
          .thenReturn(Future.successful(Some(existingFinancialData)))
        when(mockFinancialDataRepository.set(any[FinancialDataState]))
          .thenReturn(Future.successful(existingFinancialData))

        val result = service.processSubmission(vpdId, validReturnRequest)

        whenReady(result) { response =>
          response shouldBe Right(expectedSubmission)
          verify(mockFinancialDataRepository).get(eqTo(vpdId))
          verify(mockFinancialDataRepository).set(any[FinancialDataState])
        }
      }

      "not generate financial data for nil returns" in {
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

        when(mockUuidGenerator.uuid).thenReturn(submissionId)
        when(mockChargeReferenceGenerator.generate(any[BigDecimal])).thenReturn(None)

        val expectedSubmission = ReturnSubmission(
          vpdId = vpdId,
          periodKey = periodKey,
          chargeReference = None,
          submittedReturn = nilReturnRequest,
          submittedAt = Instant.now(fixedClock),
          submissionId = submissionId
        )

        when(mockReturnSubmissionRepository.set(any[ReturnSubmission]))
          .thenReturn(Future.successful(expectedSubmission))
        when(mockObligationsRepository.markAsFulfilled(any[String], any[String], any[Instant]))
          .thenReturn(Future.successful(None))

        val result = service.processSubmission(vpdId, nilReturnRequest)

        whenReady(result) { response =>
          response shouldBe Right(expectedSubmission)
        }
      }
    }
  }
}
