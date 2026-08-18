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

package uk.gov.hmrc.vapingdutystubs.data.obligations

import uk.gov.hmrc.vapingdutystubs.models.ReturnPeriod
import uk.gov.hmrc.vapingdutystubs.models.obligations.{Identification, ObligationDetails, ObligationItem, ObligationState}

import java.time.LocalDate

object ObligationsData {

  private val STATUS_OPEN = "O"
  private val STATUS_FULFILLED = "F"
  private val MONTHS_TO_GENERATE = 36
  private val DUE_DATE_DAY = 7

  private val REFERENCE_TYPE_VPD = "ZVPD"

  private def createObligationDetails(
    status: String,
    fromDate: LocalDate,
    toDate: LocalDate,
    dueDate: LocalDate,
    periodKey: String,
    receivedDate: Option[LocalDate] = None
  ): ObligationDetails =
    ObligationDetails(
      openOrFulfilledStatus = status,
      iCFromDate = fromDate,
      iCToDate = toDate,
      iCDateReceived = receivedDate,
      iCDueDate = dueDate,
      periodKey = periodKey
    )

  /**
   * Generates 36 months of obligations from the current month going back 35 months.
   * Distribution:
   * - 33 obligations: Fulfilled (completed on time)
   * - 1 obligation: Due (previous month - not yet submitted, not overdue)
   * - 1 obligation: Overdue (2 months ago - past due date)
   * - 1 obligation: Open (current month - current period)
   */
  def generate36MonthsObligations(vpdId: String): ObligationState = {
    val today = LocalDate.now()
    val currentMonthStart = LocalDate.of(today.getYear, today.getMonthValue, 1)

    val obligationDetails = (0 until MONTHS_TO_GENERATE).map { monthsBack =>
      // Calculate the year and month for this obligation
      val targetDate = currentMonthStart.minusMonths(monthsBack)
      val year = targetDate.getYear
      val month = targetDate.getMonthValue

      // Create proper period dates
      val periodStart = LocalDate.of(year, month, 1)
      val periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth())
      val dueDate = periodStart.plusMonths(1).withDayOfMonth(DUE_DATE_DAY)

      // Generate period key from the period start date
      val returnPeriod = ReturnPeriod.fromDateInPeriod(periodStart)
      val periodKey = returnPeriod.toPeriodKey

      monthsBack match {
        // Current month - Current period (Open, not overdue)
        case 0 =>
          createObligationDetails(
            status = STATUS_OPEN,
            fromDate = periodStart,
            toDate = periodEnd,
            dueDate = dueDate,
            periodKey = periodKey
          )

        // Previous month - Due (not yet submitted, but not overdue)
        case 1 =>
          createObligationDetails(
            status = STATUS_OPEN,
            fromDate = periodStart,
            toDate = periodEnd,
            dueDate = dueDate,
            periodKey = periodKey
          )

        // 2 months ago - Overdue (past due date)
        case 2 =>
          createObligationDetails(
            status = STATUS_OPEN,
            fromDate = periodStart,
            toDate = periodEnd,
            dueDate = dueDate,
            periodKey = periodKey
          )

        // All other months (33 obligations) - Fulfilled
        case _ =>
          // Received date is a few days before the due date
          val receivedDate = dueDate.minusDays(5)
          createObligationDetails(
            status = STATUS_FULFILLED,
            fromDate = periodStart,
            toDate = periodEnd,
            dueDate = dueDate,
            periodKey = periodKey,
            receivedDate = Some(receivedDate)
          )
      }
    }.toSeq.reverse // Reverse to get chronological order (oldest first)

    val obligationItem = ObligationItem(
      identification = Identification(
        referenceType = REFERENCE_TYPE_VPD,
        referenceNumber = vpdId,
        incomeSourceType = None
      ),
      obligationDetails = obligationDetails
    )

