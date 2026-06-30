/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.vapingdutystubs.controllers.returns

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.vapingdutystubs.data.returns.{ReturnSubmissionData, ReturnsData}
import uk.gov.hmrc.vapingdutystubs.models.{DownstreamError, DownstreamErrorDetails, EtmpDownstreamError, EtmpDownstreamErrorDetails}
import uk.gov.hmrc.vapingdutystubs.repositories.ReturnSubmissionRepository
import uk.gov.hmrc.vapingdutystubs.utils.RandomUUIDGenerator

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ViewReturnController @Inject()(
                                      cc: ControllerComponents,
                                      returnSubmissionRepository: ReturnSubmissionRepository,
                                      uuidGenerator: RandomUUIDGenerator
                                    )(using ExecutionContext) extends BackendController(cc)
  with Logging {

  private val LOG_ID = "ABCDEF1234567890ABCDEF1234567890"
  private val chargeReference = s"XMVPD${uuidGenerator.uuidHyphenTrimmed.take(12)}".toUpperCase
  /**
   * Checks if the VPD ID's last digit triggers a test error response.
   * Returns Some(Result) if an error should be returned, None for normal flow.
   * VPD ID format: (GB|XI)WK[7 digits]WK, so we check the 7th digit (3rd from end)
   */
  private def checkForTestErrorResponse(vpdId: String): Option[Result] = {
    // Extract the last digit before "WK" suffix (3rd character from end)
    val lastDigit = if (vpdId.length >= 3) vpdId.charAt(vpdId.length - 3).toString else ""
    val now = Instant.now()

    lastDigit match {
      case "1" =>
        logger.info(s"[ViewReturn] Returning 400 Bad Request for test vpdId: $vpdId")
        Some(BadRequest(Json.toJson(DownstreamError(
          DownstreamErrorDetails("400", "Invalid request payload. Missing required field 'periodKey'.", LOG_ID)
        ))))
      case "2" =>
        logger.info(s"[ViewReturn] Returning 403 Forbidden for test vpdId: $vpdId")
        Some(Forbidden(Json.toJson(DownstreamError(
          DownstreamErrorDetails("403", "Forbidden", LOG_ID)
        ))))
      case "3" =>
        logger.info(s"[ViewReturn] Returning 404 Not Found for test vpdId: $vpdId")
        Some(NotFound(Json.toJson(DownstreamError(
          DownstreamErrorDetails("404", "Not Found", LOG_ID)
        ))))
      case "5" =>
        logger.info(s"[ViewReturn] Returning 422 Unprocessable Entity for test vpdId: $vpdId")
        Some(UnprocessableEntity(Json.toJson(EtmpDownstreamError(
          EtmpDownstreamErrorDetails("002", "ID Number missing or invalid", now.toString)
        ))))
      case "8" =>
        logger.info(s"[ViewReturn] Returning 500 Internal Server Error for test vpdId: $vpdId")
        Some(InternalServerError(Json.toJson(DownstreamError(
          DownstreamErrorDetails("500", "SAP PI system is currently unavailable. Please try again later.", LOG_ID)
        ))))
      case _ =>
        None
    }
  }

  def viewReturn(vpdReference: String, periodKey: String): Action[AnyContent] = Action.async {
    implicit request =>
      logger.info(s"[ViewReturn] Received request to view return for vpdId: $vpdReference, periodKey: $periodKey")

      // Check for test error responses based on last digit of VPD ID
      checkForTestErrorResponse(vpdReference) match {
        case Some(errorResult) =>
          Future.successful(errorResult)
        case None =>
          processViewReturn(vpdReference, periodKey)
      }
  }

  private def processViewReturn(vpdReference: String, periodKey: String): Future[Result] = {
    logger.debug(s"[ViewReturn] Querying repository for vpdId: $vpdReference, periodKey: $periodKey")

    returnSubmissionRepository.get(vpdReference, periodKey).flatMap {
      case Some(submission) =>
        logger.info(s"[ViewReturn] Found return submission for vpdId: $vpdReference, periodKey: $periodKey")
        logger.debug(s"[ViewReturn] Submission details - chargeRef: ${submission.chargeReference}, submissionId: ${submission.submissionId}, submittedAt: ${submission.submittedAt}")

        val returnData = ReturnsData.fromSubmission(submission)
        logger.debug(s"[ViewReturn] Transformed submission to display format for vpdId: $vpdReference, periodKey: $periodKey")
        logger.debug(s"[ViewReturn] Response: ${Json.toJson(returnData)}")

        Future.successful(Ok(Json.toJson(returnData)))

      case None =>
        logger.info(s"[ViewReturn] No return submission found for vpdId=$vpdReference, periodKey=$periodKey - generating submissions")
        // Generate all 33 return submissions for this VPD ID
        val submissions = ReturnSubmissionData.generate33ReturnSubmissions(vpdReference)

        // Save all submissions
        Future.sequence(submissions.map(returnSubmissionRepository.set)).flatMap { _ =>
          // Try to retrieve the requested period again
          returnSubmissionRepository.get(vpdReference, periodKey).map {
            case Some(submission) =>
              logger.info(s"[ViewReturn] Generated and found return submission for vpdId=$vpdReference, periodKey=$periodKey")
              Ok(Json.toJson(ReturnsData.fromSubmission(submission)))
            case None =>
              logger.info(s"[ViewReturn] Period $periodKey not in fulfilled obligations for vpdId=$vpdReference - returning generated data")
              Ok(Json.toJson(ReturnsData(vpdReference, periodKey, submissionId = "submissionId", chargeReference = chargeReference)))
          }
        }
    }
  }
}
