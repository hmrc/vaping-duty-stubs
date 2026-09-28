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

package uk.gov.hmrc.vapingdutystubs.controllers.returns

import play.api.Logging
import play.api.http.HeaderNames
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.vapingdutystubs.config.Constants.Headers.*
import uk.gov.hmrc.vapingdutystubs.models.{DownstreamError, DownstreamErrorDetails, EtmpDownstreamError, EtmpDownstreamErrorDetails}
import uk.gov.hmrc.vapingdutystubs.models.returns.submit.{ReturnCreateRequest, ReturnCreateResponse, ReturnSubmittedResponse}
import uk.gov.hmrc.vapingdutystubs.services.returns.ReturnSubmissionService
import uk.gov.hmrc.vapingdutystubs.utils.LogHeadersHelper.logHeaders

import java.time.{Clock, Instant, ZoneId}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmitReturnController @Inject()(
                                        cc: ControllerComponents,
                                        returnSubmissionService: ReturnSubmissionService,
                                        clock: Clock
                                      )(using ExecutionContext) extends BackendController(cc) with Logging {

  private val submitReturnHeaders = Set(
    HeaderNames.AUTHORIZATION,
    xMessageType,
    xRegimeType,
    correlationIdHeader,
    xOriginatingSystemHeader,
    xReceiptDateHeader,
    xTransmittingSystemHeader,
    xZVPD
  )

  private val LOG_ID = "ABCDEF1234567890ABCDEF1234567890"

  /**
   * Checks if the VPD ID's last digit triggers a test error response.
   * Returns Some(Result) if an error should be returned, None for normal flow.
   * VPD ID format: (GB|XI)WK[7 digits]WK, so we check the 7th digit (3rd from end)
   */
  private def checkForTestErrorResponse(vpdId: String): Option[Result] = {
    // Extract the last digit before "WK" suffix (3rd character from the end)
    val lastDigit = if (vpdId.length >= 3) vpdId.charAt(vpdId.length - 3).toString else ""
    val now = Instant.now(clock)

    lastDigit match {
      case "1" =>
        logger.info(s"[SubmitReturn] Returning 400 Bad Request for test vpdId: $vpdId")
        Some(BadRequest(Json.toJson(DownstreamError(
          DownstreamErrorDetails("400", "Invalid request payload. Missing required field 'periodKey'.", LOG_ID)
        ))))
      case "2" =>
        logger.info(s"[SubmitReturn] Returning 403 Forbidden for test vpdId: $vpdId")
        Some(Forbidden(Json.toJson(DownstreamError(
          DownstreamErrorDetails("403", "Forbidden", LOG_ID)
        ))))
      case "4" =>
        logger.info(s"[SubmitReturn] Returning 409 Conflict for test vpdId: $vpdId")
        Some(Conflict(Json.toJson(EtmpDownstreamError(
          EtmpDownstreamErrorDetails("004", "Duplicate submission", now.toString)
        ))))
      case "5" =>
        logger.info(s"[SubmitReturn] Returning 422 Unprocessable Entity for test vpdId: $vpdId")
        Some(UnprocessableEntity(Json.toJson(EtmpDownstreamError(
          EtmpDownstreamErrorDetails("001", "Regime missing or invalid", now.toString)
        ))))
      case "8" =>
        logger.info(s"[SubmitReturn] Returning 500 Internal Server Error for test vpdId: $vpdId")
        Some(InternalServerError(Json.toJson(DownstreamError(
          DownstreamErrorDetails("500", "SAP PI system is currently unavailable. Please try again later.", LOG_ID)
        ))))
      case _ =>
        None
    }
  }

  def submitReturn(): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      logHeaders(request, "submitReturn", submitReturnHeaders)

      logger.info(s"[SubmitReturn] Received return submission request")
      logger.debug(s"[SubmitReturn] Request body: ${request.body}")

      val vpdId = request.headers
        .get(xZVPD)
        .getOrElse(
          throw new IllegalArgumentException("Expected x-zvpd header")
        )

      logger.info(s"[SubmitReturn] Processing submission for vpdId: $vpdId")

      // Check for test error responses based on last digit of VPD ID
      checkForTestErrorResponse(vpdId) match {
        case Some(errorResult) =>
          Future.successful(errorResult)
        case None =>
          processReturnSubmission(vpdId, request.body)
      }
  }

  private def processReturnSubmission(vpdId: String, body: JsValue): Future[Result] = {
    body.validate[ReturnCreateRequest].fold(
      errors => {
        logger.error(s"[SubmitReturn] JSON validation failed for vpdId: $vpdId, errors: $errors")
        Future.successful(BadRequest(Json.obj("error" -> "Invalid request body")))
      },
      returnRequest => {
        logger.info(s"[SubmitReturn] JSON parsed successfully for vpdId: $vpdId, periodKey: ${returnRequest.periodKey}")

        returnSubmissionService.processSubmission(vpdId, returnRequest).map {
          case Left(validationError) =>
            logger.warn(s"[SubmitReturn] Business validation failed for vpdId: $vpdId, periodKey: ${returnRequest.periodKey}, error: $validationError")
            BadRequest(Json.obj("error" -> validationError))

          case Right(submission) =>
            val paymentDueDate = submission.submittedAt.atZone(ZoneId.systemDefault()).toLocalDate.plusMonths(1)

            val response = ReturnCreateResponse(
              ReturnSubmittedResponse(
                processingDate = submission.submittedAt,
                vpdReferenceNumber = vpdId,
                submissionID = Some(submission.submissionId),
                chargeReference = submission.chargeReference,
                amount = returnRequest.totalDutyDue.totalDue,
                paymentDueDate = Some(paymentDueDate),
                declaration = returnRequest.declaration
              )
            )

            logger.info(s"[SubmitReturn] Successfully completed submission for vpdId: $vpdId, periodKey: ${returnRequest.periodKey}, submissionId: ${submission.submissionId}")
            logger.debug(s"[SubmitReturn] Response: ${Json.toJson(response)}")

            Created(Json.toJson(response))
        }
      }
    )
  }
}
