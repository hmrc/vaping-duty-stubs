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

package uk.gov.hmrc.vapingdutystubs.repositories

import org.mongodb.scala.model.Filters
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport
import uk.gov.hmrc.vapingdutystubs.base.ISpecBase
import uk.gov.hmrc.vapingdutystubs.models.financialdata.{DocumentDetails, FinancialDataState, LineItemDetails}

import java.time.{Instant, LocalDate}

class FinancialDataRepositorySpec
    extends ISpecBase
    with PlayMongoRepositorySupport[FinancialDataState] {

  protected override val repository: FinancialDataRepository = new FinancialDataRepository(
    mongoComponent = mongoComponent,
    config = config
  )

  val testVpdId = "GBWK0000000001WK"

  val testDocument: DocumentDetails = DocumentDetails(
    documentNumber = "1000000000001",
    documentType = "TRM New Charge",
    chargeReferenceNumber = Some("XMVPD0000000001"),
    businessPartnerNumber = s"BP$testVpdId",
    contractAccountNumber = s"CA$testVpdId",
    contractAccountCategory = "Excise",
    contractObjectNumber = s"CO$testVpdId",
    contractObjectType = "ZVPD",
    postingDate = LocalDate.of(2026, 10, 31),
    issueDate = LocalDate.of(2026, 10, 31),
    documentTotalAmount = BigDecimal("500.00"),
    documentClearedAmount = BigDecimal(0),
    documentOutstandingAmount = BigDecimal("500.00"),
    lineItemDetails = Seq(LineItemDetails(
      itemNumber = "0001",
      subItemNumber = "001",
      mainTransaction = "4060",
      subTransaction = "3392",
      chargeDescription = "VPD Return",
      periodFromDate = LocalDate.of(2026, 10, 1),
      periodToDate = LocalDate.of(2026, 10, 31),
      periodKey = "26KJ",
      netDueDate = LocalDate.of(2026, 11, 14),
      formBundleNumber = "FB1",
      statisticalKey = "1",
      amount = BigDecimal("500.00")
    ))
  )

  val testState: FinancialDataState = FinancialDataState(
    vpdId = testVpdId,
    noDataIdentified = false,
    documentDetails = Seq(testDocument),
    lastUpdated = Instant.parse("2026-10-01T10:15:10Z")
  )

  "get must" - {
    "get the record if the given vpdId exists in the repository" in {
      repository.set(testState).futureValue

      val result = repository.get(testVpdId).futureValue

      result.value mustBe testState
    }

    "return None if the given vpdId does not exist in the repository" in {
      repository.get("NONEXISTENT").futureValue must not be defined
    }
  }

  "set must" - {
    "save the supplied financial data state to the repository" in {
      val savedState = repository.set(testState).futureValue
      val getSavedRecord = find(Filters.equal("_id", testVpdId)).futureValue.headOption.value

      savedState mustBe testState
      getSavedRecord mustBe testState
    }

    "upsert if the vpdId already exists in the repository" in {
      repository.set(testState).futureValue

      val noDataState = testState.copy(noDataIdentified = true, documentDetails = Seq.empty)

      val savedState = repository.set(noDataState).futureValue
      val getSavedRecord = find(Filters.equal("_id", testVpdId)).futureValue.headOption.value

      savedState mustBe noDataState
      getSavedRecord mustBe noDataState

      // Verify only one record exists (upsert, not insert)
      val allRecords = find(Filters.equal("_id", testVpdId)).futureValue
      allRecords.size mustBe 1
    }
  }

  "clear must" - {
    "clear all data in the repository" in {
      val savedState = repository.set(testState).futureValue
      val getSavedRecord = find(Filters.equal("_id", testVpdId)).futureValue.headOption.value

      savedState mustBe testState
      getSavedRecord mustBe testState

      val clearedData = repository.clear.futureValue
      val retrieveRecord = find(Filters.equal("_id", testVpdId)).futureValue.headOption

      clearedData.isDefined mustBe true
      retrieveRecord.isDefined mustBe false
    }
  }
}
