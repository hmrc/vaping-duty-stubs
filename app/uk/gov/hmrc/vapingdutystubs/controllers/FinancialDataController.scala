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
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.vapingdutystubs.data.financialdata.FinancialDataStubData
import uk.gov.hmrc.vapingdutystubs.models.{DownstreamErrorsDetails, UnprocessableEntityError}
import uk.gov.hmrc.vapingdutystubs.models.financialdata.{FinancialDataBody, FinancialDataQueryRequest, FinancialDataResponse, FinancialDataState, FinancialDataSuccessBody}
import uk.gov.hmrc.vapingdutystubs.repositories.FinancialDataRepository

import java.time.{Clock, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class FinancialDataController @Inject()(
  cc: ControllerComponents,
  financialDataRepository: FinancialDataRepository,
  clock: Clock
)(using ExecutionContext) extends BackendController(cc) with Logging {

  private val noDataIdentifiedCode = "018"

  def query(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[FinancialDataQueryRequest] match {
      case JsError(errors) =>
        logger.warn(s"Invalid financial data query request: $errors")
        Future.successful(BadRequest(Json.obj("error" -> "Invalid request body")))

      case JsSuccess(queryRequest, _) =>
        val vpdId = queryRequest.taxpayerInformation.idNumber

        financialDataRepository.get(vpdId).flatMap {
          case Some(state) if state.noDataIdentified =>
            logger.info(s"Returning 422/018 No Data Identified for vpdId=$vpdId")
            Future.successful(UnprocessableEntity(Json.toJson(UnprocessableEntityError(
              errors = DownstreamErrorsDetails(processingDate = Instant.now(clock), code = noDataIdentifiedCode, text = "No Data Identified")
            ))))

          case Some(state) =>
            logger.info(s"Returning ${state.documentDetails.size} document(s) for vpdId=$vpdId")
            Future.successful(Created(Json.toJson(successResponse(state))))

          case None =>
            logger.info(s"No financial data scenario set for vpdId=$vpdId - generating default data")
            val sampleState = FinancialDataStubData.defaultData(vpdId)
            financialDataRepository.set(sampleState).map { _ =>
              Created(Json.toJson(successResponse(sampleState)))
            }
        }
    }
  }

  private def successResponse(state: FinancialDataState): FinancialDataResponse =
    FinancialDataResponse(
      success = FinancialDataSuccessBody(
        processingDate = Instant.now(clock),
        financialData = FinancialDataBody(documentDetails = state.documentDetails)
      )
    )
}
