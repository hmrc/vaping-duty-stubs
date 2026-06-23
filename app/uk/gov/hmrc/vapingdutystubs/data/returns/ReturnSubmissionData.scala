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

object ReturnSubmissionData {

  private val random = new Random()

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
        submittedReturn = returnRequest,
        submittedAt = Instant.now().minusSeconds(random.nextInt(86400 * 30)),
        submissionId = submissionId
      )
    }
  }
}
