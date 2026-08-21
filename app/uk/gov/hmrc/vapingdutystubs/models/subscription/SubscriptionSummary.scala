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
import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
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

  case object Deregistered extends ApprovalStatus

  case object Revoked extends ApprovalStatus

  implicit val approvalStatusWrites: Writes[ApprovalStatus] = {
    case Approved => JsString("01")
    case Deregistered => JsString("04")
    case Revoked => JsString("05")
  }

  implicit val approvalStatusReads: Reads[ApprovalStatus] = {
    case JsString("01") => JsSuccess(Approved)
    case JsString("04") => JsSuccess(Deregistered)
    case JsString("05") => JsSuccess(Revoked)
  }
}

case class EmailPreferences(
                             paperlessPreference: Boolean,
                             emailAddress: Option[String],
                             emailVerificationFlag: Option[Boolean],
                             bouncedEmailFlag: Option[Boolean]
                           )

case class CorrespondenceAddress(
                                  addressLine1: Option[String],
                                  addressLine2: Option[String] = None,
                                  addressLine3: Option[String] = None,
                                  addressLine4: Option[String] = None,
                                  addressLine5: Option[String] = None,
                                  postCode: Option[String]
                                )

final case class SubscriptionSummaryResponse(
                                              processingDate: Instant,
                                              organisationName: String,
                                              paperlessPreference: Option[Boolean],
                                              emailAddress: Option[String],
                                              verifiedEmail: Option[Boolean],
                                              bouncedEmail: Option[Boolean],
                                              addressLine1: Option[String],
                                              addressLine2: Option[String],
                                              addressLine3: Option[String],
                                              addressLine4: Option[String],
                                              addressLine5: Option[String],
                                              postCode: Option[String],
                                              approvalStatus: ApprovalStatus,
                                              insolvencyStatus: String,
                                              vpdId: Option[String]
                                            )

object SubscriptionSummaryResponse {

  import JsonHelpers.*

  implicit def format: OFormat[SubscriptionSummaryResponse] =
    (
      (__ \ "processingDate").format(MongoJavatimeFormats.instantFormat) and
        (__ \ "organisationName").format[String] and
        (__ \ "paperlessPreference").formatNullable[Boolean] and
        (__ \ "emailAddress").formatNullable[String] and
        (__ \ "verifiedEmail").formatNullable[Boolean] and
        (__ \ "bouncedEmail").formatNullable[Boolean] and
        (__ \ "addressLine1").formatNullable[String] and
        (__ \ "addressLine2").formatNullable[String] and
        (__ \ "addressLine3").formatNullable[String] and
        (__ \ "addressLine4").formatNullable[String] and
        (__ \ "addressLine5").formatNullable[String] and
        (__ \ "postCode").formatNullable[String] and
        (__ \ "approvalStatus").format[ApprovalStatus] and
        (__ \ "insolvencyStatus").format[String] and
        (__ \ "vpdId").formatNullable[String]
      )(SubscriptionSummaryResponse.apply, o => Tuple.fromProductTyped(o))
}
