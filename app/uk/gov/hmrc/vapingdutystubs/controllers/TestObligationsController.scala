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

package uk.gov.hmrc.vapingdutystubs.controllers

import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.vapingdutystubs.data.obligations.ObligationsData
import uk.gov.hmrc.vapingdutystubs.models.obligations.ObligationState
import uk.gov.hmrc.vapingdutystubs.repositories.ObligationsRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton() class TestObligationsController @Inject()(cc: ControllerComponents, obligationsRepository: ObligationsRepository)(using ExecutionContext) extends BackendController(cc) with Logging {

  private val SCENARIO_ONLY_OPEN = "only-open"
  private val SCENARIO_ONLY_COMPLETED = "only-completed"
  private val SCENARIO_MIXED = "mixed"
  private val SCENARIO_NONE = "none"

  private val validScenarios = Set(SCENARIO_ONLY_OPEN, SCENARIO_ONLY_COMPLETED, SCENARIO_MIXED, SCENARIO_NONE)

  def setScenario(vpdId: String, scenarioName: String): Action[AnyContent] = Action.async { implicit request =>
    if (!validScenarios.contains(scenarioName)) {
      logger.warn(s"Invalid scenario name: $scenarioName")
      Future.successful(BadRequest(Json.obj(
        "error" -> "Invalid scenario",
        "message" -> s"Scenario must be one of: ${validScenarios.mkString(", ")}",
        "provided" -> scenarioName)))
    } else {
      val obligationState = scenarioName match {
        case SCENARIO_ONLY_OPEN => ObligationsData.onlyOpenReturns(vpdId)
        case SCENARIO_ONLY_COMPLETED => ObligationsData.onlyCompletedReturns(vpdId)
        case SCENARIO_MIXED => ObligationsData.sampleObligations(vpdId)
        case SCENARIO_NONE => ObligationsData.noObligations(vpdId)
      }

      obligationsRepository.set(obligationState).map { _ =>
        logger.info(s"Set scenario '$scenarioName' for vpdId=$vpdId")
        Ok(Json.obj(
          "message" -> s"Successfully set scenario '$scenarioName' for VPD ID $vpdId",
          "vpdId" -> vpdId,
          "scenario" -> scenarioName,
          "obligationCount" -> obligationState.obligations.size
        ))
      }
    }
  }

  def clearForVpdId(vpdId: String): Action[AnyContent] = Action.async { implicit request =>
    obligationsRepository.get(vpdId).flatMap {
      case Some(_) => obligationsRepository.set(ObligationState(vpdId = vpdId, obligations = Seq.empty)).map { _ =>
        logger.info(s"Cleared obligations for vpdId=$vpdId")
        Ok(Json.obj("message" -> s"Successfully cleared obligations for VPD ID $vpdId", "vpdId" -> vpdId))
      }
      case None => logger.info(s"No obligations found for vpdId=$vpdId")
        Future.successful(NotFound(Json.obj("message" -> s"No obligations found for VPD ID $vpdId", "vpdId" -> vpdId)))
    }
  }

  def clearAll(): Action[AnyContent] = Action.async { implicit request =>
    obligationsRepository.clear.map { _ =>
      logger.info("Cleared all obligations data")
      Ok(Json.obj("message" -> "Successfully cleared all obligations data"))
    }
  }

  def setCustom(vpdId: String): Action[AnyContent] = Action.async { implicit request =>
    request.body.asJson match {
      case Some(json) => json.validate[ObligationState] match {
        case JsSuccess(obligationState, _) => if (obligationState.vpdId != vpdId) {
          Future.successful(BadRequest(Json.obj("error" -> "VPD ID mismatch", "message" -> s"VPD ID in URL ($vpdId) does not match VPD ID in body (${obligationState.vpdId})")))
        } else {
          obligationsRepository.set(obligationState).map { _ =>
            logger.info(s"Set custom obligations for vpdId=$vpdId with ${obligationState.obligations.size} obligations")
            Ok(Json.obj("message" -> s"Successfully set custom obligations for VPD ID $vpdId", "vpdId" -> vpdId, "obligationCount" -> obligationState.obligations.size))
          }
        }
        case JsError(errors) => logger.warn(s"Invalid JSON for custom obligations: $errors")
          Future.successful(BadRequest(Json.obj("error" -> "Invalid JSON", "message" -> "Could not parse obligation state from request body", "details" -> JsError.toJson(errors))))
      }
      case None => Future.successful(BadRequest(Json.obj("error" -> "Missing JSON body", "message" -> "Request body must contain valid ObligationState JSON")))
    }
  }
}
