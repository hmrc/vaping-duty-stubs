/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.vapingdutystubs.models

import play.api.libs.json.Json
import uk.gov.hmrc.vapingdutystubs.base.SpecBase
import uk.gov.hmrc.vapingdutystubs.data.subscription.SubscriptionSummaryData
import uk.gov.hmrc.vapingdutystubs.models.subscription.ApprovalStatus
import uk.gov.hmrc.vapingdutystubs.models.subscription.ApprovalStatus.{Approved, Deregistered, Revoked}

import java.time.Instant

class SubscriptionSummarySpec extends SpecBase {
  "SubscriptionSummary must" - {
    "serialise to json" in new SetUp {
      Json
        .toJson(
          SubscriptionSummaryData.approvedSubscriptionSummary(
            now,
            false,
            standardEmailPreferences,
            ukCorrespondenceAddress
          )
        )
        .toString mustBe subscriptionSummaryJson
    }

    Seq((Approved, "01"), (Deregistered, "04"), (Revoked, "05")).foreach { case (approvalStatus, code) =>
      s"serialise ApprovalStatus ${approvalStatus.entryName} to the code $code" in new SetUp {
        Json.toJson(approvalStatus: ApprovalStatus).toString mustBe s""""$code""""
      }
    }
  }

  class SetUp {
    val now                     = Instant.now(clock)
    val subscriptionSummaryJson =
      """{"processingDate":{"$date":{"$numberLong":"1718118467838"}},"organisationName":"testAwNwaIL Ltd","paperlessPreference":"1","emailAddress":"john.doe@example.com","verifiedEmail":"1","bouncedEmail":"0","addressLine1":"Flat 123","addressLine2":"1 Example Road","postCode":"AB1 2CD","approvalStatus":"01","insolvencyFlag":"0"}"""
  }
}
