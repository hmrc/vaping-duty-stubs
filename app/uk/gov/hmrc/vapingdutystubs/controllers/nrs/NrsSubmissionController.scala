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

package uk.gov.hmrc.vapingdutystubs.controllers.nrs

import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.vapingdutystubs.models.nrs.{NrsApiErrorMessage, NrsSubmissionRequest, NrsSubmissionResponse}
import uk.gov.hmrc.vapingdutystubs.utils.RandomUUIDGenerator

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NrsSubmissionController @Inject()(
  cc: ControllerComponents,
  uuidGenerator: RandomUUIDGenerator
)(using ExecutionContext) extends BackendController(cc) with Logging {

  private val API_KEY_HEADER = "X-API-KEY"
  private val CORRELATION_ID_HEADER = "X-Correlation-Id"
  private val MISSING_API_KEY_MESSAGE = "Missing X-API-KEY header"
  private val MISSING_CORRELATION_ID_MESSAGE = "Missing X-Correlation-Id header"
  private val INVALID_REQUEST_BODY_MESSAGE = "Invalid request body"

  def submit(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    // Validate X-API-KEY header
    request.headers.get(API_KEY_HEADER) match {
      case None =>
        logger.warn(s"[NRS] Submission rejected - $MISSING_API_KEY_MESSAGE")
        Future.successful(Unauthorized(Json.toJson(
          NrsApiErrorMessage(List(MISSING_API_KEY_MESSAGE))
        )))
      case Some(_) =>
        // Validate X-Correlation-Id header
        request.headers.get(CORRELATION_ID_HEADER) match {
          case None =>
            logger.warn(s"[NRS] Submission rejected - $MISSING_CORRELATION_ID_MESSAGE")
            Future.successful(Unauthorized(Json.toJson(
              NrsApiErrorMessage(List(MISSING_CORRELATION_ID_MESSAGE))
            )))
          case Some(correlationId) =>
            // Parse and validate request body
            request.body.validate[NrsSubmissionRequest].fold(
              errors => {
                logger.warn(s"[NRS] Submission rejected - $INVALID_REQUEST_BODY_MESSAGE: $errors")
                Future.successful(BadRequest(Json.toJson(
                  NrsApiErrorMessage(List(INVALID_REQUEST_BODY_MESSAGE))
                )))
              },
              nrsRequest => {
                // Extract VPD ID for logging
                val vpdId = nrsRequest.metadata.searchKeys.getOrElse("approvalId", "unknown")

                // Log request details
                logger.info(
                  s"[NRS] Submission received - " +
                  s"CorrelationId: $correlationId, " +
                  s"VPD: $vpdId, " +
                  s"NotableEvent: ${nrsRequest.metadata.notableEvent}, " +
                  s"BusinessId: ${nrsRequest.metadata.businessId}"
                )
                logger.debug(s"[NRS] Full request body: ${Json.toJson(nrsRequest)}")

                // Generate submission ID and return 202 Accepted
                val submissionId = uuidGenerator.uuid
                logger.info(s"[NRS] Submission accepted - SubmissionId: $submissionId, CorrelationId: $correlationId")

                Future.successful(Accepted(Json.toJson(
                  NrsSubmissionResponse(nrSubmissionId = submissionId)
                )))
              }
            )
        }
    }
  }
}
