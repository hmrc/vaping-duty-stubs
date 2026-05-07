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
import play.api.http.HeaderNames
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.vapingdutystubs.config.Constants.Headers.*
import uk.gov.hmrc.vapingdutystubs.models.returns.submit.{ReturnCreateResponse, ReturnSubmittedResponse}
import uk.gov.hmrc.vapingdutystubs.repositories.ReturnSubmissionRepository
import uk.gov.hmrc.vapingdutystubs.utils.LogHeadersHelper.logHeaders

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.vapingdutystubs.data.returns.ReturnsData

class ViewReturnController @Inject()(
  cc: ControllerComponents,
  returnSubmissionRepository: ReturnSubmissionRepository
)(using ExecutionContext) extends BackendController(cc)
  with Logging {

  def viewReturn(vpdReference: String, periodKey: String): Action[AnyContent] = Action.async {
    implicit request =>
      returnSubmissionRepository.get(vpdReference, periodKey).map {
        case Some(submission) =>
          logger.info(s"Found return submission for vpdId=$vpdReference, periodKey=$periodKey")
          Ok(Json.toJson(ReturnsData.fromSubmission(submission)))
        case None =>
          logger.info(s"No return submission found for vpdId=$vpdReference, periodKey=$periodKey - returning generated data")
          Ok(Json.toJson(ReturnsData(
            vpdReference,
            periodKey,
            submissionId = "submissionId"
          )))
      }
  }
}
