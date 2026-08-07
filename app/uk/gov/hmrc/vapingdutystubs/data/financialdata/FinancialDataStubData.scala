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
import uk.gov.hmrc.vapingdutystubs.models.financialdata.{DocumentDetails, FinancialDataState, LineItemDetails, RegimeTotalisation, Totalisation}

import java.time.{Instant, LocalDate}

object FinancialDataStubData {

  // Matches Alcohol Duty's "Overpayment"/payment-on-account transaction code - the only one
  // relevant for VPD unallocated payments for now.
  private val unallocatedMainTransaction = "0060"
  private val returnMainTransaction = "4060"
  private val interestMainTransaction = "4061"

  def calculateTotalisation(documentDetails: Seq[DocumentDetails]): Option[Totalisation] = {
    if (documentDetails.isEmpty) {
      None
    } else {
      val today = LocalDate.now()
      
      val (overdue, notYetDue, credit) = documentDetails.foldLeft((BigDecimal(0), BigDecimal(0), BigDecimal(0))) {
        case ((overdueAcc, notYetDueAcc, creditAcc), doc) =>
          val outstanding = doc.documentOutstandingAmount
          
          // Credit amounts are negative outstanding amounts or unallocated payments
          if (outstanding < 0 || doc.documentType == "Payment on Account") {
            (overdueAcc, notYetDueAcc, creditAcc + outstanding.abs)
          } else if (outstanding > 0) {
            // Check if overdue based on line item net due dates
            val isOverdue = doc.lineItemDetails.exists { lineItem =>
              lineItem.netDueDate.isBefore(today)
            }
            
            if (isOverdue) {
              (overdueAcc + outstanding, notYetDueAcc, creditAcc)
            } else {
              (overdueAcc, notYetDueAcc + outstanding, creditAcc)
            }
          } else {
            (overdueAcc, notYetDueAcc, creditAcc)
          }
      }
      
      val balance = overdue + notYetDue - credit
      
      Some(Totalisation(
        regimeTotalisation = Some(RegimeTotalisation(
          totalAccountOverdue = if (overdue > 0) Some(overdue) else None,
          totalAccountNotYetDue = if (notYetDue > 0) Some(notYetDue) else None,
          totalAccountCredit = if (credit > 0) Some(credit) else None,
          totalAccountBalance = Some(balance)
        ))
      ))
    }
  }

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
        netDueDate = periodEnd.plusMonths(1).withDayOfMonth(15),
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
        netDueDate = postingDate.withDayOfMonth(15),
        formBundleNumber = s"FB$documentNumber",
        statisticalKey = "1",
        amount = amount
      ))
    )

  // Interest is charged on an already-overdue payment, so netDueDate is expected to be in the past
  // (matching how a real interest document is only ever raised against an overdue charge).
  private def interestDocument(
    vpdId: String,
    chargeReference: String,
    netDueDate: LocalDate,
    amount: BigDecimal
  ): DocumentDetails =
    DocumentDetails(
      documentNumber = s"4${chargeReference.takeRight(11)}",
      documentType = "Interest Document",
      chargeReferenceNumber = Some(chargeReference),
      businessPartnerNumber = s"BP$vpdId",
      contractAccountNumber = s"CA$vpdId",
      contractAccountCategory = "Excise",
      contractObjectNumber = s"CO$vpdId",
      contractObjectType = "ZVPD",
      postingDate = netDueDate,
      issueDate = netDueDate,
      documentTotalAmount = amount,
      documentClearedAmount = BigDecimal(0),
      documentOutstandingAmount = amount,
      lineItemDetails = Seq(LineItemDetails(
        itemNumber = "0001",
        subItemNumber = "000",
        mainTransaction = interestMainTransaction,
        subTransaction = "3392",
        chargeDescription = "Vaping Duty Return Interest",
        periodFromDate = netDueDate,
        periodToDate = netDueDate,
        periodKey = periodKeyFor(netDueDate),
        netDueDate = netDueDate,
        formBundleNumber = s"FB$chargeReference",
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
        outstandingDocument(vpdId, "XMVPD0000000001", today.minusMonths(1), today.plusMonths(1).withDayOfMonth(15), BigDecimal("500.00")),
        outstandingDocument(vpdId, "XMVPD0000000002", today.minusMonths(2), today.minusMonths(1).withDayOfMonth(15), BigDecimal("250.00"))
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
        outstandingDocument(vpdId, "XMVPD0000000003", today.minusMonths(1), today.plusMonths(1).withDayOfMonth(15), BigDecimal("500.00")),
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
        outstandingDocument(vpdId, "XMVPD0000000005", today.minusMonths(1), today.plusMonths(1).withDayOfMonth(15), BigDecimal("500.00")),
        unallocatedDocument(vpdId, "3000000000002", today.minusDays(5), BigDecimal("150.00")),
        clearedDocument(vpdId, "XMVPD0000000006", today.minusMonths(2), today.minusMonths(1).plusDays(9), BigDecimal("750.00"))
      ),
      lastUpdated = Instant.now()
    )
  }

  // An overdue original charge plus its late payment interest, raised as a separate document with
  // its own charge reference and mainTransaction 4061 - proves the "Late payment interest" heading
  // renders for the interest row while the original charge keeps the normal payment heading.
  def interestPayment(vpdId: String): FinancialDataState = {
    val today = LocalDate.now()
    FinancialDataState(
      vpdId = vpdId,
      noDataIdentified = false,
      documentDetails = Seq(
        outstandingDocument(vpdId, "XMVPD0000000007", today.minusMonths(3), today.minusMonths(2).withDayOfMonth(15), BigDecimal("10000.00")),
        interestDocument(vpdId, "XIVPD0000000001", today.minusMonths(1).withDayOfMonth(15), BigDecimal("200.00"))
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

  // BTA summary tile scenarios - a single outstanding charge, not yet due.
  def singleOutstanding(vpdId: String): FinancialDataState = {
    val today = LocalDate.now()
    FinancialDataState(
      vpdId = vpdId,
      noDataIdentified = false,
      documentDetails = Seq(
        outstandingDocument(vpdId, "XMVPD0000000010", today.minusMonths(1), today.plusMonths(1).withDayOfMonth(15), BigDecimal("4574.84"))
      ),
      lastUpdated = Instant.now()
    )
  }

  // BTA summary tile scenario - a single outstanding charge whose due date has already passed.
  // The Payments schema itself has no separate "overdue" flag - this scenario exists to prove the
  // aggregation logic correctly includes overdue amounts in the totalised balance.
  def overdueBalance(vpdId: String): FinancialDataState = {
    val today = LocalDate.now()
    FinancialDataState(
      vpdId = vpdId,
      noDataIdentified = false,
      documentDetails = Seq(
        outstandingDocument(vpdId, "XMVPD0000000011", today.minusMonths(3), today.minusMonths(2).withDayOfMonth(15), BigDecimal("1200.00"))
      ),
      lastUpdated = Instant.now()
    )
  }

  // BTA summary tile scenario - only an unallocated payment on account, no outstanding charges,
  // so calculateTotalisation nets out to a negative (credit) balance.
  def creditBalance(vpdId: String): FinancialDataState = {
    val today = LocalDate.now()
    FinancialDataState(
      vpdId = vpdId,
      noDataIdentified = false,
      documentDetails = Seq(
        unallocatedDocument(vpdId, "3000000000099", today.minusDays(5), BigDecimal("325.50"))
      ),
      lastUpdated = Instant.now()
    )
  }

  // BTA summary tile scenario - nothing owed. Reuses clearedOnly, which already nets to a zero balance.
  def nothingOwed(vpdId: String): FinancialDataState = clearedOnly(vpdId)

  // Fixed VPD IDs for the BTA summary tile payments scenarios, seeded at startup (see StartupSeeder).
  // Digits 1-5 and 8 are reserved by the returns/obligations error-simulation and sample-obligations
  // sets (see README.md), so these use the remaining unused digits.
  val singleOutstandingVpdId = "GBWK0900906WK"
  val overdueBalanceVpdId    = "GBWK0900907WK"
  val creditBalanceVpdId     = "GBWK0900909WK"
  val nothingOwedVpdId       = "GBWK0900900WK"

  def sampleFinancialDataScenarios: Seq[FinancialDataState] = Seq(
    singleOutstanding(singleOutstandingVpdId),
    overdueBalance(overdueBalanceVpdId),
    creditBalance(creditBalanceVpdId),
    nothingOwed(nothingOwedVpdId)
  )
}