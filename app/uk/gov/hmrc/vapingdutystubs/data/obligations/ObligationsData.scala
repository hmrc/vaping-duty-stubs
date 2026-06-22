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

import uk.gov.hmrc.vapingdutystubs.models.obligations.{ObligationDetails, ObligationItem, ObligationState, ObligationsResponse}
import uk.gov.hmrc.vapingdutystubs.models.ReturnPeriod

import java.time.LocalDate

object ObligationsData {

  private val STATUS_OPEN = "O"
  private val STATUS_FULFILLED = "F"
  private val MONTHS_TO_GENERATE = 36
  private val DUE_DATE_DAY = 7

  private def createObligation(
    status: String,
    fromDate: LocalDate,
    toDate: LocalDate,
    dueDate: LocalDate,
    periodKey: String,
    receivedDate: Option[LocalDate] = None
  ): ObligationItem =
    ObligationItem(
      identification = None,
      obligationDetails = ObligationDetails(
        openOrFulfilledStatus = status,
        iCFromDate = fromDate,
        iCToDate = toDate,
        iCDateReceived = receivedDate,
        iCDueDate = dueDate,
        periodKey = periodKey
      )
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

    val obligations = (0 until MONTHS_TO_GENERATE).map { monthsBack =>
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
          createObligation(
            status = STATUS_OPEN,
            fromDate = periodStart,
            toDate = periodEnd,
            dueDate = dueDate,
            periodKey = periodKey
          )

        // Previous month - Due (not yet submitted, but not overdue)
        case 1 =>
          createObligation(
            status = STATUS_OPEN,
            fromDate = periodStart,
            toDate = periodEnd,
            dueDate = dueDate,
            periodKey = periodKey
          )

        // 2 months ago - Overdue (past due date)
        case 2 =>
          createObligation(
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
          createObligation(
            status = STATUS_FULFILLED,
            fromDate = periodStart,
            toDate = periodEnd,
            dueDate = dueDate,
            periodKey = periodKey,
            receivedDate = Some(receivedDate)
          )
      }
    }.toSeq.reverse // Reverse to get chronological order (oldest first)

    ObligationState(
      vpdId = vpdId,
      obligations = obligations
    )
  }

  def sampleObligations(vpdId: String): ObligationState = {

    ObligationState(
      vpdId = vpdId,
      obligations = Seq(
        // Outstanding return - Due soon
        createObligation(
          status = "O",
          fromDate = LocalDate.of(2027, 12, 1),
          toDate = LocalDate.of(2027, 12, 31),
          dueDate = LocalDate.of(2028, 1, 7),
          periodKey = "27AL"
        ),
        // Outstanding return - Overdue
        createObligation(
          status = "O",
          fromDate = LocalDate.of(2027, 11, 1),
          toDate = LocalDate.of(2027, 11, 30),
          dueDate = LocalDate.of(2027, 12, 7),
          periodKey = "27AK"
        ),
        // Fulfilled return
        createObligation(
          status = "F",
          fromDate = LocalDate.of(2027, 10, 1),
          toDate = LocalDate.of(2027, 10, 31),
          dueDate = LocalDate.of(2027, 11, 7),
          periodKey = "27AJ",
          receivedDate = Some(LocalDate.of(2027, 11, 15))
        )
      )
    )
  }

  val sampleVpdIds: Seq[String] = Seq(
    "GBWK0000001WK",
    "GBWK0000002WK",
    "GBWK0000003WK"
  )

  def allSampleObligations: Seq[ObligationState] =
    sampleVpdIds.map(sampleObligations)

  def onlyOpenReturns(vpdId: String): ObligationState = {
    val currentDate = LocalDate.now()

    ObligationState(
      vpdId = vpdId,
      obligations = Seq(
        // Open return - Due in 10 days
        createObligation(
          status = "O",
          fromDate = LocalDate.of(2027, 12, 1),
          toDate = LocalDate.of(2027, 12, 31),
          dueDate = currentDate.plusDays(10),
          periodKey = "27AL"
        ),
        // Open return - Overdue by 5 days
        createObligation(
          status = "O",
          fromDate = LocalDate.of(2027, 11, 1),
          toDate = LocalDate.of(2027, 11, 30),
          dueDate = currentDate.minusDays(5),
          periodKey = "27AK"
        ),
        // Open return - Due in 30 days
        createObligation(
          status = "O",
          fromDate = LocalDate.of(2028, 1, 1),
          toDate = LocalDate.of(2028, 1, 31),
          dueDate = currentDate.plusDays(30),
          periodKey = "28AA"
        )
      )
    )
  }

  def onlyCompletedReturns(vpdId: String): ObligationState = {
    val currentDate = LocalDate.now()

    ObligationState(
      vpdId = vpdId,
      obligations = Seq(
        // Completed return 1
        createObligation(
          status = "F",
          fromDate = LocalDate.of(2027, 10, 1),
          toDate = LocalDate.of(2027, 10, 31),
          dueDate = LocalDate.of(2027, 11, 30),
          periodKey = "27AJ",
          receivedDate = Some(LocalDate.of(2027, 11, 15))
        ),
        // Completed return 2
        createObligation(
          status = "F",
          fromDate = LocalDate.of(2027, 9, 1),
          toDate = LocalDate.of(2027, 9, 30),
          dueDate = LocalDate.of(2027, 10, 31),
          periodKey = "27AI",
          receivedDate = Some(LocalDate.of(2027, 10, 20))
        ),
        // Completed return 3
        createObligation(
          status = "F",
          fromDate = LocalDate.of(2027, 8, 1),
          toDate = LocalDate.of(2027, 8, 31),
          dueDate = LocalDate.of(2027, 9, 30),
          periodKey = "27AH",
          receivedDate = Some(LocalDate.of(2027, 9, 25))
        )
      )
    )
  }

  def noObligations(vpdId: String): ObligationState =
    ObligationState(
      vpdId = vpdId,
      obligations = Seq.empty
    )
    sampleVpdIds.map(generate36MonthsObligations)
}
