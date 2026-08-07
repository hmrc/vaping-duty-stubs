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

import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class FinancialDataStubDataSpec extends AnyFreeSpec with Matchers with OptionValues {

  private val vpdId = "GBWK0000006WK"

  "FinancialDataStubData" - {
    "singleOutstanding must produce a single positive-balance outstanding charge" in {
      val state = FinancialDataStubData.singleOutstanding(vpdId)
      val totalisation = FinancialDataStubData.calculateTotalisation(state.documentDetails).value.regimeTotalisation.value

      state.noDataIdentified mustBe false
      state.documentDetails.flatMap(_.chargeReferenceNumber) must have size 1
      totalisation.totalAccountBalance.value must be > BigDecimal(0)
    }

    "overdueBalance must produce a positive balance from an overdue charge" in {
      val state = FinancialDataStubData.overdueBalance(vpdId)
      val totalisation = FinancialDataStubData.calculateTotalisation(state.documentDetails).value.regimeTotalisation.value

      totalisation.totalAccountOverdue mustBe defined
      totalisation.totalAccountBalance.value must be > BigDecimal(0)
    }

    "creditBalance must produce a negative net balance" in {
      val state = FinancialDataStubData.creditBalance(vpdId)
      val totalisation = FinancialDataStubData.calculateTotalisation(state.documentDetails).value.regimeTotalisation.value

      totalisation.totalAccountCredit mustBe defined
      totalisation.totalAccountBalance.value must be < BigDecimal(0)
    }

    "nothingOwed must produce a zero balance" in {
      val state = FinancialDataStubData.nothingOwed(vpdId)
      val totalisation = FinancialDataStubData.calculateTotalisation(state.documentDetails).value.regimeTotalisation.value

      totalisation.totalAccountBalance mustBe Some(BigDecimal(0))
    }

    "interestPayment must produce an overdue original charge and an overdue interest charge with mainTransaction 4061" in {
      val state = FinancialDataStubData.interestPayment(vpdId)
      val totalisation = FinancialDataStubData.calculateTotalisation(state.documentDetails).value.regimeTotalisation.value

      state.documentDetails must have size 2

      val interestLineItems = state.documentDetails.flatMap(_.lineItemDetails).filter(_.mainTransaction == "4061")
      interestLineItems must have size 1
      interestLineItems.head.netDueDate.isBefore(java.time.LocalDate.now()) mustBe true

      totalisation.totalAccountOverdue.value mustBe BigDecimal("10200.00")
    }

    "sampleFinancialDataScenarios must contain one state per fixed VPD ID" in {
      val states = FinancialDataStubData.sampleFinancialDataScenarios

      states.map(_.vpdId) must contain theSameElementsAs Seq(
        FinancialDataStubData.singleOutstandingVpdId,
        FinancialDataStubData.overdueBalanceVpdId,
        FinancialDataStubData.creditBalanceVpdId,
        FinancialDataStubData.nothingOwedVpdId
      )
    }
  }
}
