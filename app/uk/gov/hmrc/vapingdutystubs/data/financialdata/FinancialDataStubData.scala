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

package uk.gov.hmrc.vapingdutystubs.data.financialdata

import uk.gov.hmrc.vapingdutystubs.models.ReturnPeriod
import uk.gov.hmrc.vapingdutystubs.models.financialdata.{DocumentDetails, FinancialDataState, LineItemDetails}

import java.time.{Instant, LocalDate}

object FinancialDataStubData {

  // Matches Alcohol Duty's "Overpayment"/payment-on-account transaction code - the only one
  // relevant for VPD unallocated payments for now.
  private val unallocatedMainTransaction = "0060"
  private val returnMainTransaction = "4060"

  private def periodKeyFor(date: LocalDate): String = ReturnPeriod.fromDateInPeriod(date).toPeriodKey

  private def outstandingDocument(
    vpdId: String,
    chargeReference: String,
    periodStart: LocalDate,
    netDueDate: LocalDate,
    amount: BigDecimal
  ): DocumentDetails = {
    val periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth())
    DocumentDetails(
      documentNumber = s"1${chargeReference.takeRight(11)}",
      documentType = "TRM New Charge",
      chargeReferenceNumber = Some(chargeReference),
      businessPartnerNumber = s"BP$vpdId",
      contractAccountNumber = s"CA$vpdId",
      contractAccountCategory = "Excise",
      contractObjectNumber = s"CO$vpdId",
      contractObjectType = "ZVPD",
      postingDate = periodEnd,
      issueDate = periodEnd,
      documentTotalAmount = amount,
      documentClearedAmount = BigDecimal(0),
      documentOutstandingAmount = amount,
      lineItemDetails = Seq(LineItemDetails(
        itemNumber = "0001",
        subItemNumber = "001",
        mainTransaction = returnMainTransaction,
        subTransaction = "3392",
        chargeDescription = "VPD Return",
        periodFromDate = periodStart,
        periodToDate = periodEnd,
        periodKey = periodKeyFor(periodStart),
        netDueDate = netDueDate,
        formBundleNumber = s"FB$chargeReference",
        statisticalKey = "1",
        amount = amount
      ))
    )
  }

  private def clearedDocument(
    vpdId: String,
    chargeReference: String,
    periodStart: LocalDate,
    clearingDate: LocalDate,
    amount: BigDecimal
  ): DocumentDetails = {
    val periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth())
    DocumentDetails(
      documentNumber = s"2${chargeReference.takeRight(11)}",
      documentType = "TRM New Charge",
      chargeReferenceNumber = Some(chargeReference),
      businessPartnerNumber = s"BP$vpdId",
      contractAccountNumber = s"CA$vpdId",
      contractAccountCategory = "Excise",
      contractObjectNumber = s"CO$vpdId",
      contractObjectType = "ZVPD",
      postingDate = periodEnd,
      issueDate = periodEnd,
      documentTotalAmount = amount,
      documentClearedAmount = amount,
      documentOutstandingAmount = BigDecimal(0),
      lineItemDetails = Seq(LineItemDetails(
        itemNumber = "0001",
        subItemNumber = "001",
        mainTransaction = returnMainTransaction,
        subTransaction = "3392",
        chargeDescription = "VPD Return",
        periodFromDate = periodStart,
        periodToDate = periodEnd,
        periodKey = periodKeyFor(periodStart),
        netDueDate = periodEnd.plusDays(14),
        formBundleNumber = s"FB$chargeReference",
        statisticalKey = "1",
        amount = amount,
        clearingDate = Some(clearingDate),
        clearingReason = Some("01"),
        clearingDocument = Some(s"CD$chargeReference")
      ))
    )
  }

  private def unallocatedDocument(
    vpdId: String,
    documentNumber: String,
    postingDate: LocalDate,
    amount: BigDecimal
  ): DocumentDetails =
    DocumentDetails(
      documentNumber = documentNumber,
      documentType = "Payment on Account",
      chargeReferenceNumber = None,
      businessPartnerNumber = s"BP$vpdId",
      contractAccountNumber = s"CA$vpdId",
      contractAccountCategory = "Excise",
      contractObjectNumber = s"CO$vpdId",
      contractObjectType = "ZVPD",
      postingDate = postingDate,
      issueDate = postingDate,
      documentTotalAmount = amount,
      documentClearedAmount = BigDecimal(0),
      documentOutstandingAmount = amount,
      lineItemDetails = Seq(LineItemDetails(
        itemNumber = "0001",
        subItemNumber = "001",
        mainTransaction = unallocatedMainTransaction,
        subTransaction = "0000",
        chargeDescription = "Payment on Account",
        periodFromDate = postingDate,
        periodToDate = postingDate,
        periodKey = periodKeyFor(postingDate),
        netDueDate = postingDate,
        formBundleNumber = s"FB$documentNumber",
        statisticalKey = "1",
        amount = amount
      ))
    )

  def outstandingOnly(vpdId: String): FinancialDataState = {
    val today = LocalDate.now()
    FinancialDataState(
      vpdId = vpdId,
      noDataIdentified = false,
      documentDetails = Seq(
        outstandingDocument(vpdId, "XMVPD0000000001", today.minusMonths(1), today.plusDays(14), BigDecimal("500.00")),
        outstandingDocument(vpdId, "XMVPD0000000002", today.minusMonths(2), today.minusDays(10), BigDecimal("250.00"))
      ),
      lastUpdated = Instant.now()
    )
  }

  def withUnallocated(vpdId: String): FinancialDataState = {
    val today = LocalDate.now()
    FinancialDataState(
      vpdId = vpdId,
      noDataIdentified = false,
      documentDetails = Seq(
        outstandingDocument(vpdId, "XMVPD0000000003", today.minusMonths(1), today.plusDays(14), BigDecimal("500.00")),
        unallocatedDocument(vpdId, "3000000000001", today.minusDays(5), BigDecimal("150.00"))
      ),
      lastUpdated = Instant.now()
    )
  }

  def clearedOnly(vpdId: String): FinancialDataState = {
    val today = LocalDate.now()
    FinancialDataState(
      vpdId = vpdId,
      noDataIdentified = false,
      documentDetails = Seq(
        clearedDocument(vpdId, "XMVPD0000000004", today.minusMonths(2), today.minusMonths(1).plusDays(9), BigDecimal("750.00"))
      ),
      lastUpdated = Instant.now()
    )
  }

  def mixed(vpdId: String): FinancialDataState = {
    val today = LocalDate.now()
    FinancialDataState(
      vpdId = vpdId,
      noDataIdentified = false,
      documentDetails = Seq(
        outstandingDocument(vpdId, "XMVPD0000000005", today.minusMonths(1), today.plusDays(14), BigDecimal("500.00")),
        unallocatedDocument(vpdId, "3000000000002", today.minusDays(5), BigDecimal("150.00")),
        clearedDocument(vpdId, "XMVPD0000000006", today.minusMonths(2), today.minusMonths(1).plusDays(9), BigDecimal("750.00"))
      ),
      lastUpdated = Instant.now()
    )
  }

  // Reproduces the real ETMP 422/code-018 "No Data Identified" business response - used both
  // as an explicit test-only scenario and to model any of the AC's empty-section cases.
  def noData(vpdId: String): FinancialDataState =
    FinancialDataState(
      vpdId = vpdId,
      noDataIdentified = true,
      documentDetails = Seq.empty,
      lastUpdated = Instant.now()
    )

  def defaultData(vpdId: String): FinancialDataState = mixed(vpdId)
}
