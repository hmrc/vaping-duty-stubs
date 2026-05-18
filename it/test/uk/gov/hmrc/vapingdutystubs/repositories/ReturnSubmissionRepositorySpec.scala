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
import uk.gov.hmrc.vapingdutystubs.models.returns.{RegularReturn, ReturnSubmission, TotalDutyDue, VapingProductsProduced}
import uk.gov.hmrc.vapingdutystubs.models.returns.submit.ReturnCreateRequest

import java.time.Instant

class ReturnSubmissionRepositorySpec
    extends ISpecBase
    with PlayMongoRepositorySupport[ReturnSubmission] {

  protected override val repository: ReturnSubmissionRepository = new ReturnSubmissionRepository(
    mongoComponent = mongoComponent,
    config = config
  )

  val testVpdId = "GBWK0000000001WK"
  val testPeriodKey = "27AJ"
  val testChargeReference = "XMVPD0123456789ab"
  val testSubmissionId = "submission-001"
  val testSubmittedAt = Instant.parse("2027-11-15T10:30:00Z")

  val sampleReturnRequest = ReturnCreateRequest(
    periodKey = testPeriodKey,
    vapingProductsProduced = VapingProductsProduced(
      nilReturn = Seq.empty,
      regularReturn = Seq(RegularReturn(
        taxType = "301",
        dutyRate = BigDecimal("0.05"),
        amountProducedLiquid = BigDecimal("1000.50"),
        dutyDue = BigDecimal("50.03")
      ))
    ),
    totalDutyDue = TotalDutyDue(
      totalDutyDue = BigDecimal("50.03"),
      totalDutyDueVapingProducts = BigDecimal("50.03"),
      totalDutyOverDeclaration = BigDecimal("0.00"),
      totalDutySpoiltProduct = BigDecimal("0.00"),
      totalDutyUnderDeclaration = BigDecimal("0.00"),
      adjustmentAmount = BigDecimal("0.00")
    )
  )

  val testSubmission: ReturnSubmission = ReturnSubmission(
    vpdId = testVpdId,
    periodKey = testPeriodKey,
    chargeReference = testChargeReference,
    submittedReturn = sampleReturnRequest,
    submittedAt = testSubmittedAt,
    submissionId = testSubmissionId
  )

  "get must" - {
    "get the record if the given vpdId and periodKey exist in the repository" in {
      repository.set(testSubmission).futureValue

      val result = repository.get(testVpdId, testPeriodKey).futureValue

      result.value mustBe testSubmission
    }

    "return None if the given vpdId and periodKey do not exist in the repository" in {
      repository.get("NONEXISTENT", "99ZZ").futureValue must not be defined
    }
  }

  "set must" - {
    "save the supplied submission to the repository" in {
      val savedSubmission = repository.set(testSubmission).futureValue
      val getSavedRecord = find(
        Filters.and(
          Filters.equal("vpdId", testVpdId),
          Filters.equal("periodKey", testPeriodKey)
        )
      ).futureValue.headOption.value

      savedSubmission mustBe testSubmission
      getSavedRecord mustBe testSubmission
    }

    "upsert if the vpdId and periodKey already exist in the repository (amendment)" in {
      repository.set(testSubmission).futureValue

      val amendedSubmission = testSubmission.copy(
        chargeReference = "XMVPD9876543210fe",
        submissionId = "submission-002",
        submittedAt = Instant.parse("2027-11-16T14:00:00Z")
      )

      val savedSubmission = repository.set(amendedSubmission).futureValue
      val getSavedRecord = find(
        Filters.and(
          Filters.equal("vpdId", testVpdId),
          Filters.equal("periodKey", testPeriodKey)
        )
      ).futureValue.headOption.value

      savedSubmission mustBe amendedSubmission
      getSavedRecord mustBe amendedSubmission

      // Verify only one record exists (upsert, not insert)
      val allRecords = find(Filters.equal("vpdId", testVpdId)).futureValue
      allRecords.size mustBe 1
    }
  }

  "clear must" - {
    "clear all data in the repository" in {
      val savedSubmission = repository.set(testSubmission).futureValue
      val getSavedRecord = find(
        Filters.and(
          Filters.equal("vpdId", testVpdId),
          Filters.equal("periodKey", testPeriodKey)
        )
      ).futureValue.headOption.value

      savedSubmission mustBe testSubmission
      getSavedRecord mustBe testSubmission

      val clearedData = repository.clear.futureValue
      val retrieveRecord = find(
        Filters.and(
          Filters.equal("vpdId", testVpdId),
          Filters.equal("periodKey", testPeriodKey)
        )
      ).futureValue.headOption

      clearedData.isDefined mustBe true
      retrieveRecord.isDefined mustBe false
    }
  }
}