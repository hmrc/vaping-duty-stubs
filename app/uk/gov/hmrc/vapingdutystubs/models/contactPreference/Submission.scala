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

package uk.gov.hmrc.vapingdutystubs.models.contactPreference

import com.google.inject.Inject
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.{BadRequest, Forbidden, InternalServerError, NotFound, Ok, UnprocessableEntity, UnsupportedMediaType}
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.vapingdutystubs.config.Constants.Headers.*
import uk.gov.hmrc.vapingdutystubs.data.subscription.SubscriptionSummaryData
import uk.gov.hmrc.vapingdutystubs.models.ErrorData
import uk.gov.hmrc.vapingdutystubs.models.subscription.{CorrespondenceAddress, EmailPreferences}
import uk.gov.hmrc.vapingdutystubs.repositories.SubscriptionSummaryRepository
import uk.gov.hmrc.vapingdutystubs.utils.AsyncHelper

import java.time.{Clock, Instant}
import scala.concurrent.{ExecutionContext, Future}

case class Submission @Inject() (clock: Clock, errorData: ErrorData, subscriptionSummaryRepository: SubscriptionSummaryRepository)
                                (implicit ec: ExecutionContext) extends Logging {

  def process(idType: String, idValue: String, regime: String, correlationId: String)
             (implicit request: Request[JsValue]): Future[Result] = {

    checkIdRegime(idType, regime, idValue, correlationId)
  }

  /** Extracts second int from vpdId */
  private def getStubIndex(vpdId: String): Int = vpdId.replaceAll("[a-zA-Z]+", "").charAt(1).toString.toInt
  
  private def checkIdRegime(idType: String, regime: String, idValue: String, correlationId: String)
                           (implicit request: Request[JsValue]) = {
    if (HasCorrectIdentifiers(idType, regime)) {
      Future.successful(
        UnprocessableEntity(Json.toJson(errorData.unprocessableEntity))
          .withHeaders(correlationIdHeader -> correlationId)
      )
    } else {
      checkPreferences(regime, idValue, correlationId)
    }
  }

  private def checkPreferences(regime: String, idValue: String, correlationId: String)
                              (implicit request: Request[JsValue]) = {

    val preference = request.body.validate[PaperlessPreferenceSubmission]
      .getOrElse(PaperlessPreferenceSubmission(true, None, None, None))

      logger.info(s"Email contact preference submission received for regime $regime, vpdId $idValue: $preference")

      if (HasCorrectPreferences(preference)) {
        Future.successful(
          UnprocessableEntity(Json.toJson(errorData.etmpUnprocessableEntity))
            .withHeaders(correlationIdHeader -> correlationId)
        )
      } else {
        getResult(idValue, correlationId, preference)
      }

  }

  private def getResult(idValue: String, correlationId: String, submission: PaperlessPreferenceSubmission) = {
    val now = Instant.now(clock)
    Future.successful(
      getStubIndex(idValue) match {
        case 0 =>
          Ok(Json.toJson(
            PaperlessPreferenceSubmittedSuccess(
              PaperlessPreferenceSubmittedResponse(processingDate = now, "910000000000")
            )
          )).withHeaders(correlationIdHeader -> correlationId)
        case 5 => Forbidden.withHeaders(correlationIdHeader -> correlationId)
        case 6 => UnsupportedMediaType(Json.toJson(errorData.unsupportedMediaType)).withHeaders(correlationIdHeader -> correlationId)
        case 7 =>
          BadRequest(Json.toJson(errorData.badRequest)).withHeaders(correlationIdHeader -> correlationId)
        case 8 => NotFound
        case 9 =>
          AsyncHelper().await(
            subscriptionSummaryRepository.set(
              SubscriptionSummaryData.approvedSubscriptionSummary(
                Instant.now(),
                false,
                EmailPreferences(submission.paperlessPreference, submission.emailAddress, submission.emailVerification, submission.bouncedEmail),
                CorrespondenceAddress(
                  addressLine1 = Some("Flat 123"),
                  addressLine2 = Some("1 Example Road"),
                  postCode = Some("AB1 2CD")
                )
              ), idValue
          ).map { _ =>
              Ok(Json.toJson(
                PaperlessPreferenceSubmittedSuccess(
                  PaperlessPreferenceSubmittedResponse(processingDate = now, "910000000000")
                )
              )).withHeaders(correlationIdHeader -> correlationId)
            }
          )
        case _ =>
          InternalServerError(Json.toJson(errorData.internalServerError))
            .withHeaders(correlationIdHeader -> correlationId)
      }
    )
  }
}
