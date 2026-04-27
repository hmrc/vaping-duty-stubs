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
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.vapingdutystubs.config.Constants.Headers.*
import uk.gov.hmrc.vapingdutystubs.models.ErrorData
import uk.gov.hmrc.vapingdutystubs.models.contactPreference.*
import uk.gov.hmrc.vapingdutystubs.models.returns.{ReturnCreateResponse, ReturnSubmittedResponse}
import uk.gov.hmrc.vapingdutystubs.repositories.SubscriptionSummaryRepository
import uk.gov.hmrc.vapingdutystubs.utils.LogHeadersHelper.logHeaders

import java.time.{Clock, Instant, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmitReturnController @Inject()(
                                        cc: ControllerComponents,
                                      )(using ExecutionContext) extends BackendController(cc)
  with Logging {

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

  def submitReturn(): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      logHeaders(request, "submitReturn", submitReturnHeaders)

      logger.info(s"Return submission received with json: ${request.body}")

      val correlationId = request.headers
        .get(correlationIdHeader)
        .getOrElse(
          throw new IllegalArgumentException("Expected correlation ID header")
        )

      Future.successful(
        Created(Json.toJson(ReturnCreateResponse(
          ReturnSubmittedResponse(
            processingDate = Instant.now(),
            vpdReferenceNumber = "vpdReferenceNumber",
            submissionID = Option("submissionID"),
            chargeReference = Option("chargeReference"),
            amount = BigDecimal(0),
            paymentDueDate = Option(LocalDate.now())
          )
        )))
      )
  }
}
