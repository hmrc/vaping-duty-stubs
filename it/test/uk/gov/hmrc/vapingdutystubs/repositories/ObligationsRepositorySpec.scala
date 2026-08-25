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

package uk.gov.hmrc.vapingdutystubs.repositories

import org.mongodb.scala.model.Filters
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport
import uk.gov.hmrc.vapingdutystubs.base.ISpecBase
import uk.gov.hmrc.vapingdutystubs.models.obligations.{Identification, ObligationDetails, ObligationItem, ObligationState}

import java.time.{Instant, LocalDate}

class ObligationsRepositorySpec
    extends ISpecBase
    with PlayMongoRepositorySupport[ObligationState] {

  protected override val repository: ObligationsRepository = new ObligationsRepository(
    mongoComponent = mongoComponent,
    config = config
  )

  val testVpdId = "GBWK0000000001WK"
  val testPeriodKey = "27AJ"
  val currentDate = LocalDate.now()

  val openObligationDetails = ObligationDetails(
    openOrFulfilledStatus = "O",
    iCFromDate = LocalDate.of(2027, 12, 1),
    iCToDate = LocalDate.of(2027, 12, 31),
    iCDateReceived = None,
    iCDueDate = currentDate.plusDays(10),
    periodKey = "27AL"
  )

  val fulfilledObligationDetails = ObligationDetails(
    openOrFulfilledStatus = "F",
    iCFromDate = LocalDate.of(2027, 10, 1),
    iCToDate = LocalDate.of(2027, 10, 31),
    iCDateReceived = Some(LocalDate.of(2027, 11, 15)),
    iCDueDate = LocalDate.of(2027, 11, 30),
    periodKey = testPeriodKey
  )

  val testObligationItem = ObligationItem(
    identification = Identification(
      referenceType = "ZVPD",
      referenceNumber = testVpdId,
      incomeSourceType = None
    ),
    obligationDetails = Seq(openObligationDetails, fulfilledObligationDetails)
  )

  val testObligationState: ObligationState = ObligationState(
    vpdId = testVpdId,
    obligations = Seq(testObligationItem)
  )

  "get must" - {
    "get the record if the given vpdId exists in the repository" in {
      repository.set(testObligationState).futureValue

      val result = repository.get(testVpdId).futureValue

      result.value mustBe testObligationState
    }

    "return None if the given vpdId does not exist in the repository" in {
      repository.get("NONEXISTENT").futureValue must not be defined
    }
  }

  "set must" - {
    "save the supplied obligation state to the repository" in {
      val savedState = repository.set(testObligationState).futureValue
      val getSavedRecord = find(Filters.equal("_id", testVpdId)).futureValue.headOption.value

      savedState mustBe testObligationState
      getSavedRecord mustBe testObligationState
    }

    "upsert if the vpdId already exists in the repository" in {
      repository.set(testObligationState).futureValue

      val updatedObligationDetails = openObligationDetails.copy(
        openOrFulfilledStatus = "F",
        iCDateReceived = Some(currentDate)
      )

      val updatedObligationItem = testObligationItem.copy(
        obligationDetails = Seq(updatedObligationDetails, fulfilledObligationDetails)
      )

      val updatedState = testObligationState.copy(
        obligations = Seq(updatedObligationItem)
      )

      val savedState = repository.set(updatedState).futureValue
      val getSavedRecord = find(Filters.equal("_id", testVpdId)).futureValue.headOption.value

      savedState mustBe updatedState
      getSavedRecord mustBe updatedState

      // Verify only one record exists (upsert, not insert)
      val allRecords = find(Filters.equal("_id", testVpdId)).futureValue
      allRecords.size mustBe 1
    }
  }

  "markAsFulfilled must" - {
    "update the obligation status to fulfilled for the matching periodKey" in {
      val openObligationDetailsForTest = openObligationDetails.copy(
        periodKey = testPeriodKey,
        openOrFulfilledStatus = "O",
        iCDateReceived = None
      )

      val itemWithOpenObligation = testObligationItem.copy(
        obligationDetails = Seq(openObligationDetailsForTest)
      )

      val stateWithOpenObligation = testObligationState.copy(
        obligations = Seq(itemWithOpenObligation)
      )

      repository.set(stateWithOpenObligation).futureValue

      val receivedDate = Instant.parse("2027-11-15T10:30:00Z")
      val result = repository.markAsFulfilled(testVpdId, testPeriodKey, receivedDate).futureValue

      result.value.obligations.head.obligationDetails.head.openOrFulfilledStatus mustBe "F"
      result.value.obligations.head.obligationDetails.head.iCDateReceived.value mustBe receivedDate.atZone(java.time.ZoneId.systemDefault()).toLocalDate
    }

    "not update obligations with different periodKeys" in {
      repository.set(testObligationState).futureValue

      val receivedDate = Instant.parse("2027-11-15T10:30:00Z")
      val result = repository.markAsFulfilled(testVpdId, "DIFFERENT_KEY", receivedDate).futureValue

      // Original obligations should remain unchanged
      result.value.obligations mustBe testObligationState.obligations
    }

    "return None if the vpdId does not exist" in {
      val receivedDate = Instant.parse("2027-11-15T10:30:00Z")
      val result = repository.markAsFulfilled("NONEXISTENT", testPeriodKey, receivedDate).futureValue

      result must not be defined
    }
  }

  "clear must" - {
    "clear all data in the repository" in {
      val savedState = repository.set(testObligationState).futureValue
      val getSavedRecord = find(Filters.equal("_id", testVpdId)).futureValue.headOption.value

      savedState mustBe testObligationState
      getSavedRecord mustBe testObligationState

      val clearedData = repository.clear.futureValue
      val retrieveRecord = find(Filters.equal("_id", testVpdId)).futureValue.headOption

      clearedData.isDefined mustBe true
      retrieveRecord.isDefined mustBe false
    }
  }
}