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

import uk.gov.hmrc.vapingdutystubs.models.returns.{ReturnSubmission, TotalDutyDue, VapingProductsProduced}
import uk.gov.hmrc.vapingdutystubs.models.returns.view.*

import java.time.{Instant, LocalDate, ZoneId}
import uk.gov.hmrc.vapingdutystubs.models.returns.NilReturn
import uk.gov.hmrc.vapingdutystubs.models.returns.RegularReturn

object ReturnsData {
  private val now = Instant.now()

  private val regularReturn_ = RegularReturn(
    taxType = "301",
    dutyRate = BigDecimal("0.05"),
    amountProducedLiquid = BigDecimal("9999999.9"),
    dutyDue = BigDecimal("999999999.99")
  )

  def idDetails(vpdReference: String, submissionId: String) = IdDetails(vpdReference, Some(submissionId))
  def vapingProductsProduced(nilReturn: Seq[NilReturn], regularReturn: Seq[RegularReturn]) = VapingProductsProduced(nilReturn, regularReturn)

  private val totalDutyDue_bigDecimal = BigDecimal("-99999999999.99")
  private val totalDutyDue = TotalDutyDue(
    totalDutyDue = totalDutyDue_bigDecimal,
    totalDutyDueVapingProducts = totalDutyDue_bigDecimal,
    totalDutyOverDeclaration = totalDutyDue_bigDecimal,
    totalDutySpoiltProduct = totalDutyDue_bigDecimal,
    totalDutyUnderDeclaration = totalDutyDue_bigDecimal,
    adjustmentAmount = totalDutyDue_bigDecimal
  )

  private def chargeDetails(periodKey: String) = ChargeDetails(
    periodKey,
    chargeReference = Some("XMVPDP0000123"),
    periodFrom = LocalDate.of(2026, 3, 14),
    periodTo = LocalDate.of(2026, 3, 30),
    receiptDate = now,
  )

  def apply(vpdReference: String, periodKey: String, submissionId: String): ReturnDisplayResponse = {
    val successResponse = ReturnDisplaySuccess(
      processingDate = now,
      idDetails = Some(idDetails(vpdReference, submissionId)),
      chargeDetails = Some(chargeDetails(periodKey)),
      vapingProductsProduced = Some(vapingProductsProduced(nilReturn = Seq.empty, regularReturn = Seq(regularReturn_))),
      overDeclaration = None,
      underDeclaration = None,
      spoiltProduct = None,
      totalDutyDue = Some(totalDutyDue),
      totalDutyDueByTaxType = None,
      otherOptions = None
    )

    ReturnDisplayResponse(success = successResponse)
  }

  def fromSubmission(submission: ReturnSubmission): ReturnDisplayResponse = {
    val submittedReturn = submission.submittedReturn
    val receiptDate = submission.submittedAt.atZone(ZoneId.systemDefault()).toLocalDate

    val successResponse = ReturnDisplaySuccess(
      processingDate = submission.submittedAt,
      idDetails = Some(IdDetails(submission.vpdId, Some(submission.submissionId))),
      chargeDetails = Some(ChargeDetails(
        periodKey = submission.periodKey,
        chargeReference = Some(submission.chargeReference),
        periodFrom = receiptDate.withDayOfMonth(1),
        periodTo = receiptDate.withDayOfMonth(receiptDate.lengthOfMonth()),
        receiptDate = submission.submittedAt
      )),
      vapingProductsProduced = Some(submittedReturn.vapingProductsProduced),
      overDeclaration = None,
      underDeclaration = None,
      spoiltProduct = None,
      totalDutyDue = Some(submittedReturn.totalDutyDue),
      totalDutyDueByTaxType = None,
      otherOptions = None
    )

    ReturnDisplayResponse(success = successResponse)
  }
}
