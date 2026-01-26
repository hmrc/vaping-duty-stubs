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

package uk.gov.hmrc.vapingdutystubs.models.subscription

import enumeratum.{Enum, EnumEntry}
import play.api.libs.json._
import uk.gov.hmrc.vapingdutystubs.models.JsonHelpers

import java.time.Instant

sealed trait ApprovalType extends EnumEntry

object ApprovalType extends Enum[ApprovalType] {
  val values = findValues
}

sealed trait ApprovalStatus extends EnumEntry

object ApprovalStatus extends Enum[ApprovalStatus] {
  val values = findValues

  case object Approved extends ApprovalStatus
  case object Rejected extends ApprovalStatus
  case object Withdrawn extends ApprovalStatus

  implicit val approvalStatusWrites: Writes[ApprovalStatus] = {
    case Approved     => JsString("01")
    case Rejected     => JsString("02")
    case Withdrawn    => JsString("03")
  }
}

case class EmailPreferences(
  paperlessReference: Boolean,
  emailAddress: Option[String],
  emailVerificationFlag: Option[Boolean],
  bouncedEmailFlag: Option[Boolean]
)

case class CorrespondenceAddress(
  addressLine1: Option[String],
  addressLine2: Option[String] = None,
  addressLine3: Option[String] = None,
  addressLine4: Option[String] = None,
  postcode: Option[String],
  country: Option[String]
)

final case class SubscriptionSummarySuccess(success: SubscriptionSummaryResponse)

object SubscriptionSummarySuccess {
  implicit val subscriptionSummarySuccessWrites: Writes[SubscriptionSummarySuccess] =
    Json.writes[SubscriptionSummarySuccess]
}

final case class SubscriptionSummaryResponse(
  processingDate: Instant,
  organisationName: String,
  paperlessReference: Option[Boolean],
  emailAddress: Option[String],
  verifiedEmail: Option[Boolean],
  bouncedEmail: Option[Boolean],
  addressLine1: Option[String],
  addressLine2: Option[String],
  addressLine3: Option[String],
  addressLine4: Option[String],
  postcode: Option[String],
  country: Option[String],
  approvalStatus: ApprovalStatus,
  insolvencyFlag: Boolean
)

object SubscriptionSummaryResponse {
  import JsonHelpers.booleanWrites

  implicit val subscriptionSummaryResponseWrites: Writes[SubscriptionSummaryResponse] =
    Json.writes[SubscriptionSummaryResponse]
}
