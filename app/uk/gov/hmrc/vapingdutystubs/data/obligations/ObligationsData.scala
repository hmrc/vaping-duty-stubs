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
   * Generates 36 months of obligations from the previous month going back 35 months.
   * Distribution:
   * - 33 obligations: Fulfilled (completed on time)
   * - 3 obligations: Open (previous month, 2 months ago, 3 months ago - may be overdue based on due date)
   * 
   * Note: Current month obligation does not exist yet - it only appears from the 1st of next month.
   */
  def generate36MonthsObligations(vpdId: String): ObligationState = {
    val today = LocalDate.now()
    val previousMonthStart = LocalDate.of(today.getYear, today.getMonthValue, 1).minusMonths(1)

    val obligationDetails = (0 until MONTHS_TO_GENERATE).map { monthsBack =>
      // Calculate the year and month for this obligation
      val targetDate = previousMonthStart.minusMonths(monthsBack)
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
        // Previous month - Open (may be overdue if today > due date)
        case 0 =>
          createObligationDetails(
            status = STATUS_OPEN,
            fromDate = periodStart,
            toDate = periodEnd,
            dueDate = dueDate,
            periodKey = periodKey
          )

        // 2 months ago - Open (overdue)
        case 1 =>
          createObligationDetails(
            status = STATUS_OPEN,
            fromDate = periodStart,
            toDate = periodEnd,
            dueDate = dueDate,
            periodKey = periodKey
          )

        // 3 months ago - Open (overdue)
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
   * Starts from previous month (current month obligation does not exist yet).
   */
  def generate36MonthsAllFulfilled(vpdId: String): ObligationState = {
    val today = LocalDate.now()
    val previousMonthStart = LocalDate.of(today.getYear, today.getMonthValue, 1).minusMonths(1)

    val obligationDetails = (0 until MONTHS_TO_GENERATE).map { monthsBack =>
      // Calculate the year and month for this obligation
      val targetDate = previousMonthStart.minusMonths(monthsBack)
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

    val obligationDetails = Seq(
      // Open return - Previous month (may be overdue if today > due date)
      createObligationDetails(
        status = STATUS_OPEN,
        fromDate = previousMonth,
        toDate = previousMonth.withDayOfMonth(previousMonth.lengthOfMonth()),
        dueDate = previousMonth.plusMonths(1).withDayOfMonth(DUE_DATE_DAY),
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

  def simulatedError(vpdId: String): ObligationState = {
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
      obligations = Seq(obligationItem),
      simulateError = true
    )
  }

  def all36MonthsObligations: Seq[ObligationState] =
    sampleVpdIds.map(generate36MonthsObligations)

  /**
   * - 1 open obligation with due date in the future (not overdue)
   * - No completed returns
   */
  def singleDue(vpdId: String): ObligationState = {
    val today = LocalDate.now()
    val currentMonthStart = LocalDate.of(today.getYear, today.getMonthValue, 1)

    val periodStart = currentMonthStart.minusMonths(1)
    val periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth())
    val dueDate = periodEnd
    val returnPeriod = ReturnPeriod.fromDateInPeriod(periodStart)
    val periodKey = returnPeriod.toPeriodKey

    val obligationDetails = Seq(
      createObligationDetails(
        status = STATUS_OPEN,
        fromDate = periodStart,
        toDate = periodEnd,
        dueDate = dueDate,
        periodKey = periodKey
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

  /**
   * - 1 open obligation with due date in the future (not overdue)
   * - 3 fulfilled obligations (completed returns in the past)
   */
  def singleDueWithCompleted(vpdId: String): ObligationState = {
    val today = LocalDate.now()
    val currentMonthStart = LocalDate.of(today.getYear, today.getMonthValue, 1)

    val obligationDetails = (0 until 4).map { monthsBack =>
      val targetDate = currentMonthStart.minusMonths(monthsBack)
      val periodStart = LocalDate.of(targetDate.getYear, targetDate.getMonthValue, 1)
      val periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth())
      val dueDate = periodStart.plusMonths(1).withDayOfMonth(DUE_DATE_DAY)
      val returnPeriod = ReturnPeriod.fromDateInPeriod(periodStart)
      val periodKey = returnPeriod.toPeriodKey

      if (monthsBack == 0) {
        // Most recent month - open (due)
        createObligationDetails(
          status = STATUS_OPEN,
          fromDate = periodStart,
          toDate = periodEnd,
          dueDate = periodEnd,
          periodKey = periodKey
        )
      } else {
        // Older months - fulfilled
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
    }.reverse

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
   * - 1 open obligation with due date in the future (not overdue)
   * - 1 open obligation that is overdue (due date in the past)
   * - 3 fulfilled obligations (completed returns in the past)
   */
  def singleDueOneOverdue(vpdId: String): ObligationState = {
    val today = LocalDate.now()
    val currentMonthStart = LocalDate.of(today.getYear, today.getMonthValue, 1)

    val obligationDetails = (0 until 5).map { monthsBack =>
      val targetDate = currentMonthStart.minusMonths(monthsBack)
      val periodStart = LocalDate.of(targetDate.getYear, targetDate.getMonthValue, 1)
      val periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth())
      val dueDate = periodStart.plusMonths(1).withDayOfMonth(DUE_DATE_DAY)
      val returnPeriod = ReturnPeriod.fromDateInPeriod(periodStart)
      val periodKey = returnPeriod.toPeriodKey

      monthsBack match {
        case 0 =>
          // Most recent month - open (due)
          createObligationDetails(
            status = STATUS_OPEN,
            fromDate = periodStart,
            toDate = periodEnd,
            dueDate = periodEnd,
            periodKey = periodKey
          )
        case 1 =>
          // 2 months ago - open (overdue)
          createObligationDetails(
            status = STATUS_OPEN,
            fromDate = periodStart,
            toDate = periodEnd,
            dueDate = dueDate,
            periodKey = periodKey
          )
        case _ =>
          // Older months - fulfilled
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
    }.reverse

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
   * - 1 open obligation with due date in the future (not overdue)
   * - 3 open obligations that are overdue (due dates in the past)
   * - 3 fulfilled obligations (completed returns in the past)
   */
  def singleDueMultipleOverdue(vpdId: String): ObligationState = {
    val today = LocalDate.now()
    val currentMonthStart = LocalDate.of(today.getYear, today.getMonthValue, 1)

    val obligationDetails = (0 until 7).map { monthsBack =>
      val targetDate = currentMonthStart.minusMonths(monthsBack)
      val periodStart = LocalDate.of(targetDate.getYear, targetDate.getMonthValue, 1)
      val periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth())
      val dueDate = periodStart.plusMonths(1).withDayOfMonth(DUE_DATE_DAY)
      val returnPeriod = ReturnPeriod.fromDateInPeriod(periodStart)
      val periodKey = returnPeriod.toPeriodKey

      monthsBack match {
        case 0 =>
          // Most recent month - open (due)
          createObligationDetails(
            status = STATUS_OPEN,
            fromDate = periodStart,
            toDate = periodEnd,
            dueDate = periodEnd,
            periodKey = periodKey
          )
        case 1 | 2 | 3 =>
          // 2-4 months ago - open (overdue)
          createObligationDetails(
            status = STATUS_OPEN,
            fromDate = periodStart,
            toDate = periodEnd,
            dueDate = dueDate,
            periodKey = periodKey
          )
        case _ =>
          // Older months - fulfilled
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
    }.reverse

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
}
