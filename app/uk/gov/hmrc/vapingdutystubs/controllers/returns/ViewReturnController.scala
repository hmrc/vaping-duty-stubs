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
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.vapingdutystubs.data.returns.{ReturnsData, ReturnSubmissionData}
import uk.gov.hmrc.vapingdutystubs.repositories.ReturnSubmissionRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ViewReturnController @Inject()(
  cc: ControllerComponents,
  returnSubmissionRepository: ReturnSubmissionRepository
)(using ExecutionContext) extends BackendController(cc)
  with Logging {

  def viewReturn(vpdReference: String, periodKey: String): Action[AnyContent] = Action.async {
    implicit request =>
      logger.info(s"[ViewReturn] Received request to view return for vpdId: $vpdReference, periodKey: $periodKey")
      logger.debug(s"[ViewReturn] Querying repository for vpdId: $vpdReference, periodKey: $periodKey")

      returnSubmissionRepository.get(vpdReference, periodKey).map {
      returnSubmissionRepository.get(vpdReference, periodKey).flatMap {
        case Some(submission) =>
          logger.info(s"[ViewReturn] Found return submission for vpdId: $vpdReference, periodKey: $periodKey")
          logger.debug(s"[ViewReturn] Submission details - chargeRef: ${submission.chargeReference}, submissionId: ${submission.submissionId}, submittedAt: ${submission.submittedAt}")

          val returnData = ReturnsData.fromSubmission(submission)
          logger.debug(s"[ViewReturn] Transformed submission to display format for vpdId: $vpdReference, periodKey: $periodKey")
          logger.debug(s"[ViewReturn] Response: ${Json.toJson(returnData)}")

          Ok(Json.toJson(returnData))

        case None =>
          logger.warn(s"[ViewReturn] No return submission found in repository for vpdId: $vpdReference, periodKey: $periodKey")
          logger.info(s"[ViewReturn] Returning generated/stub data for vpdId: $vpdReference, periodKey: $periodKey")

          val generatedData = ReturnsData(vpdReference, periodKey, submissionId = "submissionId")
          logger.debug(s"[ViewReturn] Generated data response: ${Json.toJson(generatedData)}")

          Ok(Json.toJson(generatedData))
          logger.info(s"Found return submission for vpdId=$vpdReference, periodKey=$periodKey")
          Future.successful(Ok(Json.toJson(ReturnsData.fromSubmission(submission))))
        case None =>
          logger.info(s"No return submission found for vpdId=$vpdReference, periodKey=$periodKey - generating submissions")
          // Generate all 33 return submissions for this VPD ID
          val submissions = ReturnSubmissionData.generate33ReturnSubmissions(vpdReference)

          // Save all submissions
          Future.sequence(submissions.map(returnSubmissionRepository.set)).flatMap { _ =>
            // Try to retrieve the requested period again
            returnSubmissionRepository.get(vpdReference, periodKey).map {
              case Some(submission) =>
                logger.info(s"Generated and found return submission for vpdId=$vpdReference, periodKey=$periodKey")
                Ok(Json.toJson(ReturnsData.fromSubmission(submission)))
              case None =>
                logger.info(s"Period $periodKey not in fulfilled obligations for vpdId=$vpdReference - returning generated data")
                Ok(Json.toJson(ReturnsData(vpdReference, periodKey, submissionId = "submissionId")))
            }
          }
      }
  }
}
