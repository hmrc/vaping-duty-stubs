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

package uk.gov.hmrc.vapingdutystubs.controllers

import play.api.Logging
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.vapingdutystubs.config.AppConfig
import uk.gov.hmrc.vapingdutystubs.config.Constants.Headers.*
import uk.gov.hmrc.vapingdutystubs.data.subscription.SubscriptionSummaryData
import uk.gov.hmrc.vapingdutystubs.models.ErrorData
import uk.gov.hmrc.vapingdutystubs.models.subscription.{CorrespondenceAddress, EmailPreferences}
import uk.gov.hmrc.vapingdutystubs.utils.LogHeadersHelper.logHeaders

import java.time.{Clock, Instant}
import javax.inject.Inject

class SubscriptionSummaryController @Inject()(
                                            config: AppConfig,
                                            errorData: ErrorData,
                                            clock: Clock,
                                            cc: ControllerComponents
) extends BackendController(cc)
    with Logging {

  private val allReturnsHeaders = Set(
    HeaderNames.AUTHORIZATION,
    correlationIdHeader,
    xOriginatingSystemHeader,
    xReceiptDateHeader,
    xTransmittingSystemHeader
  )

  private val approved            = config.Patterns.approved
  private val rejected            = config.Patterns.rejected
  private val withdrawn           = config.Patterns.withdrawn
  private val notFound            = config.Patterns.notFound
  private val badRequest          = config.Patterns.badRequest
  private val unprocessableEntity = config.Patterns.unprocessableEntity
  
  private val emailAddress = "john.doe@example.com"

  private def getEmailPreferences(idValue: String): EmailPreferences =
    // Regex extracts first digit in String
    val emailFlagDigit = config.Patterns.getEmailFlagDigit(idValue)

    emailFlagDigit match {
      case 0 | 5 | 6 | 7 | 8 => // email selected
        EmailPreferences(
          paperlessPreference = true,
          emailAddress = Some(emailAddress),
          emailVerificationFlag = Some(true),
          bouncedEmailFlag = Some(false)
        )
      case 1                 => // paper selected, has email in system with no problems
        EmailPreferences(
          paperlessPreference = false,
          emailAddress = Some(emailAddress),
          emailVerificationFlag = Some(true),
          bouncedEmailFlag = Some(false)
        )
      case 2                 => // paper selected, has unverified email in system
        EmailPreferences(
          paperlessPreference = false,
          emailAddress = Some(emailAddress),
          emailVerificationFlag = Some(false),
          bouncedEmailFlag = Some(false)
        )
      case 3                 => // paper selected, has bounced email in system
        EmailPreferences(
          paperlessPreference = false,
          emailAddress = Some(emailAddress),
          emailVerificationFlag = Some(true),
          bouncedEmailFlag = Some(true)
        )
      case _                 => // paper selected, no email in system
        EmailPreferences(
          paperlessPreference = false,
          emailAddress = None,
          emailVerificationFlag = None,
          bouncedEmailFlag = None
        )

    }

  private def getCorrespondenceAddress(idValue: String): CorrespondenceAddress =
    val emailFlagDigit = config.Patterns.getEmailFlagDigit(idValue)

      emailFlagDigit match {
        case 5 => // overseas address 1
          CorrespondenceAddress(
            addressLine1 = Some("Flat 123"),
            addressLine2 = Some("1 Example Road"),
            addressLine3 = Some("Toronto"),
            postCode = Some("P55555")
          )
        case 6 => // overseas address 2
          CorrespondenceAddress(
            addressLine1 = Some("1 Example Road"),
            addressLine2 = Some("Barcelona"),
            postCode = Some("P66666")
          )
        case 7 => // country code not in mapping
          CorrespondenceAddress(
            addressLine1 = Some("Flat 123"),
            addressLine2 = Some("1 Example Road"),
            addressLine3 = Some("District A"),
            postCode = None
          )
        case 8 => // no country code
          CorrespondenceAddress(
            addressLine1 = Some("Building 1"),
            postCode = Some("P88888")
          )
        case _ => // UK address
          CorrespondenceAddress(
            addressLine1 = Some("Flat 123"),
            addressLine2 = Some("1 Example Road"),
            postCode = Some("AB1 2CD")
          )
    }

  def getSubscriptionSummary(idValue: String): Action[AnyContent] = Action { request =>
    logHeaders(request, "getSubscriptionSummary", allReturnsHeaders)
    if (!idValue.matches(config.Patterns.validVpdId)) {
      throw new RuntimeException(s"Bad vpdId '$idValue' sent to stubs")
    } else {
      val correlationId = request.headers
        .get(correlationIdHeader)
        .getOrElse(throw new IllegalArgumentException("Expected correlation ID header"))

        val now = Instant.now(clock)
        val emailPreferences = getEmailPreferences(idValue)
        val correspondenceAddress = getCorrespondenceAddress(idValue)

        idValue.replaceAll(config.Patterns.allChars, "") match {
          case approved() =>
            Ok(
              Json.toJson(
                SubscriptionSummaryData
                  .approvedSubscriptionSummary(
                    now,
                    false,
                    emailPreferences,
                    correspondenceAddress
                  )
              )
            ).withHeaders(correlationIdHeader -> correlationId)
          case rejected() =>
            Ok(
              Json.toJson(
                SubscriptionSummaryData
                  .rejectedSubscriptionSummary(
                    now,
                    false,
                    emailPreferences,
                    correspondenceAddress
                  )
              )
            ).withHeaders(correlationIdHeader -> correlationId)
          case withdrawn() =>
            Ok(
              Json.toJson(
                SubscriptionSummaryData
                  .withDrawnSubscriptionSummary(
                    now,
                    false,
                    emailPreferences,
                    correspondenceAddress
                  )
              )
            ).withHeaders(correlationIdHeader -> correlationId)
          case badRequest() =>
            BadRequest(Json.toJson(errorData.badRequest)).withHeaders(correlationIdHeader -> correlationId)
          case notFound() => NotFound
          case unprocessableEntity() =>
            UnprocessableEntity(Json.toJson(errorData.unprocessableEntity))
              .withHeaders(correlationIdHeader -> correlationId)
          case _ =>
            InternalServerError(Json.toJson(errorData.internalServerError))
              .withHeaders(correlationIdHeader -> correlationId)
        }
      }
    }
  }
