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
import uk.gov.hmrc.vapingdutystubs.models.returns.ReturnSubmission
import uk.gov.hmrc.vapingdutystubs.models.returns.submit.{ReturnCreateRequest, ReturnCreateResponse, ReturnSubmittedResponse}
import uk.gov.hmrc.vapingdutystubs.repositories.{ObligationsRepository, ReturnSubmissionRepository}
import uk.gov.hmrc.vapingdutystubs.utils.LogHeadersHelper.logHeaders
import uk.gov.hmrc.vapingdutystubs.utils.{LogHeadersHelper, RandomUUIDGenerator}

import java.time.{Clock, Instant, ZoneId}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmitReturnController @Inject()(
  cc: ControllerComponents,
  returnSubmissionRepository: ReturnSubmissionRepository,
  obligationsRepository: ObligationsRepository,
  uuidGenerator: RandomUUIDGenerator,
  clock: Clock
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

      val vpdId = request.headers
        .get(xZVPD)
        .getOrElse(
          throw new IllegalArgumentException("Expected x-zvpd header")
        )

      request.body.validate[ReturnCreateRequest].fold(
        errors => {
          logger.error(s"Invalid return submission request: $errors")
          Future.successful(BadRequest(Json.obj("error" -> "Invalid request body")))
        },
        returnRequest => {
          val now = Instant.now(clock)
          val submissionId = uuidGenerator.uuid
          val chargeReference = s"XMVPD${uuidGenerator.uuidHyphenTrimmed.take(12)}".toUpperCase

          val submission = ReturnSubmission(
            vpdId = vpdId,
            periodKey = returnRequest.periodKey,
            chargeReference = chargeReference,
            submittedReturn = returnRequest,
            submittedAt = now,
            submissionId = submissionId
          )

          for {
            _ <- returnSubmissionRepository.set(submission)
            _ <- obligationsRepository.markAsFulfilled(vpdId, returnRequest.periodKey, now)
          } yield {
            val paymentDueDate = now.atZone(ZoneId.systemDefault()).toLocalDate.plusMonths(1)

            Created(Json.toJson(ReturnCreateResponse(
              ReturnSubmittedResponse(
                processingDate = now,
                vpdReferenceNumber = vpdId,
                submissionID = Some(submissionId),
                chargeReference = Some(chargeReference),
                amount = returnRequest.totalDutyDue.totalDutyDue,
                paymentDueDate = Some(paymentDueDate),
                declaration = returnRequest.declaration
              )
            )))
          }
        }
      )
  }
}
