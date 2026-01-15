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

package uk.gov.hmrc.vapingdutystubs.data.subscription

import uk.gov.hmrc.vapingdutystubs.models.subscription.*
import uk.gov.hmrc.vapingdutystubs.models.subscription.ApprovalStatus.{Approved, DeRegistered, Revoked}

import java.time.Instant

object SubscriptionSummaryData {
  private val organisation = "testAwNwaIL Ltd"

  private def subscriptionSummarySuccess(
    now: Instant,
    approvalStatus: ApprovalStatus,
    insolvencyFlag: Boolean,
    emailPreferences: EmailPreferences,
    correspondenceAddress: CorrespondenceAddress
  ): SubscriptionSummarySuccess =
    SubscriptionSummarySuccess(
      SubscriptionSummaryResponse(
        processingDate = now,
        organizationName = organisation,
        paperlessReference = Some(emailPreferences.paperlessReference), // TODO: Remove Option when included in real API
        emailAddress = emailPreferences.emailAddress,
        emailVerificationFlag = emailPreferences.emailVerificationFlag,
        bouncedEmailFlag = emailPreferences.bouncedEmailFlag,
        addressLine1 = correspondenceAddress.addressLine1,
        addressLine2 = correspondenceAddress.addressLine2,
        addressLine3 = correspondenceAddress.addressLine3,
        addressLine4 = correspondenceAddress.addressLine4,
        postcode = correspondenceAddress.postcode,
        country = correspondenceAddress.country,
        approvalStatus = approvalStatus,
        insolvencyFlag = insolvencyFlag
      )
    )

  def approvedSubscriptionSummary(
    now: Instant,
    insolvencyFlag: Boolean,
    emailPreferences: EmailPreferences,
    correspondenceAddress: CorrespondenceAddress
  ): SubscriptionSummarySuccess =
    subscriptionSummarySuccess(
      now,
      Approved,
      insolvencyFlag,
      emailPreferences,
      correspondenceAddress
    )

  def deregisteredSubscriptionSummary(
    now: Instant,
    insolvencyFlag: Boolean,
    emailPreferences: EmailPreferences,
    correspondenceAddress: CorrespondenceAddress
  ): SubscriptionSummarySuccess =
    subscriptionSummarySuccess(
      now,
      DeRegistered,
      insolvencyFlag,
      emailPreferences,
      correspondenceAddress
    )

  def revokedSubscriptionSummary(
    now: Instant,
    insolvencyFlag: Boolean,
    emailPreferences: EmailPreferences,
    correspondenceAddress: CorrespondenceAddress
  ): SubscriptionSummarySuccess =
    subscriptionSummarySuccess(
      now,
      Revoked,
      insolvencyFlag,
      emailPreferences,
      correspondenceAddress
    )
  
}
