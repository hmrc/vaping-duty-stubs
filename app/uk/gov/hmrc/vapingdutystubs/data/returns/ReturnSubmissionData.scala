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

package uk.gov.hmrc.vapingdutystubs.data.returns

import uk.gov.hmrc.vapingdutystubs.models.ReturnPeriod
import uk.gov.hmrc.vapingdutystubs.models.returns.*
import uk.gov.hmrc.vapingdutystubs.models.returns.submit.ReturnCreateRequest

import java.time.{Instant, YearMonth, ZoneId}
import scala.util.Random
import java.time.{Instant, LocalDate, ZoneOffset}

object ReturnSubmissionData {

  private val random = new Random()
  private val FULFILLED_OBLIGATIONS_COUNT = 33
  private val DUE_DATE_DAY = 7

  private val sampleRegularReturn = RegularReturn(
    taxType = "301",
    dutyRate = BigDecimal("0.05"),
    amountProducedLiquid = BigDecimal("1000.50"),
    dutyDue = BigDecimal("50.03")
  )

  private def generateVapingReturn(): VapingReturn = {
    VapingReturn(
      taxType = "641",
      dutyRate = BigDecimal("10.50"),
      amountProducedLiquid = BigDecimal(1000 + random.nextInt(5000)) + BigDecimal(random.nextInt(100)) / 100,
      dutyDue = BigDecimal(10000 + random.nextInt(50000)) + BigDecimal(random.nextInt(100)) / 100
    )
  }

  private def generateDeclaration(): DeclarationDetails = {
    DeclarationDetails(
      fullName = "John Smith",
      capacityInWhichSigned = "Director",
      signeesEmailAddress = "john.smith@example.com"
    )
  }

  private def generateTotalDutyDue(isNil: Boolean): TotalDutyDue = {
    if (isNil) {
      TotalDutyDue(
        totalDutyDueVapingProducts = BigDecimal("0.00"),
        totalDutyOverDeclaration = BigDecimal("0.00"),
        totalDutyUnderDeclaration = BigDecimal("0.00"),
        totalDutySpoiltProduct = BigDecimal("0.00"),
        adjustmentAmount = BigDecimal("0.00"),
        totalDue = BigDecimal("0.00")
      )
    } else {
      val vapingProductsDuty = BigDecimal(10000 + random.nextInt(50000)) + BigDecimal(random.nextInt(100)) / 100
      TotalDutyDue(
        totalDutyDueVapingProducts = vapingProductsDuty,
        totalDutyOverDeclaration = BigDecimal("0.00"),
        totalDutyUnderDeclaration = BigDecimal("0.00"),
        totalDutySpoiltProduct = BigDecimal("0.00"),
        adjustmentAmount = BigDecimal("0.00"),
        totalDue = vapingProductsDuty
      )
    }
  }

  private def generateNilReturnRequest(periodKey: String): ReturnCreateRequest = {
    ReturnCreateRequest(
      periodKey = periodKey,
      vapingProductsProduced = VapingProductsProduced(
        vapingProdManufactured = "0",
        returns = Seq.empty
      ),
      overDeclaration = None,
      underDeclaration = None,
      spoiltProduct = None,
      totalDutyDue = generateTotalDutyDue(isNil = true),
      otherOptions = None,
      declaration = generateDeclaration()
    )
  }

  private def generateRegularReturnRequest(periodKey: String): ReturnCreateRequest = {
    val returns = Seq(generateVapingReturn())
    ReturnCreateRequest(
      periodKey = periodKey,
      vapingProductsProduced = VapingProductsProduced(
        vapingProdManufactured = "1",
        returns = returns
      ),
      overDeclaration = None,
      underDeclaration = None,
      spoiltProduct = None,
      totalDutyDue = generateTotalDutyDue(isNil = false),
      otherOptions = None,
      declaration = generateDeclaration()
    )
  }

  def generate33ReturnSubmissions(vpdId: String): Seq[ReturnSubmission] = {
    val currentYear = Instant.now().atZone(ZoneId.systemDefault()).getYear
    val startYear = currentYear - 2

    (0 until 33).map { index =>
      val year = startYear + (index / 12)
      val month = (index % 12) + 1
      val yearMonth = YearMonth.of(year, month)
      val periodKey = ReturnPeriod(yearMonth).toPeriodKey

      val isNil = random.nextBoolean()
      val returnRequest = if (isNil) {
        generateNilReturnRequest(periodKey)
      } else {
        generateRegularReturnRequest(periodKey)
      }

      val submissionId = f"${100000000000L + random.nextInt(900000000)}%012d"
      val chargeReference = s"XMVPD${submissionId.take(12)}"

    ReturnSubmission(
      vpdId = vpdId,
      periodKey = periodKey,
      chargeReference = chargeReference,
      submittedReturn = returnCreateRequest,
      submittedAt = submittedAt,
      submissionId = submissionId
    )
  }

  /**
   * Generates 33 return submissions matching the 33 fulfilled obligations
   * from 35 months ago to 3 months ago (skipping current month, previous month, and 2 months ago which are Open/Due/Overdue)
   */
  def generate33ReturnSubmissions(vpdId: String): Seq[ReturnSubmission] = {
    val today = LocalDate.now()
    val currentMonthStart = LocalDate.of(today.getYear, today.getMonthValue, 1)

    // Generate submissions for months 3-35 (skipping 0=current, 1=previous, 2=2 months ago)
    (3 until 36).map { monthsBack =>
      // Calculate the year and month for this submission
      val targetDate = currentMonthStart.minusMonths(monthsBack)
      val year = targetDate.getYear
      val month = targetDate.getMonthValue

      // Create proper period dates
      val periodStart = LocalDate.of(year, month, 1)
      val dueDate = periodStart.plusMonths(1).withDayOfMonth(DUE_DATE_DAY)

      // Generate period key from the period start date
      val returnPeriod = ReturnPeriod.fromDateInPeriod(periodStart)
      val periodKey = returnPeriod.toPeriodKey

      // Submission date is 5 days before due date (matching the receivedDate in obligations)
      val submissionDate = dueDate.minusDays(5)
      val submittedAt = submissionDate.atTime(10, 30).toInstant(ZoneOffset.UTC)

      // Generate unique charge reference and submission ID
      val chargeReference = f"XMVPD${year}${month}%02d${vpdId.takeRight(4)}"
      val submissionId = f"submission-${36 - monthsBack}%03d"

      createReturnSubmission(
        vpdId = vpdId,
        periodKey = periodKey,
        chargeReference = chargeReference,
        submissionId = submissionId,
        submittedAt = submittedAt
      )
    }
  }

  def sampleReturnSubmission(vpdId: String): ReturnSubmission = {
    // This matches the fulfilled obligation in ObligationsData (period 27AJ)
    createReturnSubmission(
      vpdId = vpdId,
      periodKey = "27AJ",
      chargeReference = "XMVPD0123456789ab",
      submissionId = "submission-001",
      submittedAt = Instant.parse("2027-11-15T10:30:00Z")
    )
      ReturnSubmission(
        vpdId = vpdId,
        periodKey = periodKey,
        chargeReference = chargeReference,
        submittedReturn = returnRequest,
        submittedAt = Instant.now().minusSeconds(random.nextInt(86400 * 30)),
        submissionId = submissionId
      )
    }
  }

  val sampleVpdIds: Seq[String] = Seq(
    "GBWK0000001WK",
    "GBWK0000002WK",
    "GBWK0000003WK"
  )

  def allSampleReturnSubmissions: Seq[ReturnSubmission] =
    sampleVpdIds.flatMap(generate33ReturnSubmissions)
}
