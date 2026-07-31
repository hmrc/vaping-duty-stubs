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

package uk.gov.hmrc.vapingdutystubs.controllers.testonly

import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.vapingdutystubs.data.financialdata.FinancialDataStubData
import uk.gov.hmrc.vapingdutystubs.models.financialdata.FinancialDataState
import uk.gov.hmrc.vapingdutystubs.repositories.FinancialDataRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton() class TestFinancialDataController @Inject()(
  cc: ControllerComponents,
  financialDataRepository: FinancialDataRepository
)(using ExecutionContext) extends BackendController(cc) with Logging {

  private val SCENARIO_OUTSTANDING_ONLY = "outstanding-only"
  private val SCENARIO_WITH_UNALLOCATED = "with-unallocated"
  private val SCENARIO_CLEARED_ONLY = "cleared-only"
  private val SCENARIO_MIXED = "mixed"
  private val SCENARIO_NONE = "none"
  private val SCENARIO_SINGLE_OUTSTANDING = "single-outstanding"
  private val SCENARIO_OVERDUE_BALANCE = "overdue-balance"
  private val SCENARIO_CREDIT_BALANCE = "credit-balance"
  private val SCENARIO_NOTHING_OWED = "nothing-owed"

  private val validScenarios =
    Set(
      SCENARIO_OUTSTANDING_ONLY,
      SCENARIO_WITH_UNALLOCATED,
      SCENARIO_CLEARED_ONLY,
      SCENARIO_MIXED,
      SCENARIO_NONE,
      SCENARIO_SINGLE_OUTSTANDING,
      SCENARIO_OVERDUE_BALANCE,
      SCENARIO_CREDIT_BALANCE,
      SCENARIO_NOTHING_OWED
    )

  def setScenario(vpdId: String, scenarioName: String): Action[AnyContent] = Action.async { implicit request =>
    if (!validScenarios.contains(scenarioName)) {
      logger.warn(s"Invalid scenario name: $scenarioName")
      Future.successful(BadRequest(Json.obj(
        "error" -> "Invalid scenario",
        "message" -> s"Scenario must be one of: ${validScenarios.mkString(", ")}",
        "provided" -> scenarioName)))
    } else {
      val state = scenarioName match {
        case SCENARIO_OUTSTANDING_ONLY => FinancialDataStubData.outstandingOnly(vpdId)
        case SCENARIO_WITH_UNALLOCATED => FinancialDataStubData.withUnallocated(vpdId)
        case SCENARIO_CLEARED_ONLY => FinancialDataStubData.clearedOnly(vpdId)
        case SCENARIO_MIXED => FinancialDataStubData.mixed(vpdId)
        case SCENARIO_NONE => FinancialDataStubData.noData(vpdId)
        case SCENARIO_SINGLE_OUTSTANDING => FinancialDataStubData.singleOutstanding(vpdId)
        case SCENARIO_OVERDUE_BALANCE => FinancialDataStubData.overdueBalance(vpdId)
        case SCENARIO_CREDIT_BALANCE => FinancialDataStubData.creditBalance(vpdId)
        case SCENARIO_NOTHING_OWED => FinancialDataStubData.nothingOwed(vpdId)
      }

      financialDataRepository.set(state).map { _ =>
        logger.info(s"Set scenario '$scenarioName' for vpdId=$vpdId with ${state.documentDetails.size} document(s)")
        Ok(Json.obj(
          "message" -> s"Successfully set scenario '$scenarioName' for VPD ID $vpdId",
          "vpdId" -> vpdId,
          "scenario" -> scenarioName,
          "documentCount" -> state.documentDetails.size,
          "noDataIdentified" -> state.noDataIdentified
        ))
      }
    }
  }

  def setCustom(vpdId: String): Action[AnyContent] = Action.async { implicit request =>
    request.body.asJson match {
      case Some(json) => json.validate[FinancialDataState] match {
        case JsSuccess(state, _) => if (state.vpdId != vpdId) {
          Future.successful(BadRequest(Json.obj("error" -> "VPD ID mismatch", "message" -> s"VPD ID in URL ($vpdId) does not match VPD ID in body (${state.vpdId})")))
        } else {
          financialDataRepository.set(state).map { _ =>
            logger.info(s"Set custom financial data for vpdId=$vpdId with ${state.documentDetails.size} document(s)")
            Ok(Json.obj("message" -> s"Successfully set custom financial data for VPD ID $vpdId", "vpdId" -> vpdId, "documentCount" -> state.documentDetails.size))
          }
        }
        case JsError(errors) => logger.warn(s"Invalid JSON for custom financial data: $errors")
          Future.successful(BadRequest(Json.obj("error" -> "Invalid JSON", "message" -> "Could not parse financial data state from request body", "details" -> JsError.toJson(errors))))
      }
      case None => Future.successful(BadRequest(Json.obj("error" -> "Missing JSON body", "message" -> "Request body must contain valid FinancialDataState JSON")))
    }
  }

  def clearForVpdId(vpdId: String): Action[AnyContent] = Action.async { implicit request =>
    financialDataRepository.get(vpdId).flatMap {
      case Some(_) =>
        financialDataRepository.set(FinancialDataState(vpdId = vpdId, noDataIdentified = false, documentDetails = Seq.empty, lastUpdated = java.time.Instant.now())).map { _ =>
          logger.info(s"Cleared financial data for vpdId=$vpdId")
          Ok(Json.obj("message" -> s"Successfully cleared financial data for VPD ID $vpdId", "vpdId" -> vpdId))
        }
      case None =>
        logger.info(s"No financial data found for vpdId=$vpdId")
        Future.successful(NotFound(Json.obj("message" -> s"No financial data found for VPD ID $vpdId", "vpdId" -> vpdId)))
    }
  }

  def clearAll(): Action[AnyContent] = Action.async { implicit request =>
    financialDataRepository.clear.map { _ =>
      logger.info("Cleared all financial data")
      Ok(Json.obj("message" -> "Successfully cleared all financial data"))
    }
  }
}
