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

package uk.gov.hmrc.vapingdutystubs.data.returns

import uk.gov.hmrc.vapingdutystubs.models.returns.{DeclarationDetails, NilReturn, RegularReturn, ReturnSubmission, TotalDutyDue, VapingProductsProduced}
import uk.gov.hmrc.vapingdutystubs.models.returns.submit.ReturnCreateRequest

import java.time.Instant

object ReturnSubmissionData {

  private val sampleRegularReturn = RegularReturn(
    taxType = "301",
    dutyRate = BigDecimal("0.05"),
    amountProducedLiquid = BigDecimal("1000.50"),
    dutyDue = BigDecimal("50.03")
  )

  private val sampleTotalDutyDue = TotalDutyDue(
    totalDutyDue = BigDecimal("50.03"),
    totalDutyDueVapingProducts = BigDecimal("50.03"),
    totalDutyOverDeclaration = BigDecimal("0.00"),
    totalDutySpoiltProduct = BigDecimal("0.00"),
    totalDutyUnderDeclaration = BigDecimal("0.00"),
    adjustmentAmount = BigDecimal("0.00")
  )

  private val sampleDeclarationDetails = DeclarationDetails(
    fullName = "John Smith",
    capacityInWhichSigned = "Director",
    signeesEmailAddress = "john.smith@example.com"
  )

  private def createReturnSubmission(
    vpdId: String,
    periodKey: String,
    chargeReference: String,
    submissionId: String,
    submittedAt: Instant
  ): ReturnSubmission = {
    val returnCreateRequest = ReturnCreateRequest(
      periodKey = periodKey,
      vapingProductsProduced = VapingProductsProduced(
        nilReturn = Seq.empty,
        regularReturn = Seq(sampleRegularReturn)
      ),
      totalDutyDue = sampleTotalDutyDue,
      declaration = sampleDeclarationDetails
    )

    ReturnSubmission(
      vpdId = vpdId,
      periodKey = periodKey,
      chargeReference = chargeReference,
      submittedReturn = returnCreateRequest,
      submittedAt = submittedAt,
      submissionId = submissionId
    )
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
  }

  val sampleVpdIds: Seq[String] = Seq(
    "GBWK0000001WK",
    "GBWK0000002WK",
    "GBWK0000003WK"
  )

  def allSampleReturnSubmissions: Seq[ReturnSubmission] =
    sampleVpdIds.map(sampleReturnSubmission)
}