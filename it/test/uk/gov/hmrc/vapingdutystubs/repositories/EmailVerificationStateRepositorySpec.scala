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
import uk.gov.hmrc.vapingdutystubs.base.ISpecBase
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport
import uk.gov.hmrc.vapingdutystubs.models.emailcontactpreferences.EmailVerificationState

import java.time.Clock

class EmailVerificationStateRepositorySpec
    extends ISpecBase
    with PlayMongoRepositorySupport[EmailVerificationState] {
  
  protected override val repository: EmailVerificationStateRepository = new EmailVerificationStateRepository(
    mongoComponent = mongoComponent
  )

  val testCredId = "TESTCREDID00001"

  val stateDataToCache: EmailVerificationState = EmailVerificationState(testCredId, returnAllUnverified = false)

  "get must" - {
    "get the record if the given credId exists in the cache" in {
      repository.set(stateDataToCache).futureValue

      val result = repository.get(testCredId).futureValue

      result.value mustBe stateDataToCache
    }

    "return None if the given credId does not exist in the cache" in {
      repository.get("credId that does not exist").futureValue must not be defined
    }
  }

  "set must" - {
    "save the supplied state data to the cache" in {
      val savedStateData = repository.set(stateDataToCache).futureValue
      val getSavedRecord = find(Filters.equal("credId", testCredId)).futureValue.headOption.value

      savedStateData mustBe stateDataToCache
      getSavedRecord mustBe stateDataToCache
    }

    "upsert if the credId already exists in the cache" in {
      repository.set(stateDataToCache).futureValue

      val now = Clock.systemDefaultZone().instant()
      
      val expectedSavedStateData = EmailVerificationState(testCredId, returnAllUnverified = true)

      val savedStateData = repository.set(stateDataToCache.copy(returnAllUnverified = true)).futureValue
      val getSavedRecord = find(Filters.equal("credId", testCredId)).futureValue.headOption.value

      savedStateData mustBe expectedSavedStateData
      getSavedRecord mustBe expectedSavedStateData
    }
  }
}
