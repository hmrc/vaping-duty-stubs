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

import uk.gov.hmrc.vapingdutystubs.models.obligations.{ObligationDetails, ObligationItem, ObligationState}

import java.time.LocalDate

object ObligationsData {

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

  def sampleObligations(vpdId: String): ObligationState = {
    val currentDate = LocalDate.now()

    ObligationState(
      vpdId = vpdId,
      obligations = Seq(
        // Outstanding return - Due soon
        createObligation(
          status = "O",
          fromDate = LocalDate.of(2027, 12, 1),
          toDate = LocalDate.of(2027, 12, 31),
          dueDate = currentDate.plusDays(10),
          periodKey = "27AL"
        ),
        // Outstanding return - Overdue
        createObligation(
          status = "O",
          fromDate = LocalDate.of(2027, 11, 1),
          toDate = LocalDate.of(2027, 11, 30),
          dueDate = currentDate.minusDays(5),
          periodKey = "27AK"
        ),
        // Fulfilled return
        createObligation(
          status = "F",
          fromDate = LocalDate.of(2027, 10, 1),
          toDate = LocalDate.of(2027, 10, 31),
          dueDate = LocalDate.of(2027, 11, 30),
          periodKey = "27AJ",
          receivedDate = Some(LocalDate.of(2027, 11, 15))
        )
      )
    )
  }

  val sampleVpdIds: Seq[String] = Seq(
    "GBWK0000000001WK",
    "GBWK0000000002WK",
    "GBWK0000000003WK"
  )

  def allSampleObligations: Seq[ObligationState] =
    sampleVpdIds.map(sampleObligations)
}