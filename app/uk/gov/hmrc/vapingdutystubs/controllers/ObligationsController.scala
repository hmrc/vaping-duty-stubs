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

package uk.gov.hmrc.vapingdutystubs.controllers

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.vapingdutystubs.data.obligations.ObligationsData
import uk.gov.hmrc.vapingdutystubs.models.obligations.{ObligationState, ObligationStatus, ObligationsResponse}
import uk.gov.hmrc.vapingdutystubs.repositories.ObligationsRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class ObligationsController @Inject()(
  cc: ControllerComponents,
  obligationsRepository: ObligationsRepository
)(using ExecutionContext) extends BackendController(cc) with Logging {

  def get(): Action[AnyContent] = Action.async { request =>

    val params = extractParameters(request)
    
    obligationsRepository.get(params._2).map {
      case Some(obligationState) =>
        logger.info(s"Found obligations for vpdId=${params._2}")
        Ok(Json.toJson(ObligationsResponse(obligation = obligationState.obligations)))
      case None =>
        logger.info(s"No obligations found for vpdId=${params._2} - returning generated data")
        Ok(Json.toJson(ObligationsData.createMockObligationsResponse()))
    }
  }

  private def extractParameters(request: Request[_]) = {
    val params = request.queryString.view.mapValues(_.mkString(","))

    logger.info(s"Received the following parameters: ${params.mkString(",")}")

    val status = params.getOrElse("displayRequest", throw new IllegalArgumentException("Expected displayRequest in parameters")) match {
      case "O" => ObligationStatus.O
      case "F" => ObligationStatus.F
      case "A" => ObligationStatus.A
      case _ => throw new IllegalArgumentException("Invalid status parameter")
    }

    val vpdId = params.getOrElse("referenceNumber", throw new IllegalArgumentException("Expected referenceNumber in parameters"))

    (status, vpdId)
  }
}
