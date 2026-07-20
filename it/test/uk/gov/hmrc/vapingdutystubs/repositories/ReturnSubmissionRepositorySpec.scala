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

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport
import uk.gov.hmrc.vapingdutystubs.base.ISpecBase
import uk.gov.hmrc.vapingdutystubs.models.returns.*
import uk.gov.hmrc.vapingdutystubs.models.returns.submit.ReturnCreateRequest

import java.time.Instant

class ReturnSubmissionRepositorySpec
    extends ISpecBase
    with PlayMongoRepositorySupport[ReturnSubmission]
    with BeforeAndAfterEach {

  override protected val repository: ReturnSubmissionRepository = new ReturnSubmissionRepository(
    mongoComponent = mongoComponent,
    config = config
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.clear.futureValue
  }

  val vpdId = "GBWK1234567WK"
  val periodKey = "24KA"

  val sampleReturnRequest: ReturnCreateRequest = ReturnCreateRequest(
    periodKey = periodKey,
    vapingProductsProduced = VapingProductsProduced(
      vapingProdManufactured = "1",
      returns = Seq(VapingReturn(
        taxType = "641",
        dutyRate = BigDecimal("10.50"),
        amountProducedLiquid = BigDecimal("1500.25"),
        dutyDue = BigDecimal("15752.63")
      ))
    ),
    overDeclaration = None,
    underDeclaration = None,
    spoiltProduct = None,
    totalDutyDue = TotalDutyDue(
      totalDutyDueVapingProducts = BigDecimal("15752.63"),
      totalDutyOverDeclaration = BigDecimal("0.00"),
      totalDutyUnderDeclaration = BigDecimal("0.00"),
      totalDutySpoiltProduct = BigDecimal("0.00"),
      totalDue = BigDecimal("15752.63")
    ),
    otherOptions = None,
    declaration = DeclarationDetails(
      fullName = "John Smith",
      capacityInWhichSigned = "Director",
      signeesEmailAddress = "john.smith@example.com"
    )
  )

  val sampleSubmission: ReturnSubmission = ReturnSubmission(
    vpdId = "GBWK1234567WK",
    periodKey = "24KA",
    chargeReference = Some("XMVPD123456789012"),
    submittedReturn = sampleReturnRequest,
    submittedAt = Instant.parse("2026-05-28T10:30:00Z"),
    submissionId = "123456789012"
  )

  "ReturnSubmissionRepository" - {
    "set" - {
      "must store a return submission" in {
        val result = repository.set(sampleSubmission).futureValue

        result shouldBe sampleSubmission
      }

      "must update an existing return submission" in {
        repository.set(sampleSubmission).futureValue

        val updatedSubmission = sampleSubmission.copy(
          chargeReference = Some("XMVPD999999999999")
        )

        val result = repository.set(updatedSubmission).futureValue

        result shouldBe updatedSubmission
      }
    }

    "get" - {
      "must return a return submission when it exists" in {
        repository.set(sampleSubmission).futureValue

        val result = repository.get(vpdId, periodKey).futureValue

        result.value shouldBe sampleSubmission
      }

      "must return None when return submission does not exist" in {
        val result = repository.get(vpdId, periodKey).futureValue

        result shouldBe None
      }
    }

    "clear" - {
      "must remove all return submissions" in {
        repository.set(sampleSubmission).futureValue

        repository.clear.futureValue

        val result = repository.get(vpdId, periodKey).futureValue

        result shouldBe None
      }
    }
  }
}
