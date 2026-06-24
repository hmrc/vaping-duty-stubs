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
import uk.gov.hmrc.vapingdutystubs.models.returns.view.*
import uk.gov.hmrc.vapingdutystubs.models.returns.*

import java.time.{Instant, LocalDate}

object ReturnsData {
  private val now = Instant.now()

  private val sampleVapingReturn = VapingReturn(
    taxType = "641",
    dutyRate = BigDecimal("10.50"),
    amountProducedLiquid = BigDecimal("1500.25"),
    dutyDue = BigDecimal("15752.63")
  )

  def idDetails(vpdReference: String, submissionId: String): IdDetails = 
    IdDetails(vpdReference, Some(submissionId))

  def nilReturn(): VapingProductsProduced = 
    VapingProductsProduced(vapingProdManufactured = "0", returns = Seq.empty)

  def regularReturn(items: Seq[VapingReturn]): VapingProductsProduced = 
    VapingProductsProduced(vapingProdManufactured = "1", returns = items)

  private val totalDutyDue = TotalDutyDue(
    totalDutyDueVapingProducts = BigDecimal("15752.63"),
    totalDutyOverDeclaration = BigDecimal("1050.00"),
    totalDutyUnderDeclaration = BigDecimal("2100.00"),
    totalDutySpoiltProduct = BigDecimal("525.00"),
    adjustmentAmount = BigDecimal("0.00"),
    totalDue = BigDecimal("14177.63")
  )

  private val sampleDeclarationDetails = DeclarationDetails(
    fullName = "John Smith",
    capacityInWhichSigned = "Director",
    signeesEmailAddress = "john.smith@example.com"
  )

  private def chargeDetails(periodKey: String, chargeReference: String): ChargeDetails = ChargeDetails(
    periodKey,
    chargeReference,
    LocalDate.of(2024, 1, 1),
    LocalDate.of(2024, 1, 31),
    Instant.parse("2024-02-01T10:00:00Z")
  )

  def apply(vpdReference: String, periodKey: String, submissionId: String, chargeReference: String): ReturnDisplayResponse = {
    val successResponse = ReturnDisplaySuccess(
      processingDate = now,
      idDetails = idDetails(vpdReference, submissionId),
      chargeDetails = chargeDetails(periodKey, chargeReference),
      vapingProductsProduced = regularReturn(Seq(sampleVapingReturn)),
      overDeclaration = None,
      underDeclaration = None,
      spoiltProduct = None,
      totalDutyDue = totalDutyDue,
      otherOptions = None,
      declaration = sampleDeclarationDetails
    )

    ReturnDisplayResponse(success = successResponse)
  }

  def fromSubmission(submission: ReturnSubmission): ReturnDisplayResponse = {
    val submittedReturn = submission.submittedReturn
    val returnPeriod = ReturnPeriod.fromPeriodKeyOrThrow(submission.periodKey)

    val successResponse = ReturnDisplaySuccess(
      processingDate = submission.submittedAt,
      idDetails = IdDetails(submission.vpdId, Some(submission.submissionId)),
      chargeDetails = ChargeDetails(
        periodKey = submission.periodKey,
        chargeReference = submission.chargeReference,
        periodFrom = returnPeriod.periodFromDate(),
        periodTo = returnPeriod.periodToDate(),
        receiptDate = submission.submittedAt
      ),
      vapingProductsProduced = submittedReturn.vapingProductsProduced,
      overDeclaration = submittedReturn.overDeclaration,
      underDeclaration = submittedReturn.underDeclaration,
      spoiltProduct = submittedReturn.spoiltProduct,
      totalDutyDue = submittedReturn.totalDutyDue,
      otherOptions = submittedReturn.otherOptions,
      declaration = submittedReturn.declaration
    )

    ReturnDisplayResponse(success = successResponse)
  }
}