    ObligationState(
      vpdId = vpdId,
      obligations = Seq(obligationItem)
    )
  }

  /**
   * Generates 36 months of obligations with ALL fulfilled (no open obligations).
   * All 36 obligations are marked as fulfilled with received dates.
   */
  def generate36MonthsAllFulfilled(vpdId: String): ObligationState = {
    val today = LocalDate.now()
    val currentMonthStart = LocalDate.of(today.getYear, today.getMonthValue, 1)

    val obligationDetails = (0 until MONTHS_TO_GENERATE).map { monthsBack =>
      // Calculate the year and month for this obligation
      val targetDate = currentMonthStart.minusMonths(monthsBack)
      val year = targetDate.getYear
      val month = targetDate.getMonthValue

      // Create proper period dates
      val periodStart = LocalDate.of(year, month, 1)
      val periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth())
      val dueDate = periodStart.plusMonths(1).withDayOfMonth(DUE_DATE_DAY)

      // Generate period key from the period start date
      val returnPeriod = ReturnPeriod.fromDateInPeriod(periodStart)
      val periodKey = returnPeriod.toPeriodKey

      // All obligations are fulfilled with received date 5 days before due date
      val receivedDate = dueDate.minusDays(5)
      createObligationDetails(
        status = STATUS_FULFILLED,
        fromDate = periodStart,
        toDate = periodEnd,
        dueDate = dueDate,
        periodKey = periodKey,
        receivedDate = Some(receivedDate)
      )
    }.toSeq.reverse // Reverse to get chronological order (oldest first)

    val obligationItem = ObligationItem(
      identification = Identification(
        referenceType = REFERENCE_TYPE_VPD,
        referenceNumber = vpdId,
        incomeSourceType = None
      ),
      obligationDetails = obligationDetails
    )

    ObligationState(
      vpdId = vpdId,
      obligations = Seq(obligationItem)
    )
  }

  val sampleVpdIds: Seq[String] = Seq(
    "GBWK0000001WK",
    "GBWK0000002WK",
    "GBWK0000003WK"
  )

  def onlyOpenReturns(vpdId: String): ObligationState = {
    val currentDate = LocalDate.now()
    val currentMonthStart = LocalDate.of(currentDate.getYear, currentDate.getMonthValue, 1)

    // Calculate period keys dynamically based on current date
    val previousMonth = currentMonthStart.minusMonths(1)
    val twoMonthsAgo = currentMonthStart.minusMonths(2)
    val threeMonthsAgo = currentMonthStart.minusMonths(3)

    val duePeriodKey = ReturnPeriod.fromDateInPeriod(previousMonth).toPeriodKey
    val overdue1PeriodKey = ReturnPeriod.fromDateInPeriod(twoMonthsAgo).toPeriodKey
    val overdue2PeriodKey = ReturnPeriod.fromDateInPeriod(threeMonthsAgo).toPeriodKey

    // For the "due" obligation, set due date to 5 days in the future to ensure it's not overdue
    val dueDateInFuture = currentDate.plusDays(5)

    val obligationDetails = Seq(
      // Open return - Due (not yet overdue - due date is in the future)
      createObligationDetails(
        status = STATUS_OPEN,
        fromDate = previousMonth,
        toDate = previousMonth.withDayOfMonth(previousMonth.lengthOfMonth()),
        dueDate = dueDateInFuture,
        periodKey = duePeriodKey
      ),
      // Open return - Overdue (2 months ago)
      createObligationDetails(
        status = STATUS_OPEN,
        fromDate = twoMonthsAgo,
        toDate = twoMonthsAgo.withDayOfMonth(twoMonthsAgo.lengthOfMonth()),
        dueDate = twoMonthsAgo.plusMonths(1).withDayOfMonth(DUE_DATE_DAY),
        periodKey = overdue1PeriodKey
      ),
      // Open return - Overdue (3 months ago)
      createObligationDetails(
        status = STATUS_OPEN,
        fromDate = threeMonthsAgo,
        toDate = threeMonthsAgo.withDayOfMonth(threeMonthsAgo.lengthOfMonth()),
        dueDate = threeMonthsAgo.plusMonths(1).withDayOfMonth(DUE_DATE_DAY),
        periodKey = overdue2PeriodKey
      )
    )

    val obligationItem = ObligationItem(
      identification = Identification(
        referenceType = REFERENCE_TYPE_VPD,
        referenceNumber = vpdId,
        incomeSourceType = None
      ),
      obligationDetails = obligationDetails
    )

    ObligationState(
      vpdId = vpdId,
      obligations = Seq(obligationItem)
    )
  }


  def noObligations(vpdId: String): ObligationState = {
    val obligationItem = ObligationItem(
      identification = Identification(
        referenceType = REFERENCE_TYPE_VPD,
        referenceNumber = vpdId,
        incomeSourceType = None
      ),
      obligationDetails = Seq.empty
    )

    ObligationState(
      vpdId = vpdId,
      obligations = Seq(obligationItem)
    )
  }

  def all36MonthsObligations: Seq[ObligationState] =
    sampleVpdIds.map(generate36MonthsObligations)
}
