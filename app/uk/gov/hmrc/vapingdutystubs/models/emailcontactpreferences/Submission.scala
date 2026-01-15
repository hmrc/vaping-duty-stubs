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

package uk.gov.hmrc.vapingdutystubs.models.emailcontactpreferences

import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Request, Result}
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound, Ok, UnprocessableEntity}
import uk.gov.hmrc.vapingdutystubs.config.Constants.Headers.*
import uk.gov.hmrc.vapingdutystubs.models.ErrorData

import java.time.{Clock, Instant}
import scala.concurrent.Future

case class Submission(clock: Clock, errorData: ErrorData) extends Logging {

  def process(idType: String, idValue: String, regime: String, correlationId: String)
             (implicit request: Request[JsValue]): Future[Result] = {

    checkIdRegime(idType, regime, idValue, correlationId)
  }

  private def getStubIndex(vpdId: String): Int = vpdId.takeRight(9).takeRight(1).toInt
  
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
        getResult(idValue, correlationId)
      }

  }

  private def getResult(idValue: String, correlationId: String) = {
    val now = Instant.now(clock)
    println(s"STUB INDEX ${getStubIndex(idValue)}")
    Future.successful(
      getStubIndex(idValue) match {
        case 0 =>
          Ok(Json.toJson(
            PaperlessPreferenceSubmittedSuccess(
              PaperlessPreferenceSubmittedResponse(processingDate = now, "910000000000")
            )
          )).withHeaders(correlationIdHeader -> correlationId)
        case 7 =>
          BadRequest(Json.toJson(errorData.badRequest)).withHeaders(correlationIdHeader -> correlationId)
        case 8 => NotFound
        case _ =>
          InternalServerError(Json.toJson(errorData.internalServerError))
            .withHeaders(correlationIdHeader -> correlationId)
      }
    )
  }
}
